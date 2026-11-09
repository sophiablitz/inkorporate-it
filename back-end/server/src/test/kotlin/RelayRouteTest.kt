package com.blitz

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-process integration tests for the `/relay` route, run via [testApplication] so there's
 * no port and no external server — the stale-server class of failure can't happen here.
 * These cover the behavior nothing else verified yet: room isolation, the presence flow
 * (hello -> roster/join/leave), and undo.
 *
 * Helper conventions: every socket first receives the canvas snapshot on connect, so tests
 * read and discard that first frame before asserting on what follows.
 */
class RelayRouteTest {

    /** Read the next text frame, failing the test if none arrives promptly. */
    private suspend fun io.ktor.client.plugins.websocket.DefaultClientWebSocketSession.nextText(): String =
        withTimeout(3000) {
            (incoming.receive() as Frame.Text).readText()
        }

    @Test
    fun `snapshot is sent on connect`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=r1") {
            val first = nextText()
            assertTrue(first.contains("\"type\":\"snapshot\""), "first frame is a snapshot")
            assertTrue(first.contains("\"strokes\":[]"), "fresh room has an empty canvas")
        }
    }

    @Test
    fun `a stroke reaches another client in the same room`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=same") {
            nextText() // my snapshot
            val me = this
            // Second client in the same room.
            client.webSocket("/relay?room=same") {
                nextText() // their snapshot
                me.send(Frame.Text("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}"""))
                val got = nextText()
                assertTrue(got.contains("\"type\":\"begin\"") && got.contains("s1"))
            }
        }
    }

    @Test
    fun `rooms are isolated`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=alpha") {
            nextText() // snapshot
            val alpha = this
            client.webSocket("/relay?room=beta") {
                nextText() // snapshot
                alpha.send(Frame.Text("""{"type":"begin","id":"x1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}"""))
                // beta must receive nothing more; a short wait with no frame is the pass.
                val stray = runCatching { withTimeout(500) { (incoming.receive() as Frame.Text).readText() } }.getOrNull()
                assertNull(stray, "a different room must not receive the stroke")
            }
        }
    }

    @Test
    fun `hello yields a roster to self and a join to others`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=p") {
            nextText() // snapshot for the first client
            val first = this
            client.webSocket("/relay?room=p") {
                nextText() // snapshot for the second client
                // Second client says hello.
                send(Frame.Text("""{"type":"hello","name":"Bob","color":123}"""))
                val roster = nextText()
                assertTrue(roster.contains("\"type\":\"roster\""), "self gets a roster")
                assertTrue(roster.contains("Bob"), "roster includes the joiner")
                // The first client should see a join announcing Bob.
                val join = first.run { nextText() }
                assertTrue(join.contains("\"type\":\"join\"") && join.contains("Bob"))
            }
        }
    }

    @Test
    fun `leave is announced when a client disconnects`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=q") {
            nextText() // snapshot
            val observer = this
            // A second client joins, says hello, then disconnects.
            client.webSocket("/relay?room=q") {
                nextText()
                send(Frame.Text("""{"type":"hello","name":"Amy","color":1}"""))
                nextText() // roster (self)
            }
            // observer sees: join (Amy), then leave (Amy) after the block above closes.
            val join = observer.run { nextText() }
            assertTrue(join.contains("\"type\":\"join\"") && join.contains("Amy"))
            val leave = observer.run { nextText() }
            assertTrue(leave.contains("\"type\":\"leave\""), "departure is announced")
        }
    }

    @Test
    fun `undo removes the sender's stroke and acks the room`() = testApplication {
        installServer()
        val client = createClient { install(WebSockets) }
        client.webSocket("/relay?room=u") {
            nextText() // snapshot
            send(Frame.Text("""{"type":"hello","name":"Drawer","color":1}"""))
            nextText() // roster
            // Draw a stroke (owned by this connection), then undo it.
            send(Frame.Text("""{"type":"begin","id":"s9","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}"""))
            send(Frame.Text("""{"type":"points","id":"s9","points":[{"x":1.0,"y":1.0,"t":0}]}"""))
            send(Frame.Text("""{"type":"undo"}"""))
            val ack = nextText()
            assertTrue(ack.contains("\"type\":\"undoAck\""), "undo is acked")
            assertTrue(ack.contains("s9"), "ack names the removed stroke")
        }
    }
}
