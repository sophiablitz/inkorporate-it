package com.example.inkorporate_it.data.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The real [GameSocket]: a Ktor WebSocket to the server's /ws route.
 *
 * The connection is a loop that keeps listening until the call ends. That loop runs
 * in this class's own [scope], not in a screen's, so it stays open while screens come
 * and go. InkorporateApp keeps the one instance for as long as the app runs.
 */
class KtorGameSocket(
    private val url: String = SERVER_URL,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GameSocket {

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    /** The open socket, or null while disconnected. */
    @Volatile
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _status = MutableStateFlow(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    @Synchronized
    override fun connect() {
        if (connectionJob?.isActive == true) return // already open or opening
        connectionJob = scope.launch { run() }
    }

    /** Connects, then passes on everything the server sends until the connection ends. */
    private suspend fun run() {
        _status.value = ConnectionStatus.Connecting
        try {
            client.webSocket(url) {
                session = this
                _status.value = ConnectionStatus.Connected
                Log.d(TAG, "Connected to $url")
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) _incoming.emit(frame.readText())
                    }
                } finally {
                    session = null
                    Log.d(TAG, "Disconnected from $url")
                }
            }
        } catch (e: CancellationException) {
            throw e // the app is closing the connection on purpose, not an error
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't connect to $url", e)
        } finally {
            _status.value = ConnectionStatus.Disconnected
        }
    }

    override suspend fun send(text: String): Boolean {
        val current = session ?: return false
        current.send(Frame.Text(text))
        return true
    }

    override fun close() {
        connectionJob?.cancel()
    }

    private companion object {
        const val TAG = "KtorGameSocket"

        // 10.0.2.2 is the computer running the emulator. Becomes wss:// in #151.
        const val SERVER_URL = "ws://10.0.2.2:8080/ws"
    }
}
