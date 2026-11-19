package it.inkorporate

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * The counter round trip over /ws, the way the app will use it.
 * Each test gets a fresh server (and so a fresh counter starting at 0).
 */
class CounterSocketTest {

    private val increment = """{"type":"counter_increment"}"""
    private val get = """{"type":"counter_get"}"""
    private fun value(n: Int) = """{"type":"counter_value","value":$n}"""

    @Test
    fun `a phone gets the current value as soon as it connects`() = testApplication {
        application { rootModule() }
        wsClient().webSocket("/ws") {
            assertEquals(value(0), receiveText())
        }
    }

    @Test
    fun `each counter_increment is answered with the new value`() = testApplication {
        application { rootModule() }
        wsClient().webSocket("/ws") {
            receiveText() // the value sent on connect
            send(Frame.Text(increment))
            assertEquals(value(1), receiveText())
            send(Frame.Text(increment))
            assertEquals(value(2), receiveText())
        }
    }

    @Test
    fun `counter_get answers with the current value without changing it`() = testApplication {
        application { rootModule() }
        wsClient().webSocket("/ws") {
            receiveText()
            send(Frame.Text(increment))
            assertEquals(value(1), receiveText())
            send(Frame.Text(get))
            assertEquals(value(1), receiveText())
            send(Frame.Text(get))
            assertEquals(value(1), receiveText())
        }
    }

    @Test
    fun `a broken or unknown message is ignored and the connection stays open`() = testApplication {
        application { rootModule() }
        wsClient().webSocket("/ws") {
            receiveText()
            send(Frame.Text("not json at all"))
            send(Frame.Text("""{"type":"something_else"}"""))
            // Still connected: the next real message gets a normal answer.
            send(Frame.Text(increment))
            assertEquals(value(1), receiveText())
        }
    }

    @Test
    fun `all phones share one counter`() = testApplication {
        application { rootModule() }
        val client = wsClient()
        client.webSocket("/ws") {
            receiveText()
            send(Frame.Text(increment))
            assertEquals(value(1), receiveText())
        }
        // A second phone sees the first phone's tap, and keeps counting from there.
        client.webSocket("/ws") {
            assertEquals(value(1), receiveText())
            send(Frame.Text(increment))
            assertEquals(value(2), receiveText())
        }
    }

    private fun ApplicationTestBuilder.wsClient() = createClient { install(WebSockets) }

    /** Waits up to 5 s for the next text frame, so a missing reply fails instead of hanging. */
    private suspend fun DefaultClientWebSocketSession.receiveText(): String =
        withTimeout(5_000) { (incoming.receive() as Frame.Text).readText() }
}
