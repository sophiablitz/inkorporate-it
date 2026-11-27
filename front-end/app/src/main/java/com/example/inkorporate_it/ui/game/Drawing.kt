package com.example.inkorporate_it.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

@Composable
fun DrawingScreen(
    state: GameUiState,
    onSendStroke: (Stroke) -> Unit,
    onClearCanvas: () -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    GamePlaceholderScreen(
        title = "Drawing",
        // STUB: what the server sent, so the flow can be checked before the screen is built.
        note = "Your word: ${state.wordHint} · ${state.secondsLeft} s left · ${state.strokes.size} strokes. Not built yet.",
        modifier = modifier,
        options = listOf(
            // STUB: against StubServerSocket this brings the turn's timer to its last second.
            PlaceholderOption("Skip to the end of the turn") { onStubPhase(GamePhase.RoundOver, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun DrawingScreenPreview() {
    // uncomment the line below when stub has been removed
//    InkorporateitTheme { DrawingScreen (previewGuesserState, onSendStroke = {}, onClearCanvas = {} ) }
}