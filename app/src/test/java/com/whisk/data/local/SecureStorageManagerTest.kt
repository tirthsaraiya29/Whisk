package com.whisk.data.local

import android.content.ContextWrapper
import org.junit.Assert.assertThrows
import org.junit.Test

class SecureStorageManagerTest {

    @Test
    fun testSecureStorageInitializationFailsWithoutSilentFallbackToUnencryptedPrefs() {
        val dummyContext = ContextWrapper(null)
        val secureStorageManager = SecureStorageManager(dummyContext)

        // Attempting to access sharedPreferences on a non-Android mock context without KeyStore
        // must throw a SecurityException rather than silently storing secrets in unencrypted SharedPreferences.
        assertThrows(SecurityException::class.java) {
            secureStorageManager.saveSecret("test_key", "test_value")
        }
    }
}
