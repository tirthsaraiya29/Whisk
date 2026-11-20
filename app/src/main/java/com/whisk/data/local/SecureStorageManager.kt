package com.whisk.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorageManager(context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            createEncryptedSharedPreferences(context)
        } catch (e: Exception) {
            // Only attempt destructive recovery if KeyStore or AEAD encryption keys are corrupted
            val isKeyStoreCorruption = e is java.security.KeyStoreException ||
                    e is javax.crypto.AEADBadTagException ||
                    e is java.security.GeneralSecurityException ||
                    e.cause is java.security.KeyStoreException

            if (isKeyStoreCorruption) {
                try {
                    context.deleteSharedPreferences("whisk_secure_vault_prefs")
                    createEncryptedSharedPreferences(context)
                } catch (retryEx: Exception) {
                    throw SecurityException("Secure key storage recovery failed. Secrets cannot be stored unencrypted.", retryEx)
                }
            } else {
                throw SecurityException("Secure key storage initialization error: ${e.message}", e)
            }
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "whisk_secure_vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveSecret(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    fun getSecret(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun removeSecret(key: String) {
        sharedPreferences.edit { remove(key) }
    }
}
