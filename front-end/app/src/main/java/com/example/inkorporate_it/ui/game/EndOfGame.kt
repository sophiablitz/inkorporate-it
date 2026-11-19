package com.example.inkorporate_it.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EndOfGameScreen(
    state: GameUiState,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    GamePlaceholderScreen(
        title = "End of game",
        note = "Final scores. Not built yet.",
        modifier = modifier,
        options = listOf(
            PlaceholderOption("Back to the lobby") { onStubPhase(GamePhase.Waiting, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun EndOfGameScreenPreview() {
    // uncomment when stub has been removed
//    InkorporateitTheme { EndOfGameScreen (previewGuesserState) }
}