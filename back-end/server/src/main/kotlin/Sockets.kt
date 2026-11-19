package it.inkorporate

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import it.inkorporate.protocol.decodeCounterClient
import it.inkorporate.protocol.encodeCounterServer
import kotlinx.serialization.SerializationException
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds   // the server pings each phone every 15 s
        timeout = 15.seconds      // no reply within 15 s: close the connection
        maxFrameSize = 64 * 1024L // 64 KB is plenty for the counter messages. Revisit for strokes.
    }

    // One counter for the whole server, shared by every phone that connects.
    val counter = Counter()

    routing {
        webSocket("/ws") {
            val who = call.request.origin.remoteAddress
            log.info("WebSocket opened from $who")
            try {
                // Send the current value straight away, so the screen isn't blank.
                sendCounterValue(counter.current())

                for (frame in incoming) {
                    if (frame !is Frame.Text) continue // our messages are always text (JSON)
                    val text = frame.readText()
                    log.info("Received from $who: ${text.take(200)}")

                    val message = try {
                        decodeCounterClient(text) // the shared codec, the same one the app uses
                    } catch (e: SerializationException) {
                        // Unknown type or broken JSON: log it and keep the connection open.
                        log.warn("Ignoring a message from $who that isn't a counter message: ${text.take(200)}")
                        continue
                    }

                    // Replies only to the phone that sent the message. Broadcasting to everyone is a later feature.
                    when (message) {
                        is CounterClientMessage.CounterIncrement -> sendCounterValue(counter.increment())
                        is CounterClientMessage.CounterGet -> sendCounterValue(counter.current())
                    }
                }
            } finally {
                log.info("WebSocket closed from $who")
            }
        }
    }
}

/** Sends {"type":"counter_value","value":N} to this phone. */
private suspend fun DefaultWebSocketServerSession.sendCounterValue(value: Int) {
    send(Frame.Text(encodeCounterServer(CounterServerMessage.CounterValue(value))))
}
