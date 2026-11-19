package com.example.inkorporate_it.data.network

import kotlinx.coroutines.flow.Flow

/*
 * The raw connection to the game server: text in, text out.
 *
 * GameClient talks only to this interface, never to Ktor directly, so:
 * - KtorGameSocket is the real WebSocket connection
 * - FakeGameSocket (in the tests) lets tests push server messages and read what was sent
 *
 * Messages are JSON text. Turning them into objects is GameClient's job, not this one's.
 */
interface GameSocket {

    /** Every text message the server sends, in order. Ends when the connection closes. */
    val incoming: Flow<String>

    /** Open the connection. Throws if the server can't be reached. */
    suspend fun connect(url: String)

    /** Send one text message to the server. */
    suspend fun send(text: String)

    /** Close the connection. Safe to call more than once. */
    suspend fun close()
}
