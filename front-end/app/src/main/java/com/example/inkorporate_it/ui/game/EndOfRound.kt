package com.example.inkorporate_it.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EndOfRoundScreen(
    state: GameUiState,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    GamePlaceholderScreen(
        title = "End of round",
        note = "Scores for this round. Not built yet.",
        modifier = modifier,
        options = listOf(
            PlaceholderOption("Next round: I'm drawing") { onStubPhase(GamePhase.Drawing, true) },
            PlaceholderOption("Next round: I'm guessing") { onStubPhase(GamePhase.Drawing, false) },
            PlaceholderOption("End the game") { onStubPhase(GamePhase.GameOver, false) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun EndOfRoundScreenPreview() {
    // uncomment when stub has been removed
//    InkorporateitTheme { EndOfRoundScreen (previewGuesserState) }
}