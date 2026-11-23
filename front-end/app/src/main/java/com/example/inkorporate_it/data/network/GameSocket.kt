package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's connection to the server. It sends and receives messages (the types from
 * shared/) and hides how they travel: only the real [KtorGameSocket] knows about JSON.
 *
 * An interface, so tests can plug in FakeGameSocket instead and play the server with
 * plain messages, with no JSON and no server running.
 */
interface GameSocket {

    /** Whether the connection is open right now. */
    val status: StateFlow<ConnectionStatus>

    /** Every message the server sends, in order. */
    val incoming: Flow<CounterServerMessage>

    /**
     * Opens the connection if it isn't open or opening already. Safe to call any
     * number of times. Once open, it stays open until [close] or the network drops.
     */
    fun connect()

    /** Sends [message] to the server. Returns false if there's no open connection. */
    suspend fun send(message: CounterClientMessage): Boolean

    /** Closes the connection. */
    fun close()
}

enum class ConnectionStatus { Disconnected, Connecting, Connected }
