package it.inkorporate

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds   // the server pings each phone every 15 s
        timeout = 15.seconds      // no reply within 15 s: close the connection
        maxFrameSize = 64 * 1024L // 64 KB is plenty for the counter messages. Revisit for strokes.
    }

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
