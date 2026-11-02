package com.whisk.data.rag.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.whisk.data.rag.embedding.tokenizer.TokenizationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxEmbeddingInferenceEngine(
    private val ortSession: OrtSession,
    private val ortEnv: OrtEnvironment,
    val targetDimensions: Int = 384,
) : Closeable {

    suspend fun runInference(tokenResult: TokenizationResult): FloatArray = withContext(Dispatchers.Default) {
        val shape = longArrayOf(1, tokenResult.inputIds.size.toLong())

        var tensorInputIds: OnnxTensor? = null
        var tensorAttentionMask: OnnxTensor? = null
        var tensorTokenTypeIds: OnnxTensor? = null
        var result: OrtSession.Result? = null

        try {
            tensorInputIds = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenResult.inputIds), shape)
            tensorAttentionMask = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenResult.attentionMask), shape)
            tensorTokenTypeIds = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenResult.tokenTypeIds), shape)

            val inputs = mutableMapOf<String, OnnxTensor>()
            val inputNames = ortSession.inputNames
            if (inputNames.contains("input_ids")) inputs["input_ids"] = tensorInputIds
            if (inputNames.contains("attention_mask")) inputs["attention_mask"] = tensorAttentionMask
            if (inputNames.contains("token_type_ids")) inputs["token_type_ids"] = tensorTokenTypeIds

            result = ortSession.run(inputs)

            val outputTensor = result.get(0).value
            val floatVector = processOutputTensor(outputTensor, tokenResult.attentionMask, targetDimensions)

            normalizeVector(floatVector)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw e
        } finally {
            try { tensorInputIds?.close() } catch (_: Exception) {}
            try { tensorAttentionMask?.close() } catch (_: Exception) {}
            try { tensorTokenTypeIds?.close() } catch (_: Exception) {}
            try { result?.close() } catch (_: Exception) {}
        }
    }

    private fun processOutputTensor(
        outputValue: Any,
        attentionMask: LongArray,
        dimensions: Int,
    ): FloatArray {
        if (outputValue is Array<*> && outputValue.isArrayOf<Array<FloatArray>>()) {
            @Suppress("UNCHECKED_CAST")
            val batchSeqDim = outputValue as Array<Array<FloatArray>>
            val seqEmbeddings = batchSeqDim[0]
            val pooled = FloatArray(dimensions)
            var validTokensCount = 0.0f

            for (i in seqEmbeddings.indices) {
                if (i < attentionMask.size && attentionMask[i] > 0) {
                    validTokensCount += 1.0f
                    val tokenDim = seqEmbeddings[i]
                    for (d in 0 until minOf(dimensions, tokenDim.size)) {
                        pooled[d] += tokenDim[d]
                    }
                }
            }

            if (validTokensCount > 0f) {
                for (d in pooled.indices) {
                    pooled[d] /= validTokensCount
                }
            }
            return pooled
        } else if (outputValue is Array<*> && outputValue.isArrayOf<FloatArray>()) {
            @Suppress("UNCHECKED_CAST")
            val batchDim = outputValue as Array<FloatArray>
            val vector = batchDim[0]
            val result = FloatArray(dimensions)
            System.arraycopy(vector, 0, result, 0, minOf(dimensions, vector.size))
            return result
        } else {
            throw IllegalArgumentException("Unsupported ONNX output tensor shape or type: ${outputValue.javaClass.name}")
        }
    }

    private fun normalizeVector(vector: FloatArray): FloatArray {
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm > 1e-6f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    override fun close() {
        try {
            ortSession.close()
        } catch (_: Exception) {}
    }
}
