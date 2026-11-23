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
        note = "Your turn to draw. Not built yet.",
        modifier = modifier,
        options = listOf(
            PlaceholderOption("End the round") { onStubPhase(GamePhase.RoundOver, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun DrawingScreenPreview() {
    // uncomment the line below when stub has been removed
//    InkorporateitTheme { DrawingScreen (previewGuesserState, onSendStroke = {}, onClearCanvas = {} ) }
}