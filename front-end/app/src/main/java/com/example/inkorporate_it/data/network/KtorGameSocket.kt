package com.example.inkorporate_it.data.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.decodeServer
import it.inkorporate.protocol.encodeClient
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
import kotlinx.serialization.SerializationException

/**
 * The real [GameSocket]: a Ktor WebSocket to the server's /ws route.
 *
 * It's the only part of the app that knows messages travel as JSON. It turns each
 * message into JSON with shared/'s helpers before sending, and each JSON text from
 * the server back into a message, so the server and the app use one codec.
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

    private val _incoming = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<ServerMessage> = _incoming.asSharedFlow()

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
                        if (frame is Frame.Text) receive(frame.readText())
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

    /** Decodes one text from the server and passes it on, or skips it if it's broken or a type this app doesn't know. */
    private suspend fun receive(text: String) {
        try {
            _incoming.emit(decodeServer(text))
        } catch (e: SerializationException) {
            Log.w(TAG, "Ignoring a message from the server that the app doesn't know: ${text.take(200)}")
        }
    }

    override suspend fun send(message: ClientMessage): Boolean {
        val current = session ?: return false
        current.send(Frame.Text(encodeClient(message)))
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
