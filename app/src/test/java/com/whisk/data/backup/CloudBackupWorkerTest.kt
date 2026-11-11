package com.whisk.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupWorkerTest {

    @Test
    fun testDestinationUrlValidation() {
        // Valid HTTPS endpoint
        assertTrue(CloudBackupWorker.validateDestinationUrl("https://backup.example.com/vault"))

        // Valid local HTTP endpoint
        assertTrue(CloudBackupWorker.validateDestinationUrl("http://localhost:8080/backup"))
        assertTrue(CloudBackupWorker.validateDestinationUrl("http://127.0.0.1:8080/backup"))
        assertTrue(CloudBackupWorker.validateDestinationUrl("http://10.0.2.2:8080/backup"))

        // Insecure remote HTTP endpoint (MUST BE REJECTED)
        assertFalse(CloudBackupWorker.validateDestinationUrl("http://unencrypted-remote-server.com/backup"))

        // Invalid scheme / malformed URLs
        assertFalse(CloudBackupWorker.validateDestinationUrl("ftp://server.com/backup"))
        assertFalse(CloudBackupWorker.validateDestinationUrl("   "))
    }

    @Test
    fun testSchedulingValidationRequiresEncryptionKeyForCloudBackup() {
        val dummyContext = android.content.ContextWrapper(null)

        // Scheduling with insecure remote HTTP URL must fail
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupWorker.schedulePeriodicBackup(
                context = dummyContext,
                destinationUrl = "http://remote-server.com/backup",
                encryptionKey = "valid-passphrase",
            )
        }

        // Scheduling with destination URL but missing encryption key must fail
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupWorker.schedulePeriodicBackup(
                context = dummyContext,
                destinationUrl = "https://secure.example.com/backup",
                encryptionKey = "",
            )
        }
    }
}
