package com.example.inkorporate_it.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/*
 * The real GameSocket: a WebSocket connection made with Ktor's client.
 *
 * STUB: Ktor's client isn't a dependency yet. To build it:
 * - add ktor-client-okhttp and ktor-client-websockets to the version catalog
 * - connect() opens client.webSocketSession(url)
 * - incoming reads the session's Frame.Text frames as Strings
 * - send() sends Frame.Text(text); close() closes the session
 */
class KtorGameSocket : GameSocket {

    override val incoming: Flow<String> = emptyFlow() // TODO: the session's text frames

    override suspend fun connect(url: String) {
        // TODO
    }

    override suspend fun send(text: String) {
        // TODO
    }

    override suspend fun close() {
        // TODO
    }
}
