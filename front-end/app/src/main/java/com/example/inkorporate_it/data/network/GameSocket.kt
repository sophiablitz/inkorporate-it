package com.example.inkorporate_it.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's connection to the server: it moves text back and forth and knows
 * nothing about what the text means. [GameClient] turns it into messages.
 *
 * An interface, so tests can plug in FakeGameSocket instead of the real
 * [KtorGameSocket] and still check the real JSON, with no server running.
 */
interface GameSocket {

    /** Whether the connection is open right now. */
    val status: StateFlow<ConnectionStatus>

    /** Every text message the server sends, in order. */
    val incoming: Flow<String>

    /**
     * Opens the connection if it isn't open or opening already. Safe to call any
     * number of times. Once open, it stays open until [close] or the network drops.
     */
    fun connect()

    /** Sends [text] to the server. Returns false if there's no open connection. */
    suspend fun send(text: String): Boolean

    /** Closes the connection. */
    fun close()
}

enum class ConnectionStatus { Disconnected, Connecting, Connected }
