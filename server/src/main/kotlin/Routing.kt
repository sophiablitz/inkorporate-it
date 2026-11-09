package com.blitz

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Application.configureRouting() {
    // All active rooms. A client picks a room with the `?room=<code>` query param on
    // /relay; the registry creates the room on first join and discards it when empty.
    val rooms = RoomRegistry()

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        // Echo socket: a quick manual check that the console/client can reach the server.
        // Not part of the drawing protocol — kept for connectivity debugging.
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID (and I checked it): $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
            println("client left");
        }

        // Live-streaming relay.
        //
        // Clients exchange small JSON text frames that describe a drawing as it happens:
        //   {"type":"begin","id":..,"brush":..,"color":..,"size":..,"epsilon":..}
        //   {"type":"points","id":..,"points":[{x,y,t,p?},..]}   (sent repeatedly while drawing)
        //   {"type":"end","id":..}
        //   {"type":"clear"}
        // Back-compat: a message with no "type" that has "points" is treated as one
        // complete stroke (the old single-frame protocol).
        //
        // The server:
        //   1. updates the room's canvas so it always knows the current drawing, then
        //   2. forwards the raw frame to every OTHER client (unchanged bytes), and
        //   3. on connect, sends the new client a {"type":"snapshot","strokes":[..]}
        //      so a late joiner immediately sees everything drawn so far, including
        //      strokes still in progress.
        //
        // Which room: the `?room=<code>` query param. A missing or blank code falls back
        // to "default", so an old client with no param still works (same single shared
        // canvas as before).
        webSocket("/relay") {
            val code = call.request.queryParameters["room"]?.takeIf { it.isNotBlank() } ?: "default"
            val room = rooms.join(code, this)
            println("relay[$code]: joined, ${room.size} in room, ${rooms.size} rooms")

            // Catch the new client up with this room's canvas before anything else.
            send(Frame.Text(room.snapshotJson()))

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (isClientControlFrame(text)) {
                            // CONTROL plane: inbound control frames are `hello` and `undo`.
                            val msg = runCatching {
                                controlJson.decodeFromString<ControlMessage>(text)
                            }.getOrNull()
                            when (msg) {
                                is Hello -> {
                                    val me = room.hello(this, msg.name, msg.color)
                                    // Catch this client up on who's already here...
                                    send(Frame.Text(room.rosterJson()))
                                    // ...and tell everyone else someone joined.
                                    val join = controlJson.encodeToString<ControlMessage>(Join(me))
                                    room.broadcast(from = this, text = join)
                                    println("relay[$code]: hello ${me.id} '${me.name}'")
                                }
                                is UndoRequest -> {
                                    // Undo the sender's own last stroke, then tell EVERYONE
                                    // (including the sender) to drop it so all canvases match.
                                    val removed = room.undo(this)
                                    if (removed != null) {
                                        val ack = controlJson.encodeToString<ControlMessage>(Undo(removed))
                                        room.broadcast(from = this, text = ack)
                                        send(Frame.Text(ack))
                                        println("relay[$code]: undo $removed")
                                    }
                                }
                                else -> { /* unknown control frame; ignore */ }
                            }
                        } else {
                            // DATA plane: opaque stroke frame, forwarded verbatim.
                            room.relay(from = this, text = text)
                        }
                    }
                }
            } finally {
                // Runs on normal disconnect AND on errors, so dead sessions never pile up.
                // The registry is the single removal point: it drops this session and
                // discards the room if it was the last one, returning the player (if any)
                // that just left so we can announce it.
                val gone = rooms.leave(code, this)
                if (gone != null) {
                    val leave = controlJson.encodeToString<ControlMessage>(Leave(gone.id))
                    room.broadcast(from = this, text = leave)
                }
                println("relay[$code]: left, ${rooms.size} rooms")
            }
        }
    }
}
