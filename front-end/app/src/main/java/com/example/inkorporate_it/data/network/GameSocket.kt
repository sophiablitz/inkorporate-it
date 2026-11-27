package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's one connection to the server. It sends and receives game messages (the types
 * from shared/) and hides how they travel: only the real [KtorGameSocket] knows about JSON
 * (Sophia's call: serialization stays inside the socket).
 *
 * An interface, so tests can plug in FakeGameSocket, and the app can play against
 * StubServerSocket, with plain messages: no JSON and no server running.
 */
interface GameSocket {

    /** Whether the connection is open right now. */
    val status: StateFlow<ConnectionStatus>

    /** Every message the server sends, in order. */
    val incoming: Flow<ServerMessage>

    /**
     * Opens the connection if it isn't open or opening already. Safe to call any
     * number of times. Once open, it stays open until [close] or the network drops.
     */
    fun connect()

    /** Sends [message] to the server. Returns false if there's no open connection. */
    suspend fun send(message: ClientMessage): Boolean

    /** Closes the connection. */
    fun close()
}

enum class ConnectionStatus { Disconnected, Connecting, Connected }
