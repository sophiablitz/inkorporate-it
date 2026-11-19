package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.CounterServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GameClient on a FakeGameSocket: the menu and the JSON are the real code, only the
 * network is pretend. These check the exact JSON that would go over the wire.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameClientTest {

    private val socket = FakeGameSocket()
    private val client = GameClient(socket)

    @Test
    fun connectOpensTheSocket() {
        client.connect()
        assertEquals(1, socket.connectCalls)
    }

    @Test
    fun incrementSendsTheAgreedJson() = runTest {
        client.connect()
        client.increment()
        assertEquals(listOf("""{"type":"counter_increment"}"""), socket.sent)
    }

    @Test
    fun refreshSendsCounterGet() = runTest {
        client.connect()
        client.refresh()
        assertEquals(listOf("""{"type":"counter_get"}"""), socket.sent)
    }

    @Test
    fun nothingIsSentWhileDisconnected() = runTest {
        client.increment()
        assertTrue(socket.sent.isEmpty())
    }

    @Test
    fun aReplyFromTheServerBecomesAnEvent() = runTest {
        val received = mutableListOf<CounterServerMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            client.events.collect { received += it }
        }

        socket.serverSends("""{"type":"counter_value","value":4}""")

        assertEquals(listOf(CounterServerMessage.CounterValue(4)), received)
    }

    @Test
    fun aBrokenMessageFromTheServerIsSkipped() = runTest {
        val received = mutableListOf<CounterServerMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            client.events.collect { received += it }
        }

        socket.serverSends("not json at all")
        socket.serverSends("""{"type":"counter_value","value":5}""")

        assertEquals(listOf(CounterServerMessage.CounterValue(5)), received)
    }
}
