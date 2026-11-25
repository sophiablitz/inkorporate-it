package com.example.inkorporate_it.ui.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.component.GuessChatBubble
import com.example.inkorporate_it.ui.component.PlayerEntry
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GuessingScreen(
    state: GameUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    // Scoped strictly to GuessingScreen: applies ADJUST_NOTHING on entry
    // and restores the original softInputMode in onDispose when leaving this screen.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val originalMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            if (originalMode != null) {
                window.setSoftInputMode(originalMode)
            }
        }
    }

    var draftGuess by remember { mutableStateOf("") }
    // STUB: local guess history so emulator testing shows your bubble without a server
    var localStubChat by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    val submitGuess = {
        val trimmed = draftGuess.trim()
        if (trimmed.isNotEmpty()) {
            // Find the active guesser in state.players so the bubble attaches to your inkblob
            val me = state.players.firstOrNull { !it.isDrawing && !it.hasGuessed }
                ?: state.players.firstOrNull()
            if (me != null) {
                localStubChat = localStubChat + ChatMessage(sender = me.name, text = trimmed)
            }
            onSendMessage(trimmed)
            draftGuess = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Main content stays fixed at the top and is not pushed around by the keyboard
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Players list positioned at the top with a theme-adaptive border and no yellow background
            GuessingPlayersCard(players = state.players, chat = state.chat + localStubChat)

            // STUB: placeholder -- remove once the rest is built
            /*GamePlaceholderScreen(
                title = "Guessing",
                note = "Someone else is drawing.",
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 72.dp),
                options = listOf(
                    PlaceholderOption("End the round") { onStubPhase(GamePhase.RoundOver, false) },
                ),
            )*/
        }

        // Guess input bar overlays at the bottom and floats right above the device's keyboard
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding(),
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draftGuess,
                    onValueChange = { draftGuess = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Type your guess...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { submitGuess() }),
                )
                Button(
                    onClick = submitGuess,
                    enabled = draftGuess.isNotBlank(),
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun GuessingPlayersCard(
    players: List<GamePlayer>,
    chat: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    // Latest chat line per sender, so each player shows their most recent guess.
    val latestBySender = chat.associate { it.sender to it.text }
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White else Color.Black
    val contentColor = if (isDark) Color.White else Color.Black

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, borderColor),
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                onBackground = contentColor,
                onSurface = contentColor,
            )
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(players) { player ->
                    Column(
                        modifier = Modifier.width(72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val message = latestBySender[player.name]
                        if (message != null && !player.hasGuessed) {
                            GuessChatBubble(text = message)
                        } else {
                            // Keep avatars aligned when there's no visible bubble
                            // (no guess yet, or a correct guess we don't reveal here).
                            Spacer(modifier = Modifier.height(34.dp))
                        }
                        // Hide the HOST badge once inside the active game screen
                        PlayerEntry(player = player.copy(isHost = false))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Guessing - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Guessing - Dark")
@Composable
fun GuessingScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            GuessingScreen(
                state = previewGuesserState,
                onSendMessage = {},
                onStubPhase = { _, _ -> },
            )
        }
    }
}