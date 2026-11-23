package com.example.inkorporate_it.data.network

import android.util.Log
import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The menu of everything the app can ask the server for, and the stream of what the
 * server says back. ViewModels call these functions; they never see sockets.
 *
 * It wraps a [GameSocket]: each call becomes a message, and the socket sends it. It
 * knows nothing about JSON; that's all inside KtorGameSocket. In tests, pass a
 * FakeGameSocket and play the server with plain messages.
 */
class GameClient(private val socket: GameSocket) {

    /** Whether the app is connected right now. */
    val status: StateFlow<ConnectionStatus> = socket.status

    /** Every message the server sends. */
    val events: Flow<CounterServerMessage> = socket.incoming

    /** Opens the connection if it isn't open already. Safe to call any number of times. */
    fun connect() = socket.connect()

    /** The player tapped the button: sends counter_increment. */
    suspend fun increment() = send(CounterClientMessage.CounterIncrement)

    /** Asks for the current value without changing it: sends counter_get. */
    suspend fun refresh() = send(CounterClientMessage.CounterGet)

    private suspend fun send(message: CounterClientMessage) {
        if (!socket.send(message)) {
            Log.w(TAG, "Not connected, so this wasn't sent: $message")
        }
    }

    private companion object {
        const val TAG = "GameClient"
    }
}
