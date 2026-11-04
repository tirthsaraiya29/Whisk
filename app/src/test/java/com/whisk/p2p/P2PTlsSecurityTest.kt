package com.whisk.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

class P2PTlsSecurityTest {

    @Test
    fun sslServerSocket_createsEncryptedSocketSuccessfully() {
        val serverSocket = P2PTlsSecurity.createSSLServerSocket(0)
        assertNotNull(serverSocket)
        assert(serverSocket is SSLServerSocket)
        val port = serverSocket.localPort
        assert(port > 0)
        serverSocket.close()
    }

    @Test
    fun tlsClientServerHandshake_transfersEncryptedData() = runTest {
        val server = P2PTlsSecurity.createSSLServerSocket(0)
        val port = server.localPort

        var receivedMessage = ""
        val serverJob = launch(Dispatchers.IO) {
            val clientSocket = server.accept() as SSLSocket
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(clientSocket.getOutputStream(), true)
            receivedMessage = reader.readLine() ?: ""
            writer.println("ENCRYPTED_ACK: $receivedMessage")
            clientSocket.close()
            server.close()
        }

        withContext(Dispatchers.IO) {
            val client = P2PTlsSecurity.createSSLSocket("localhost", port)
            val writer = PrintWriter(client.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))

            writer.println("WHISK_TLS_TEST_PAYLOAD")
            val response = reader.readLine()
            client.close()

            assertEquals("ENCRYPTED_ACK: WHISK_TLS_TEST_PAYLOAD", response)
        }

        serverJob.join()
        assertEquals("WHISK_TLS_TEST_PAYLOAD", receivedMessage)
    }
}
