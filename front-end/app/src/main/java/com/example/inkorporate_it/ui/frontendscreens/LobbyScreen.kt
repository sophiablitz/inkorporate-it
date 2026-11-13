package com.example.inkorporate_it.ui.frontendscreens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/** A single player in the lobby. */
data class LobbyPlayer(
    val name: String,
    val isHost: Boolean = false,
)

private const val MAX_PLAYERS = 8

@Composable
fun LobbyScreen(
    roomCode: String,
    players: List<LobbyPlayer>,
    isHost: Boolean,
    rounds: Int = 3,
    timeSeconds: Int = 80,
    wordList: String = "Provided",
    onLeave: () -> Unit = {},
    onCopyCode: () -> Unit = {},
    onTimeChange: (Int) -> Unit = {},
    onStart: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
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
            // Top bar: Leave button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeaveButton(onClick = onLeave)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Lobby",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Inkorporate.it",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF451FCF),
                )
            }

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
                        text = roomCode,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(roomCode))
                            Toast.makeText(
                                context,
                                "Room code copied",
                                Toast.LENGTH_SHORT,
                            ).show()
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
                    text = "Players (${players.size}/$MAX_PLAYERS)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PlayerGrid(players = players.take(MAX_PLAYERS))
            }

            // Game settings card
            GameCard {
                Text(
                    text = "Game settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SettingRow(label = "Rounds", value = rounds.toString())
                SettingRow(label = "Time", value = "80s")
                SettingRow(label = "Word list", value = wordList)
            }

            Spacer(Modifier.weight(1f))

            // Bottom action
            if (isHost) {
                val canStart = players.size >= 2
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
                        text = if (canStart) "Start" else "Need 2+ players",
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
fun LeaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Text("Leave")
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
private fun PlayerGrid(players: List<LobbyPlayer>, modifier: Modifier = Modifier) {
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

@Composable
private fun PlayerEntry(player: LobbyPlayer, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InkBlob(color = MaterialTheme.colorScheme.primary)
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (player.isHost) {
            Text(
                text = "HOST",
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun InkBlob(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(color, CircleShape),
    )
}

// Fixed preview render error: Removed the duplicate @Preview annotation with extreme dimensions (widthDp = 607, heightDp = 3000).
// Excessive height causes ComposeViewAdapter instantiation to fail in Android Studio Layoutlib (ClassNotFoundException / BridgeInflater error).
@Preview(showBackground = true)
@Composable
fun LobbyScreenHostPreview() {
    InkorporateitTheme {
        LobbyScreen(
            roomCode = "INK-4821",
            players = listOf(
                LobbyPlayer("Sophia", isHost = true),
                LobbyPlayer("Jerry"),
                LobbyPlayer("Alejandro"),
                LobbyPlayer("Ismael"),
                LobbyPlayer("Sam"),
            ),
            isHost = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LobbyScreenGuestPreview() {
    InkorporateitTheme {
        LobbyScreen(
            roomCode = "INK-4821",
            players = listOf(
                LobbyPlayer("Sophia", isHost = true),
                LobbyPlayer("You"),
            ),
            isHost = false,
        )
    }
}
