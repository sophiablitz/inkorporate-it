package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.CounterClientMessage
import it.inkorporate.protocol.CounterServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GameClient on a FakeGameSocket: the menu is the real code, only the network is
 * pretend. The JSON for these messages is tested in shared/ (CounterExampleTest).
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
    fun incrementSendsCounterIncrement() = runTest {
        client.connect()
        client.increment()
        assertEquals(listOf(CounterClientMessage.CounterIncrement), socket.sent)
    }

    @Test
    fun refreshSendsCounterGet() = runTest {
        client.connect()
        client.refresh()
        assertEquals(listOf(CounterClientMessage.CounterGet), socket.sent)
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

        socket.serverSends(CounterServerMessage.CounterValue(4))

        assertEquals(listOf(CounterServerMessage.CounterValue(4)), received)
    }
}
