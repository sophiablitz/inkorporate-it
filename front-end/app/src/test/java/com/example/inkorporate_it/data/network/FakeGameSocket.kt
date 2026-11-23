package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [GameSocket] for tests: no network and no JSON. Everything above it (GameClient
 * and the ViewModels) is the real code.
 *
 * The test plays the server: [serverSends] pretends a message arrived, and [sent]
 * lists every message the app sent.
 */
class FakeGameSocket : GameSocket {

    override val status = MutableStateFlow(ConnectionStatus.Disconnected)

    private val fromServer = MutableSharedFlow<CounterServerMessage>(extraBufferCapacity = 16)
    override val incoming: Flow<CounterServerMessage> = fromServer

    /** Every message the app sent, in order. */
    val sent = mutableListOf<CounterClientMessage>()

    var connectCalls = 0
        private set

    override fun connect() {
        connectCalls++
        status.value = ConnectionStatus.Connected
    }

    override suspend fun send(message: CounterClientMessage): Boolean {
        if (status.value != ConnectionStatus.Connected) return false
        sent += message
        return true
    }

    override fun close() {
        status.value = ConnectionStatus.Disconnected
    }

    /** Pretends the server sent [message]. */
    suspend fun serverSends(message: CounterServerMessage) = fromServer.emit(message)
}
