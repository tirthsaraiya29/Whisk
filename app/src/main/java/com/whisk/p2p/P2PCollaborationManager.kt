package com.whisk.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

@Serializable
data class P2PPeer(
    val id: String,
    val name: String,
    val hostAddress: String,
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

@Serializable
data class P2PSyncPayload(
    val senderDeviceId: String,
    val senderName: String,
    val notes: List<P2PNoteTransfer>
)

@Serializable
data class P2PNoteTransfer(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val isArchived: Boolean,
    val isFavorite: Boolean,
    val tags: List<String>
)

data class P2PUiState(
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val discoveredPeers: List<P2PPeer> = emptyList(),
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null
)

class P2PCollaborationManager(
    private val context: Context,
    private val notesRepository: NotesRepository,
    private val deviceId: String = UUID.randomUUID().toString(),
    private val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(P2PUiState())
    val uiState: StateFlow<P2PUiState> = _uiState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serviceType = "_whisk-sync._tcp."
    private var localPort = 0

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            _uiState.update { it.copy(isAdvertising = true) }
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            _uiState.update { it.copy(isAdvertising = false) }
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            _uiState.update { it.copy(isAdvertising = false) }
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            _uiState.update { it.copy(isAdvertising = false) }
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            _uiState.update { it.copy(isDiscovering = true) }
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceType == serviceType && serviceInfo.serviceName.contains("Whisk")) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val host = resolvedInfo.host.hostAddress ?: return
                        val peer = P2PPeer(
                            id = resolvedInfo.serviceName,
                            name = resolvedInfo.serviceName,
                            hostAddress = host,
                            port = resolvedInfo.port
                        )
                        _uiState.update { state ->
                            val existing = state.discoveredPeers.filter { it.hostAddress != host }
                            state.copy(discoveredPeers = existing + peer)
                        }
                    }
                })
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            _uiState.update { state ->
                state.copy(discoveredPeers = state.discoveredPeers.filter { it.id != serviceInfo.serviceName })
            }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            _uiState.update { it.copy(isDiscovering = false) }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            _uiState.update { it.copy(isDiscovering = false) }
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            _uiState.update { it.copy(isDiscovering = false) }
        }
    }

    fun startServerAndDiscovery(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(0)
                localPort = serverSocket!!.localPort

                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "Whisk-${deviceName.take(12)}"
                    serviceType = this@P2PCollaborationManager.serviceType
                    port = localPort
                }

                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

                listenForIncomingConnections(scope)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForIncomingConnections(scope: CoroutineScope) {
        serverJob = scope.launch(Dispatchers.IO) {
            while (serverSocket != null && !serverSocket!!.isClosed) {
                try {
                    val client = serverSocket!!.accept()
                    scope.launch(Dispatchers.IO) { handleIncomingSync(client) }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private suspend fun handleIncomingSync(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val line = reader.readLine()
            if (line != null) {
                val payload = json.decodeFromString<P2PSyncPayload>(line)
                var mergedCount = 0

                payload.notes.forEach { remoteNote ->
                    val local = notesRepository.getNoteById(remoteNote.id)
                    if (local == null || remoteNote.updatedAt > local.updatedAt) {
                        notesRepository.saveNote(
                            Note(
                                id = remoteNote.id,
                                title = remoteNote.title,
                                body = remoteNote.body,
                                updatedAt = remoteNote.updatedAt,
                                createdAt = local?.createdAt ?: remoteNote.updatedAt,
                                isArchived = remoteNote.isArchived,
                                isFavorite = remoteNote.isFavorite,
                                tags = remoteNote.tags
                            )
                        )
                        mergedCount++
                    }
                }

                writer.println("OK $mergedCount")
                _uiState.update { it.copy(lastSyncMessage = "Synced $mergedCount notes from ${payload.senderName}") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    suspend fun syncWithPeer(peer: P2PPeer) = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isSyncing = true, lastSyncMessage = "Connecting to ${peer.name}...") }
        try {
            val notes = notesRepository.getAllNotesDirect()
            val transfers = notes.map {
                P2PNoteTransfer(
                    id = it.id,
                    title = it.title,
                    body = it.body,
                    updatedAt = it.updatedAt,
                    isArchived = it.isArchived,
                    isFavorite = it.isFavorite,
                    tags = it.tags
                )
            }
            val payload = P2PSyncPayload(
                senderDeviceId = deviceId,
                senderName = deviceName,
                notes = transfers
            )

            val socket = Socket(peer.hostAddress, peer.port)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.println(json.encodeToString(payload))
            val response = reader.readLine()
            socket.close()

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncMessage = "Synced successfully with ${peer.name} ($response)"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncMessage = "Sync failed with ${peer.name}: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stop() {
        try {
            nsdManager.unregisterService(registrationListener)
            nsdManager.stopServiceDiscovery(discoveryListener)
            serverJob?.cancel()
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore cleanup errors
        }
    }
}
