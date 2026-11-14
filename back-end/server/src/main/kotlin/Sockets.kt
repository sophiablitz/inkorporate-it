package it.inkorporate

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun Application.configureSockets() {
    install(WebSockets)

    routing {
        webSocket("/ws") {
            val who = call.request.origin.remoteHost
            log.info("WebSocket opened from $who")
            try {
                for (frame in incoming) {
                    // No messages yet. They come in #142, so ignore anything sent.
                }
            } finally {
                log.info("WebSocket closed from $who")
            }
        }
    }
}
