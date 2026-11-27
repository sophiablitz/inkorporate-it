package com.example.inkorporate_it.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EndOfRoundScreen(
    state: GameUiState,
    onContinueGame: () -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    GamePlaceholderScreen(
        title = "End of round",
        // STUB: what the server sent, so the flow can be checked before the screen is built.
        note = "The word was ${state.revealedWord}.\n" +
            state.players.joinToString("\n") { "${it.name}: +${it.pointsThisTurn} (total ${it.score})" } +
            "\nNext turn in ${state.secondsLeft} s",
        modifier = modifier,
        options = listOf(
            // The server picks who draws next (join order), so the only real choice here is the
            // host's Continue: next_turn, instead of waiting for the results to time out.
            PlaceholderOption("Continue") { onContinueGame() },
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun EndOfRoundScreenPreview() {
    // uncomment when stub has been removed
//    InkorporateitTheme { EndOfRoundScreen (previewGuesserState, onContinueGame = {} ) }
}