package it.inkorporate

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlin.test.Test
import kotlin.test.assertTrue

class WebSocketTest {

    @Test
    fun `ws accepts a connection and closes cleanly`() = testApplication {
        application {
            rootModule()
        }
        val client = createClient {
            install(WebSockets)
        }

        var opened = false
        // Throws if the server refuses the connection, which fails the test.
        client.webSocket("/ws") {
            opened = true
            close(CloseReason(CloseReason.Codes.NORMAL, "test done"))
        }
        assertTrue(opened)
    }
}
