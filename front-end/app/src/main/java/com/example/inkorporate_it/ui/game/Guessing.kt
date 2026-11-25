package com.example.inkorporate_it.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.component.GuessChatBubble
import com.example.inkorporate_it.ui.component.PlayerEntry
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

@Composable
fun GuessingScreen(
    state: GameUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top header (blue rectangle following theme kit)
        GuessingTopHeader()
        // Players list (yellow card, one row, scrolls sideways if needed)
        GuessingPlayersCard(players = state.players, chat = state.chat)

        // STUB: placeholder -- remove once the rest is built
        GamePlaceholderScreen(
            title = "Guessing",
            note = "Someone else is drawing.",
            modifier = Modifier.weight(1f),
            options = listOf(
                PlaceholderOption("End the round") { onStubPhase(GamePhase.RoundOver, false) },
            ),
        )
    }
}
@Composable
private fun GuessingTopHeader(
    modifier: Modifier = Modifier,
) {
    GameCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        )
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

    GameCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    PlayerEntry(player = player)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuessingScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        GuessingScreen(
            state = previewGuesserState,
            onSendMessage = {},
            onStubPhase = { _, _ -> },
        )
    }
}