package com.example.inkorporate_it.ui.servertest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.inkorporate_it.InkorporateApp
import com.example.inkorporate_it.data.network.ConnectionStatus
import it.inkorporate.protocol.CounterServerMessage
import com.example.inkorporate_it.data.network.GameClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the counter test screen shows. */
data class CounterUiState(
    /** The server's value, or null until the server has sent one. */
    val count: Int? = null,
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
)

/**
 * The counter test screen's logic (#144). It talks to the server only through
 * [GameClient]'s menu. A test builds that GameClient on a FakeGameSocket, so it
 * needs no server.
 *
 * The number only changes when the server's reply arrives: tapping +1 sends
 * counter_increment, and the screen updates when counter_value comes back.
 */
class CounterViewModel(private val client: GameClient) : ViewModel() {

    private val count = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<CounterUiState> =
        combine(count, client.status) { count, status -> CounterUiState(count, status) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CounterUiState())

    init {
        // Listen first, so no message from the server is missed.
        viewModelScope.launch {
            client.events.collect { event ->
                when (event) {
                    is CounterServerMessage.CounterValue -> count.value = event.value
                }
            }
        }
        // Each time the connection opens, ask for the current value. The server also sends
        // it on connect, but if the connection was already open before this screen, it won't.
        viewModelScope.launch {
            client.status.collect { status ->
                if (status == ConnectionStatus.Connected) client.refresh()
            }
        }
        client.connect()
    }

    fun onIncrement() {
        viewModelScope.launch { client.increment() }
    }

    fun onRefresh() {
        viewModelScope.launch { client.refresh() }
    }

    fun onReconnect() = client.connect()

    companion object {
        /** Builds the ViewModel with the app's shared [GameClient]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as InkorporateApp
                CounterViewModel(app.gameClient)
            }
        }
    }
}
