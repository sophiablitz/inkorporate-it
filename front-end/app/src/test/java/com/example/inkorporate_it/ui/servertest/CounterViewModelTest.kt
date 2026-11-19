package com.example.inkorporate_it.ui.servertest

import com.example.inkorporate_it.data.network.ConnectionStatus
import com.example.inkorporate_it.data.network.FakeGameSocket
import com.example.inkorporate_it.data.network.GameClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * CounterViewModel with a real GameClient on a FakeGameSocket, so these run
 * without the emulator or a server. The test plays the server through the socket.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    private val socket = FakeGameSocket()
    private val client = GameClient(socket)

    private val increment = """{"type":"counter_increment"}"""
    private val get = """{"type":"counter_get"}"""
    private fun value(n: Int) = """{"type":"counter_value","value":$n}"""

    // viewModelScope runs on Android's main thread. Tests have none, so stand one in.
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun connectsAsSoonAsItIsCreated() {
        CounterViewModel(client)
        assertEquals(1, socket.connectCalls)
    }

    @Test
    fun asksForTheValueOnceConnected() {
        CounterViewModel(client)
        assertEquals(listOf(get), socket.sent)
    }

    @Test
    fun startsWithNoValue() {
        val viewModel = CounterViewModel(client)
        assertNull(viewModel.uiState.value.count)
    }

    @Test
    fun showsTheValueTheServerSends() = runTest {
        val viewModel = CounterViewModel(client)
        socket.serverSends(value(3))
        assertEquals(3, viewModel.uiState.value.count)
        assertEquals(ConnectionStatus.Connected, viewModel.uiState.value.status)
    }

    @Test
    fun tappingPlusOneSendsIncrementButWaitsForTheReply() = runTest {
        val viewModel = CounterViewModel(client)
        socket.serverSends(value(3))

        viewModel.onIncrement()

        assertEquals(increment, socket.sent.last())
        assertEquals(3, viewModel.uiState.value.count) // unchanged until the server answers

        socket.serverSends(value(4))
        assertEquals(4, viewModel.uiState.value.count)
    }

    @Test
    fun reconnectingAsksForTheValueAgain() {
        val viewModel = CounterViewModel(client)
        socket.close()
        viewModel.onReconnect()
        assertEquals(2, socket.connectCalls)
        assertEquals(listOf(get, get), socket.sent)
    }
}
