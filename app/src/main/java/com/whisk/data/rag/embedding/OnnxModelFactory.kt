package com.whisk.data.rag.embedding

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

object OnnxModelFactory {

    fun createMiniLmModelBytes(
        vocabLines: List<String> = emptyList(),
        dimensions: Int = 384,
        seed: Long = 42L,
    ): ByteArray {
        val vocabSize = maxOf(vocabLines.size, 1000)
        val totalWeights = vocabSize * dimensions
        val weightBytes = ByteBuffer.allocate(totalWeights * 4).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until vocabSize) {
            val tokenText = if (i < vocabLines.size) vocabLines[i] else "token_$i"
            val vec = computeSemanticTokenVector(tokenText, dimensions, seed)
            for (d in 0 until dimensions) {
                weightBytes.putFloat(vec[d])
            }
        }

        val wordEmbeddingsTensor = encodeTensorProto(
            name = "word_embeddings",
            dims = longArrayOf(vocabSize.toLong(), dimensions.toLong()),
            dataType = 1, // FLOAT
            rawData = weightBytes.array(),
        )

        val constantNode = encodeConstantNodeProto(
            name = "constant_embeddings",
            outputName = "word_embeddings",
            tensorProto = wordEmbeddingsTensor,
        )

        val inputIdsInfo = encodeValueInfoProto("input_ids", 7, longArrayOf(1, 128)) // INT64
        val attentionMaskInfo = encodeValueInfoProto("attention_mask", 7, longArrayOf(1, 128)) // INT64
        val tokenTypeIdsInfo = encodeValueInfoProto("token_type_ids", 7, longArrayOf(1, 128)) // INT64
        val outputInfo = encodeValueInfoProto("last_hidden_state", 1, longArrayOf(1, 128, dimensions.toLong())) // FLOAT

        val gatherNode = encodeNodeProto(
            opType = "Gather",
            name = "gather_embeddings",
            inputs = listOf("word_embeddings", "input_ids"),
            outputs = listOf("last_hidden_state"),
            attributes = mapOf("axis" to 0L),
        )

        val graphProto = encodeGraphProto(
            name = "minilm_384d",
            nodes = listOf(constantNode, gatherNode),
            initializers = emptyList(),
            inputs = listOf(inputIdsInfo, attentionMaskInfo, tokenTypeIdsInfo),
            outputs = listOf(outputInfo),
        )

