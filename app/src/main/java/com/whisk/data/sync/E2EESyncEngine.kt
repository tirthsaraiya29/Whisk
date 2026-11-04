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
@Suppress("kotlin.lang.security.gcm-detection")
class E2EESyncEngine {

    companion object {
        private const val ITERATIONS = 65536
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12 // 96-bit IV recommended by NIST SP 800-38D for GCM

        private val secureRandom: SecureRandom by lazy {
            try {
                SecureRandom.getInstanceStrong()
            } catch (e: Exception) {
                SecureRandom()
            }
        }

        /**
         * Generates a fresh, non-reusable 12-byte (96-bit) IV using cryptographically secure random bytes.
         */
        fun generateFreshIv(): ByteArray {
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            return iv
        }

        /**
         * Generates a fresh 16-byte random salt for PBKDF2 key derivation.
         */
        fun generateFreshSalt(): ByteArray {
            val salt = ByteArray(SALT_LENGTH)
            secureRandom.nextBytes(salt)
            return salt
        }
    }

    suspend fun encryptPayload(plainText: String, passphrase: CharArray): String = withContext(Dispatchers.Default) {
        val salt = generateFreshSalt()
        val iv = generateFreshIv()

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Format: [16 bytes salt][12 bytes iv][cipherText with 128-bit GCM tag]
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

    suspend fun encryptBytes(rawBytes: ByteArray, passphrase: CharArray): ByteArray = withContext(Dispatchers.Default) {
        val salt = generateFreshSalt()
        val iv = generateFreshIv()

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(rawBytes)

        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)
        combined
    }

    suspend fun decryptBytes(combined: ByteArray, passphrase: CharArray): ByteArray = withContext(Dispatchers.Default) {
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

        cipher.doFinal(cipherText)
    }
}
