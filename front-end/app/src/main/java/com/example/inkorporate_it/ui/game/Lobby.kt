package com.example.inkorporate_it.ui.game

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.component.PlayerEntry
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import kotlinx.coroutines.launch

private const val MAX_PLAYERS = 8
private const val MIN_PLAYERS_TO_START = 2

@Composable
fun LobbyScreen(
    state: GameUiState,
    modifier: Modifier = Modifier,
    timeSeconds: Int = 80,
    wordList: String = "Provided",
    onCopyCode: () -> Unit = {},
    onStart: () -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Room code card
            GameCard {
                Text(
                    text = "Room code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.roomCode,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val clipData = ClipData.newPlainText("Room Code", state.roomCode)
                                val clipEntry = ClipEntry(clipData)
                                clipboard.setClipEntry(clipEntry)
                            }
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12L and older
                                Toast.makeText(context, "Room code copied", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            onCopyCode()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // Players card
            GameCard {
                Text(
                    text = "Players (${state.players.size}/$MAX_PLAYERS)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PlayerGrid(players = state.players.take(MAX_PLAYERS))
            }

            // Game settings card
            GameCard {
                Text(
                    text = "Game settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SettingRow(label = "Rounds", value = state.round.toString())
                SettingRow(label = "Time", value = "${timeSeconds}s")
                SettingRow(label = "Word list", value = wordList)
            }

            Spacer(Modifier.weight(1f))

            // Bottom action
            if (state.isHost) {
                val canStart = state.players.size >= MIN_PLAYERS_TO_START
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text = if (canStart) "Start" else "Need $MIN_PLAYERS_TO_START+ players",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                Text(
                    text = "Waiting for host…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val MIN_TIME_SECONDS = 10
private const val MAX_TIME_SECONDS = 300

/**
 * Time setting. For the host this shows a numeric input field so the round
 * timer can be entered; for everyone else it's read-only text.
 */
@Composable
private fun TimeSettingRow(
    timeSeconds: Int,
    editable: Boolean,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!editable) {
        SettingRow(label = "Time", value = "${timeSeconds}s", modifier = modifier)
        return
    }

    // Local text state so the user can freely clear/retype digits.
    var text by remember { mutableStateOf(timeSeconds.toString()) }

    // Keep the field in sync if the value changes from elsewhere.
    LaunchedEffect(timeSeconds) {
        if (text.toIntOrNull() != timeSeconds) {
            text = timeSeconds.toString()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Time",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                // Accept digits only, max 3 chars (up to 300s).
                val digits = input.filter { it.isDigit() }.take(3)
                text = digits
                digits.toIntOrNull()?.let { entered ->
                    val clamped = entered.coerceIn(MIN_TIME_SECONDS, MAX_TIME_SECONDS)
                    onTimeChange(clamped)
                }
            },
            modifier = Modifier.width(120.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = {
                Text(
                    text = "s",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

/** Lays players out in rows of up to 4. */
@Composable
private fun PlayerGrid(players: List<GamePlayer>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        players.chunked(4).forEach { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowPlayers.forEach { player ->
                    PlayerEntry(
                        player = player,
                        modifier = Modifier.weight(1f),
                    )
                }
                // pad the last row so cells stay aligned
                repeat(4 - rowPlayers.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// Fixed preview render error: Removed the duplicate @Preview annotation with extreme dimensions (widthDp = 607, heightDp = 3000).
// Excessive height causes ComposeViewAdapter instantiation to fail in Android Studio Layoutlib (ClassNotFoundException / BridgeInflater error).
@Preview(showBackground = true)
@Composable
fun LobbyScreenHostPreview() {
    InkorporateitTheme { LobbyScreen(previewGuesserState) }
}

@Preview(showBackground = true)
@Composable
fun LobbyScreenGuestPreview() {
    InkorporateitTheme { LobbyScreen(previewGuesserState) }
}
