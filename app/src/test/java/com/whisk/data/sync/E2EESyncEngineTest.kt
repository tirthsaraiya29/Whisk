package com.whisk.data.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class E2EESyncEngineTest {

    private val engine = E2EESyncEngine()
    private val passphrase = "super_secret_master_key_123!".toCharArray()

    @Test
    fun encryptAndDecryptPayload_matchesOriginalString() = runTest {
        val original = "# Neural Graph Knowledge Base\n\n- Node 1: Memory\n- Node 2: Vector Search"
        val encrypted = engine.encryptPayload(original, passphrase)
        val decrypted = engine.decryptPayload(encrypted, passphrase)

        assertEquals(original, decrypted)
    }

    @Test
    fun encryptAndDecryptBytes_matchesOriginalByteArray() = runTest {
        val original = "Binary vault database payload buffer".toByteArray(Charsets.UTF_8)
        val encrypted = engine.encryptBytes(original, passphrase)
        val decrypted = engine.decryptBytes(encrypted, passphrase)

        assertTrue(original.contentEquals(decrypted))
    }

    @Test
    fun gcmNonceUniqueness_encryptingIdenticalPlaintext_producesUniqueIVsAndCiphertexts() = runTest {
        val plaintext = "Identical message repeated for GCM nonce test"
        val ivSet = mutableSetOf<String>()
        val cipherSet = mutableSetOf<String>()

        val iterations = 50
        for (i in 0 until iterations) {
            val encryptedBase64 = engine.encryptPayload(plaintext, passphrase)
            cipherSet.add(encryptedBase64)

            // Extract the 12-byte IV (bytes 16..27)
            val bytes = Base64.getDecoder().decode(encryptedBase64)
            val iv = bytes.copyOfRange(16, 28)
            val ivHex = iv.joinToString("") { "%02x".format(it) }
            ivSet.add(ivHex)
        }

        // All IVs and ciphertexts must be completely unique with zero collisions
        assertEquals("Every encryption must produce a distinct 96-bit IV", iterations, ivSet.size)
        assertEquals("Every encryption must produce a distinct ciphertext", iterations, cipherSet.size)
    }

    @Test
    fun decryptCorruptedPayload_throwsIllegalArgumentException() = runTest {
        val shortPayload = Base64.getEncoder().encodeToString(ByteArray(10))
        var caught = false
        try {
            engine.decryptPayload(shortPayload, passphrase)
        } catch (e: IllegalArgumentException) {
            caught = true
        }
        assertTrue("Should throw IllegalArgumentException on short payload", caught)
    }

    @Test
    fun decryptTamperedCiphertext_failsAuthentication() = runTest {
        val original = "Secret vault data"
        val encryptedBase64 = engine.encryptPayload(original, passphrase)
        val bytes = Base64.getDecoder().decode(encryptedBase64)

        // Tamper with the last byte of the ciphertext (authentication tag)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        val tamperedBase64 = Base64.getEncoder().encodeToString(bytes)

        var failed = false
        try {
            engine.decryptPayload(tamperedBase64, passphrase)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Tampered ciphertext must fail authentication", failed)
    }

    @Test
    fun legacy65536IterationsPayload_decryptsSuccessfully() = runTest {
        // Construct legacy format payload without WHISK_E2EE header (65536 iterations)
        val plainText = "Legacy backup payload"
        val salt = E2EESyncEngine.generateFreshSalt()
        val iv = E2EESyncEngine.generateFreshIv()

        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(passphrase, salt, 65536, 256)
        val secretKey = javax.crypto.spec.SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        val legacyBase64 = Base64.getEncoder().encodeToString(combined)
        val decrypted = engine.decryptPayload(legacyBase64, passphrase)

        assertEquals("Legacy 65536-iteration payload must decrypt successfully", plainText, decrypted)
    }
}
