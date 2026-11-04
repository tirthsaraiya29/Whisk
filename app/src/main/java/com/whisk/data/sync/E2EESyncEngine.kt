package com.whisk.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class E2EESyncEngine {

    companion object {
        private const val ITERATIONS = 65536
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
    }

    suspend fun encryptPayload(plainText: String, passphrase: CharArray): String = withContext(Dispatchers.Default) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Format: [16 bytes salt][12 bytes iv][cipherText]
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        Base64.getEncoder().encodeToString(combined)
    }

    suspend fun decryptPayload(encryptedBase64: String, passphrase: CharArray): String = withContext(Dispatchers.Default) {
        val combined = Base64.getDecoder().decode(encryptedBase64)
        if (combined.size < SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("Corrupted encrypted payload: insufficient bytes")
        }

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val cipherTextLength = combined.size - SALT_LENGTH - IV_LENGTH
        val cipherText = ByteArray(cipherTextLength)

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherTextLength)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val decryptedBytes = cipher.doFinal(cipherText)
        String(decryptedBytes, Charsets.UTF_8)
    }
}
