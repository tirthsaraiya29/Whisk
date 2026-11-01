package com.whisk.data.local.rag

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(
    tableName = "embedding_vectors",
    indices = [
        Index(value = ["sourceObjectId"]),
        Index(value = ["modelId"]),
    ],
)
data class EmbeddingVectorEntity(
    @PrimaryKey val chunkId: String,
    val sourceObjectId: String,
    val modelId: String,
    val dimensions: Int,
    val vectorBlob: ByteArray,
    val contentHash: String,
    val updatedAt: Long,
) {
    fun toFloatArray(): FloatArray {
        val floatCount = dimensions
        val floatArray = FloatArray(floatCount)
        val buffer = ByteBuffer.wrap(vectorBlob).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until floatCount) {
            if (buffer.remaining() >= 4) {
                floatArray[i] = buffer.float
            }
        }
        return floatArray
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EmbeddingVectorEntity
        if (chunkId != other.chunkId) return false
        if (sourceObjectId != other.sourceObjectId) return false
        if (modelId != other.modelId) return false
        if (dimensions != other.dimensions) return false
        if (!vectorBlob.contentEquals(other.vectorBlob)) return false
        if (contentHash != other.contentHash) return false
        if (updatedAt != other.updatedAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = chunkId.hashCode()
        result = 31 * result + sourceObjectId.hashCode()
        result = 31 * result + modelId.hashCode()
        result = 31 * result + dimensions
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + contentHash.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    companion object {
        fun fromFloatArray(
            chunkId: String,
            sourceObjectId: String,
            modelId: String,
            floatArray: FloatArray,
            contentHash: String,
            updatedAt: Long,
        ): EmbeddingVectorEntity {
            val buffer = ByteBuffer.allocate(floatArray.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (f in floatArray) {
                buffer.putFloat(f)
            }
            return EmbeddingVectorEntity(
                chunkId = chunkId,
                sourceObjectId = sourceObjectId,
                modelId = modelId,
                dimensions = floatArray.size,
                vectorBlob = buffer.array(),
                contentHash = contentHash,
                updatedAt = updatedAt,
            )
        }
    }
}
