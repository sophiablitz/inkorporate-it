package com.example.inkorporate_it.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import kotlinx.coroutines.CancellationException

/*
 * TEMPORARY test connection for the server test screen (#139).
 * Opens one WebSocket to the local server and logs when it opens and closes.
 * No messages yet, those come in #142.
 */
object ServerConnection {
    // 10.0.2.2 is the computer running the emulator. Becomes wss:// in #151.
    private const val SERVER_URL = "ws://10.0.2.2:8080/ws"
    private const val TAG = "ServerConnection"

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    /** Connects and stays connected until the server closes it or the caller is cancelled. */
    suspend fun connect() {
        try {
            client.webSocket(SERVER_URL) {
                Log.d(TAG, "Connected to $SERVER_URL")
                try {
                    for (frame in incoming) {
                        // No messages yet (#142), so ignore anything the server sends.
                    }
                } finally {
                    Log.d(TAG, "Disconnected from $SERVER_URL")
                }
            }
        } catch (e: CancellationException) {
            throw e // the app is closing the connection on purpose, not an error
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't connect to $SERVER_URL", e)
        }
    }
}
