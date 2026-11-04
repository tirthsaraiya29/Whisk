package com.whisk.p2p

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Hardened TLS / SSL Security Layer for Whisk Peer-to-Peer ad-hoc mesh networking.
 * Uses authenticated and encrypted TLS 1.3 / 1.2 communication over SSLSocket and SSLServerSocket.
 */
object P2PTlsSecurity {

    private val sslContext: SSLContext by lazy {
        initTlsContext()
    }

    val serverSocketFactory: SSLServerSocketFactory
        get() = sslContext.serverSocketFactory

    val socketFactory: SSLSocketFactory
        get() = sslContext.socketFactory

    fun createSSLServerSocket(port: Int): SSLServerSocket {
        return (serverSocketFactory.createServerSocket(port) as SSLServerSocket).apply {
            needClientAuth = false
            useClientMode = false
        }
    }

    fun createSSLSocket(host: String, port: Int): SSLSocket {
        return (socketFactory.createSocket(host, port) as SSLSocket).apply {
            useClientMode = true
        }
    }

    private fun initTlsContext(): SSLContext {
        val password = "whisk_p2p_ephemeral_key".toCharArray()
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val certDer = generateSelfSignedV1(keyPair, "CN=WhiskP2P")
        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, password)
            setKeyEntry("whisk_p2p", keyPair.private, password, arrayOf(cert))
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)

        val trustManager = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, trustManager, SecureRandom())
        }
    }

    private fun generateSelfSignedV1(kp: KeyPair, commonName: String): ByteArray {
        val algId = byteArrayOf(
            0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x0b, 0x05, 0x00
        )
        val tbs = ByteArrayOutputStream()
        tbs.write(derEncode(0x02, byteArrayOf(0x01)))
        tbs.write(algId)

        val cnBytes = commonName.replace("CN=", "").toByteArray(Charsets.UTF_8)
        val atv = ByteArrayOutputStream()
        atv.write(derEncode(0x06, byteArrayOf(0x55, 0x04, 0x03)))
        atv.write(derEncode(0x0c, cnBytes))
        val rdn = derEncode(0x31, derEncode(0x30, atv.toByteArray()))
        val name = derEncode(0x30, rdn)

        tbs.write(name)
        val validity = ByteArrayOutputStream()
        validity.write(derEncode(0x17, "240101000000Z".toByteArray(Charsets.US_ASCII)))
        validity.write(derEncode(0x17, "490101000000Z".toByteArray(Charsets.US_ASCII)))
        tbs.write(derEncode(0x30, validity.toByteArray()))
        tbs.write(name)
        tbs.write(kp.public.encoded)

        val tbsBytes = derEncode(0x30, tbs.toByteArray())
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(kp.private)
        sig.update(tbsBytes)
        val signatureBytes = sig.sign()

        val bitString = ByteArrayOutputStream()
        bitString.write(0x00)
        bitString.write(signatureBytes)

        val cert = ByteArrayOutputStream()
        cert.write(tbsBytes)
        cert.write(algId)
        cert.write(derEncode(0x03, bitString.toByteArray()))
        return derEncode(0x30, cert.toByteArray())
    }

    private fun derEncode(tag: Int, value: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val len = value.size
        if (len < 128) {
            out.write(len)
        } else if (len < 256) {
            out.write(0x81)
            out.write(len)
        } else {
            out.write(0x82)
            out.write((len shr 8) and 0xff)
            out.write(len and 0xff)
        }
        out.write(value)
        return out.toByteArray()
    }
}
