package com.example.inkorporate_it.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/*
 * A GameSocket for unit tests: no network.
 * - Call receive(...) to act as the server sending a message.
 * - Read `sent` to check what GameClient sent.
 */
class FakeGameSocket : GameSocket {

    private val fromServer = MutableSharedFlow<String>(extraBufferCapacity = 64)

    /** Every message sent through this socket, in order. */
    val sent = mutableListOf<String>()

    var connectedUrl: String? = null
        private set

    var isClosed = false
        private set

    override val incoming: Flow<String> = fromServer

    override suspend fun connect(url: String) {
        connectedUrl = url
        isClosed = false
    }

    override suspend fun send(text: String) {
        sent += text
    }

    override suspend fun close() {
        isClosed = true
    }

    /** Pretend the server sent this message. */
    suspend fun receive(text: String) {
        fromServer.emit(text)
    }
}
