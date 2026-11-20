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

/**
 * End-to-End Encryption Engine for Whisk notes, vaults, and cloud sync backups.
 *
 * Cryptographic Architecture & Nonce Invariants (NIST SP 800-38D):
 * 1. Cipher: AES-256-GCM authenticated encryption with 128-bit authentication tag.
 * 2. Key Derivation: PBKDF2WithHmacSHA256 with 65,536 iterations and a freshly generated
 *    16-byte cryptographically secure random salt for EVERY encryption invocation.
 * 3. IV/Nonce Uniqueness Guarantee: For each encryption operation, an ephemeral 12-byte (96-bit)
 *    random Initialization Vector is freshly sampled using [SecureRandom].
 *    Because both the derived key (via unique salt) and the 96-bit IV are uniquely sampled per operation,
 *    key/IV reuse is cryptographically impossible with collision probability <= 2^-96.
 */
class E2EESyncEngine {

    companion object {
        private const val CURRENT_ITERATIONS = 600000
        private const val LEGACY_ITERATIONS = 65536
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private val MAGIC_HEADER = "WHISK_E2EE".toByteArray(Charsets.US_ASCII)

        private val secureRandom: SecureRandom by lazy {
            try {
                SecureRandom.getInstanceStrong()
            } catch (e: Exception) {
                SecureRandom()
            }
        }

        fun generateFreshIv(): ByteArray {
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            return iv
        }

        fun generateFreshSalt(): ByteArray {
            val salt = ByteArray(SALT_LENGTH)
            secureRandom.nextBytes(salt)
            return salt
        }
    }

    suspend fun encryptPayload(plainText: String, passphrase: CharArray): String = withContext(Dispatchers.Default) {
        val bytes = encryptBytes(plainText.toByteArray(Charsets.UTF_8), passphrase)
        Base64.getEncoder().encodeToString(bytes)
    }

    suspend fun decryptPayload(encryptedBase64: String, passphrase: CharArray): String = withContext(Dispatchers.Default) {
        val combined = Base64.getDecoder().decode(encryptedBase64)
        val decrypted = decryptBytes(combined, passphrase)
        String(decrypted, Charsets.UTF_8)
    }

    suspend fun encryptBytes(rawBytes: ByteArray, passphrase: CharArray): ByteArray = withContext(Dispatchers.Default) {
        val salt = generateFreshSalt()
        val iv = generateFreshIv()

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, CURRENT_ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(rawBytes)

        // Versioned Header Format:
        // [10B Magic "WHISK_E2EE"][1B Version=0x01][4B Iterations=600000][16B Salt][12B IV][Ciphertext + Tag]
        val headerLength = MAGIC_HEADER.size + 1 + 4
        val combined = ByteArray(headerLength + salt.size + iv.size + cipherText.size)
        
        System.arraycopy(MAGIC_HEADER, 0, combined, 0, MAGIC_HEADER.size)
        combined[MAGIC_HEADER.size] = 0x01.toByte()
        
        val iterBuffer = java.nio.ByteBuffer.allocate(4).putInt(CURRENT_ITERATIONS).array()
        System.arraycopy(iterBuffer, 0, combined, MAGIC_HEADER.size + 1, 4)

        var offset = headerLength
        System.arraycopy(salt, 0, combined, offset, salt.size)
        offset += salt.size
        System.arraycopy(iv, 0, combined, offset, iv.size)
        offset += iv.size
        System.arraycopy(cipherText, 0, combined, offset, cipherText.size)
        
        combined
    }

    suspend fun decryptBytes(combined: ByteArray, passphrase: CharArray): ByteArray = withContext(Dispatchers.Default) {
        val headerLength = MAGIC_HEADER.size + 1 + 4
        val isHeaderPresent = combined.size >= headerLength &&
                combined.copyOfRange(0, MAGIC_HEADER.size).contentEquals(MAGIC_HEADER)

        val iterations: Int
        val saltOffset: Int
        
        if (isHeaderPresent) {
            val iterBuffer = java.nio.ByteBuffer.wrap(combined, MAGIC_HEADER.size + 1, 4)
            iterations = iterBuffer.int
            saltOffset = headerLength
        } else {
            iterations = LEGACY_ITERATIONS
            saltOffset = 0
        }

        if (combined.size < saltOffset + SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("Corrupted encrypted payload: insufficient bytes")
        }

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val cipherTextLength = combined.size - saltOffset - SALT_LENGTH - IV_LENGTH
        val cipherText = ByteArray(cipherTextLength)

        System.arraycopy(combined, saltOffset, salt, 0, SALT_LENGTH)
        System.arraycopy(combined, saltOffset + SALT_LENGTH, iv, 0, IV_LENGTH)
        System.arraycopy(combined, saltOffset + SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherTextLength)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        cipher.doFinal(cipherText)
    }
}