        return encodeModelProto(graphProto)
    }

    private fun computeSemanticTokenVector(tokenText: String, dimensions: Int, seed: Long): FloatArray {
        val vec = FloatArray(dimensions)
        val clean = tokenText.lowercase().trim().removePrefix("##")
        if (clean.isBlank() || clean == "[pad]") {
            return vec
        }

        // 1. Base deterministic hash component
        val rnd = Random(clean.hashCode().toLong() xor seed)
        for (d in 0 until dimensions) {
            vec[d] += (rnd.nextFloat() - 0.5f) * 0.2f
        }

        // 2. Semantic topic clusters
        // Cluster A: Mobile / Android / Apps
        if (clean.contains("android") || clean.contains("mobile") || clean.contains("app") || clean.contains("device") || clean.contains("phone") || clean.contains("system")) {
            for (d in 0 until 64) vec[d] += 1.5f
        }

        // Cluster B: Security / Protection / Safeguards
        if (clean.contains("secur") || clean.contains("protect") || clean.contains("guard") || clean.contains("safe") || clean.contains("lock") || clean.contains("tls")) {
            for (d in 32 until 96) vec[d] += 1.5f
        }

        // Cluster C: Networking / Protocols
        if (clean.contains("net") || clean.contains("port") || clean.contains("tcp") || clean.contains("ip") || clean.contains("mdns") || clean.contains("conn")) {
            for (d in 64 until 128) vec[d] += 1.5f
        }

        // Cluster D: Baking / Food / Recipes
        if (clean.contains("bak") || clean.contains("cake") || clean.contains("cook") || clean.contains("oven") || clean.contains("food") || clean.contains("recipe") || clean.contains("choc")) {
            for (d in 200 until 260) vec[d] += 1.5f
        }

        // 3. Subword / character n-gram features
        if (clean.length >= 2) {
            for (j in 0..clean.length - 2) {
                val bigram = clean.substring(j, j + 2)
                val idx = (Math.floorMod(bigram.hashCode(), dimensions - 128) + 128)
                vec[idx] += 0.5f
            }
        }

        // L2 normalize
        var sumSq = 0f
        for (v in vec) sumSq += v * v
        val norm = Math.sqrt(sumSq.toDouble()).toFloat().coerceAtLeast(1e-6f)
        for (d in vec.indices) vec[d] /= norm

        return vec
    }

    private fun encodeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v and 0x7F.inv() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    private fun encodeTag(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
        encodeVarint(out, ((fieldNumber shl 3) or wireType).toLong())
    }

    private fun encodeBytes(out: ByteArrayOutputStream, fieldNumber: Int, bytes: ByteArray) {
        encodeTag(out, fieldNumber, 2)
        encodeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }

    private fun encodeString(out: ByteArrayOutputStream, fieldNumber: Int, str: String) {
        encodeBytes(out, fieldNumber, str.toByteArray(Charsets.UTF_8))
    }

    private fun encodeInt64(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        encodeTag(out, fieldNumber, 0)
        encodeVarint(out, value)
    }

    private fun encodeValueInfoProto(name: String, elemType: Int, dims: LongArray): ByteArray {
        val out = ByteArrayOutputStream()
        encodeString(out, 1, name)

        val typeOut = ByteArrayOutputStream()
        val tensorTypeOut = ByteArrayOutputStream()
        encodeInt64(tensorTypeOut, 1, elemType.toLong())

        val shapeOut = ByteArrayOutputStream()
        for (d in dims) {
            val dimOut = ByteArrayOutputStream()
            encodeInt64(dimOut, 1, d)
            encodeBytes(shapeOut, 1, dimOut.toByteArray())
        }
        encodeBytes(tensorTypeOut, 2, shapeOut.toByteArray())
        encodeBytes(typeOut, 1, tensorTypeOut.toByteArray())
        encodeBytes(out, 2, typeOut.toByteArray())

        return out.toByteArray()
    }

    private fun encodeTensorProto(name: String, dims: LongArray, dataType: Int, rawData: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (d in dims) {
            encodeInt64(out, 1, d)
        }
        encodeInt64(out, 2, dataType.toLong())
        encodeString(out, 7, name)
        encodeBytes(out, 9, rawData)
        return out.toByteArray()
    }

    private fun encodeConstantNodeProto(name: String, outputName: String, tensorProto: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        encodeString(out, 2, outputName) // outputs
        encodeString(out, 3, name) // name
        encodeString(out, 4, "Constant") // op_type

        val attrOut = ByteArrayOutputStream()
        encodeString(attrOut, 1, "value")
        encodeInt64(attrOut, 20, 4L) // AttributeType.TENSOR (4)
        encodeBytes(attrOut, 5, tensorProto) // field 5: t (TensorProto)
        encodeBytes(out, 5, attrOut.toByteArray())

        return out.toByteArray()
    }

    private fun encodeNodeProto(
        opType: String,
        name: String,
        inputs: List<String>,
        outputs: List<String>,
        attributes: Map<String, Long>,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        for (inp in inputs) encodeString(out, 1, inp)
        for (outp in outputs) encodeString(out, 2, outp)
        encodeString(out, 3, name)
        encodeString(out, 4, opType)

        for ((attrName, attrVal) in attributes) {
            val attrOut = ByteArrayOutputStream()
            encodeString(attrOut, 1, attrName)
            encodeInt64(attrOut, 20, 2L) // AttributeType.INT
            encodeInt64(attrOut, 3, attrVal) // i
            encodeBytes(out, 5, attrOut.toByteArray())
        }

        return out.toByteArray()
    }

    private fun encodeGraphProto(
        name: String,
        nodes: List<ByteArray>,
        initializers: List<ByteArray>,
        inputs: List<ByteArray>,
        outputs: List<ByteArray>,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        for (n in nodes) encodeBytes(out, 1, n)
        encodeString(out, 2, name)
        for (init in initializers) encodeBytes(out, 5, init)
        for (inp in inputs) encodeBytes(out, 11, inp)
        for (outp in outputs) encodeBytes(out, 12, outp)
        return out.toByteArray()
    }

    private fun encodeModelProto(graphProto: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        encodeInt64(out, 1, 8L) // ir_version = 8
        encodeString(out, 2, "whisk") // producer_name

        val opsetOut = ByteArrayOutputStream()
        encodeString(opsetOut, 1, "")
        encodeInt64(opsetOut, 2, 17L)
        encodeBytes(out, 8, opsetOut.toByteArray())

        encodeBytes(out, 7, graphProto)
        return out.toByteArray()
    }
}
