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
        // STUB: what the server sent, so the flow can be checked before the screen is built.
        note = "${state.drawerName} is drawing: ${state.wordHint} · ${state.secondsLeft} s left · " +
            "${state.strokes.size} strokes so far. Not built yet.",
        modifier = modifier,
        options = listOf(
            // STUB: against StubServerSocket this brings the turn's timer to its last second.
            PlaceholderOption("Skip to the end of the turn") { onStubPhase(GamePhase.RoundOver, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun GuessingScreenPreview() {
    // uncomment when stub has been removed
//    InkorporateitTheme { GuessingScreen (previewGuesserState, onSendMessage = {}) }
}