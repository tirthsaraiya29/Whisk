package com.whisk.p2p

import android.content.ContextWrapper
import com.whisk.DummyWhiskDatabase
import com.whisk.data.repository.NotesRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class P2PCollaborationManagerTest {

    @Test
    fun testPeerAuthorizationScoping() {
        val mockContext = ContextWrapper(null)
        val dummyDb = DummyWhiskDatabase()
        val dummyNotesRepository = NotesRepository(
            dummyDb.noteDao(),
            dummyDb.folderDao(),
            dummyDb.taskDao(),
        )

        val p2pManager = P2PCollaborationManager(
            context = mockContext,
            notesRepository = dummyNotesRepository,
            deviceId = "local-device-id",
        )

        assertTrue("Own device ID should be authorized", p2pManager.isPeerAuthorized("local-device-id"))
        assertFalse("Unknown peer should not be authorized by default", p2pManager.isPeerAuthorized("remote-peer-123"))

        p2pManager.authorizePeerDevice("remote-peer-123")
        assertTrue("Explicitly authorized peer should be authorized", p2pManager.isPeerAuthorized("remote-peer-123"))

        p2pManager.revokePeerDevice("remote-peer-123")
        assertFalse("Revoked peer should no longer be authorized", p2pManager.isPeerAuthorized("remote-peer-123"))
    }
}
