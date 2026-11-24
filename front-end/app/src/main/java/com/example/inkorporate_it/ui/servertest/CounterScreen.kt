package com.example.inkorporate_it.ui.servertest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inkorporate_it.data.network.ConnectionStatus
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/**
 * The server test screen (#139): connects the [CounterViewModel] to [CounterScreen].
 * MainActivity shows this one.
 */
@Composable
fun CounterRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CounterViewModel = viewModel(factory = CounterViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CounterScreen(
        state = state,
        onIncrement = viewModel::onIncrement,
        onRefresh = viewModel::onRefresh,
        onReconnect = viewModel::onReconnect,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Just the layout: takes the state and reports taps, so it previews without a server. */
@Composable
fun CounterScreen(
    state: CounterUiState,
    onIncrement: () -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = state.status == ConnectionStatus.Connected

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when (state.status) {
                ConnectionStatus.Connected -> "Connected to the server"
                ConnectionStatus.Connecting -> "Connecting..."
                ConnectionStatus.Disconnected -> "Not connected"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        // While not connected, the number is the last value we saw and may be out of date.
        // Dim it to 38%, Material's standard "disabled" level, and say so underneath.
        val stale = !connected && state.count != null
        Text(
            // A dash until the server has sent a value.
            text = state.count?.toString() ?: "–",
            style = MaterialTheme.typography.displayLarge,
            color = LocalContentColor.current.copy(alpha = if (stale) 0.38f else 1f),
        )
        if (stale) {
            Text(
                text = "Last value we saw",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onIncrement, enabled = connected) { Text("+1") }
            OutlinedButton(onClick = onRefresh, enabled = connected) { Text("Refresh") }
        }
        if (state.status == ConnectionStatus.Disconnected) {
            TextButton(onClick = onReconnect) { Text("Reconnect") }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenConnectedPreview() {
    InkorporateitTheme(dynamicColor = false) {
        CounterScreen(
            state = CounterUiState(count = 3, status = ConnectionStatus.Connected),
            onIncrement = {}, onRefresh = {}, onReconnect = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenDisconnectedPreview() {
    InkorporateitTheme(dynamicColor = false) {
        CounterScreen(
            // A value from before the disconnect: shows the greyed-out "last value" look.
            state = CounterUiState(count = 3, status = ConnectionStatus.Disconnected),
            onIncrement = {}, onRefresh = {}, onReconnect = {}, onBack = {},
        )
    }
}
