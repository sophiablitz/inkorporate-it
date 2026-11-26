package com.example.inkorporate_it.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [GameSocket] for tests: no network at all. Everything above it (GameClient's
 * menu and its JSON) is the real code.
 *
 * The test plays the server: [serverSends] pretends a message arrived, and [sent]
 * lists every text the app sent.
 */
class FakeGameSocket : GameSocket {

    override val status = MutableStateFlow(ConnectionStatus.Disconnected)

    private val fromServer = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val incoming: Flow<String> = fromServer

    /** Every text the app sent, in order. */
    val sent = mutableListOf<String>()

    var connectCalls = 0
        private set

    override fun connect() {
        connectCalls++
        status.value = ConnectionStatus.Connected
    }

    override suspend fun send(text: String): Boolean {
        if (status.value != ConnectionStatus.Connected) return false
        sent += text
        return true
    }

    override fun close() {
        status.value = ConnectionStatus.Disconnected
    }

    /** Pretends the server sent [text]. */
    suspend fun serverSends(text: String) = fromServer.emit(text)
}
