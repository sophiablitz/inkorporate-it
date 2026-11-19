package com.example.inkorporate_it.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GuessingScreen(
    state: GameUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    GamePlaceholderScreen(
        title = "Guessing",
        note = "Someone else is drawing. Not built yet.",
        modifier = modifier,
        options = listOf(
            PlaceholderOption("End the round") { onStubPhase(GamePhase.RoundOver, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun GuessingScreenPreview() {
    // uncomment when stub has been removed
//    InkorporateitTheme { GuessingScreen (previewGuesserState, onSendMessage = {}) }
}