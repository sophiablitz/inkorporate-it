package com.blitz

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

fun Application.configureRouting() {
    // Everyone currently connected to /relay. Lives outside routing { } so every
    // connection shares the same set. Thread-safe because clients join/leave concurrently.
    val sessions = ConcurrentHashMap.newKeySet<DefaultWebSocketServerSession>()

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

        // Step 3: broadcast. Every text message is forwarded to all OTHER connected clients.
        // The server never looks inside the text, so it will relay JSON strokes unchanged.
        webSocket("/relay") {
            sessions += this
            println("relay: joined, ${sessions.size} connected")
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
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