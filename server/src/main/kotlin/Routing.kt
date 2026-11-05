package com.blitz

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

fun Application.configureRouting() {
    // Everyone currently connected to /relay. Lives outside routing { } so every
    // connection shares the same set. Thread-safe because clients join/leave concurrently.
    val sessions = ConcurrentHashMap.newKeySet<DefaultWebSocketServerSession>()

    // Shared canvas state so late joiners can be caught up. The server treats every
    // message as opaque JSON except for the small `type` field it needs to maintain
    // this state. See CanvasState for the live-streaming protocol.
    val canvas = CanvasState()

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
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
        //   1. updates `canvas` so it always knows the current drawing, then
        //   2. forwards the raw frame to every OTHER client (unchanged bytes), and
        //   3. on connect, sends the new client a {"type":"snapshot","strokes":[..]}
        //      so a late joiner immediately sees everything drawn so far, including
        //      strokes still in progress.
        webSocket("/relay") {
            sessions += this
            println("relay: joined, ${sessions.size} connected")

            // Catch the new client up with the current canvas before anything else.
            send(Frame.Text(canvas.snapshotJson()))

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        canvas.apply(text)
                        for (other in sessions) {
                            if (other != this) other.send(Frame.Text(text))
                        }
                    }
                }
            } finally {
                // Runs on normal disconnect AND on errors, so dead sessions never pile up.
                sessions -= this
                println("relay: left, ${sessions.size} connected")
            }
        }
    }
}
