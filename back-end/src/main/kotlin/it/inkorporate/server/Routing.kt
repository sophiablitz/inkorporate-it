package it.inkorporate.server

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import it.inkorporate.protocol.ErrorCode
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.decodeClient
import it.inkorporate.protocol.encodeServer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun Application.configureRouting() {
    // Everyone currently connected to /game. For now this is one shared canvas; rooms
    // (join_room etc.) will replace it with one canvas per room.
    val sessions = LinkedHashSet<DefaultWebSocketServerSession>()

    // The server's copy of the canvas, for catching up late joiners.
    val canvas = CanvasState()

    // One lock around "update the canvas + send to everyone" and "send a snapshot + join".
    // Without it, a joiner could get a batch both inside its snapshot and again as a live
    // message, or miss one between the two.
    val lock = Mutex()

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        // Drawing relay, protocol v0 (shared/src/main/kotlin/it/inkorporate/protocol/).
        //
        // Phones send `stroke` (in ~50 ms batches, last one done = true), `undo` and `clear`.
        // The server:
        //   1. on connect, sends the new phone a `canvas_snapshot` of everything drawn so far
        //      (including strokes still being drawn),
        //   2. updates its canvas copy, then
        //   3. relays the message to every OTHER phone. Strokes are relayed under a
        //      server-assigned strokeId so ids never collide between phones.
        //
        // Not yet: rooms, and only letting the current drawer draw (NOT_DRAWER).
        webSocket("/game") {
            // This phone's strokeId -> server strokeId, for strokes it is still drawing.
            val idMap = HashMap<Int, Int>()

            lock.withLock {
                send(Frame.Text(encodeServer(canvas.snapshot())))
                sessions += this
                println("game: joined, ${sessions.size} connected")
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    val msg = runCatching { decodeClient(text) }.getOrElse {
                        send(Frame.Text(encodeServer(
                            ServerMessage.Error(ErrorCode.BAD_MESSAGE, "Could not read: ${it.message}")
                        )))
                        continue
                    }

                    lock.withLock {
                        // Rooms, turns and guessing are other people's pieces; ignore for now.
                        val relay = canvas.handle(msg, idMap) ?: run {
                            println("game: not handled yet: $msg")
                            return@withLock
                        }
                        val out = encodeServer(relay)
                        for (other in sessions) {
                            if (other != this) runCatching { other.send(Frame.Text(out)) }
                        }
                    }
                }
            } finally {
                // Runs on normal disconnect AND on errors, so dead sessions never pile up.
                lock.withLock { sessions -= this }
                println("game: left, ${sessions.size} connected")
            }
        }
    }
}
