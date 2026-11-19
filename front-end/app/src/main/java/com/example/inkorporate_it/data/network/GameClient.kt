package com.example.inkorporate_it.data.network

import android.util.Log
import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import it.inkorporate.protocol.decodeCounterServer
import it.inkorporate.protocol.encodeCounterClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException

/**
 * The menu of everything the app can ask the server for, and the stream of what the
 * server says back. ViewModels call these functions; they never see JSON or sockets.
 *
 * It wraps a [GameSocket]: each call becomes a message, the shared codec in shared/
 * turns it into JSON, and the socket sends it. In tests, pass a FakeGameSocket and
 * everything here, the JSON included, runs for real without a server.
 */
class GameClient(private val socket: GameSocket) {

    /** Whether the app is connected right now. */
    val status: StateFlow<ConnectionStatus> = socket.status

    /** Every message the server sends, already decoded. */
    val events: Flow<CounterServerMessage> = socket.incoming.mapNotNull { text ->
        try {
            decodeCounterServer(text)
        } catch (e: SerializationException) {
            Log.w(TAG, "Ignoring a message from the server that isn't a counter message: ${text.take(200)}")
            null
        }
    }

    /** Opens the connection if it isn't open already. Safe to call any number of times. */
    fun connect() = socket.connect()

    /** The player tapped the button: sends counter_increment. */
    suspend fun increment() = send(CounterClientMessage.CounterIncrement)

    /** Asks for the current value without changing it: sends counter_get. */
    suspend fun refresh() = send(CounterClientMessage.CounterGet)

    private suspend fun send(message: CounterClientMessage) {
        val text = encodeCounterClient(message)
        if (!socket.send(text)) {
            Log.w(TAG, "Not connected, so this wasn't sent: $text")
        }
    }

    private companion object {
        const val TAG = "GameClient"
    }
}
