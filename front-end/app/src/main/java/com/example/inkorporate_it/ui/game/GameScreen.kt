package com.example.inkorporate_it.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inkorporate_it.ui.component.LeaveButton
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * The game: one screen that shows a different composable for each part of the game.
 *
 *   Lobby ──Start──▶ Drawing / Guessing ──▶ End of round ──▶ Drawing / Guessing ...
 *                                                       └──▶ End of game ──▶ Lobby
 *
 * In two parts:
 * 1. GameScreen(gameCode, onLeave): what the app's NavHost calls. Creates the
 *    GameViewModel for this game, reads its uiState, and passes it down.
 * 2. GameScreen(state, ...): picks what to show with when(state.phase). Draws only from
 *    the state it's given, so previews can hand it any GameUiState.
 *
 * The game state decides what shows, not navigation: when the server says the round
 * ended, every phone switches to End of round together.
 *
 * STUB: the placeholder screens' buttons call onStubPhase to jump between phases, so the
 * team can click through the whole game now. Remove them once the server sends the phase.
 */

/**
 * - gameCode: null = create a new game, a 6-digit code = join that game
 * - onLeave: go back out of the game (MainActivity pops it off the back stack).
 *   Runs after GameViewModel.leaveGame(), for both the Leave button and the Back gesture.
 */
@Composable
fun GameScreen(
    gameCode: String?,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Creates the ViewModel the first time, with this game's code; after that (rotation,
    // dark mode) it returns the same one. It's tied to this screen's place on the back stack,
    // so it's cleared when the player leaves the game.
    val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(gameCode))

    // collectAsStateWithLifecycle stops listening while the app is in the background.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val leave = {
        viewModel.leaveGame()
        onLeave()
    }
    // Without this, Back would close the game without telling the ViewModel.
    BackHandler(onBack = leave)

    GameScreen(
        state = state,
        onStartGame = viewModel::onStartGame,
        onSendMessage = viewModel::onSendMessage,
        onSendStroke = viewModel::onSendStroke,
        onClearCanvas = viewModel::onClearCanvas,
        onContinueGame = viewModel::onStartNextRound,
        onCloseRoom = viewModel::onCloseRoom,
        onGoToLobby = viewModel::onGoToLobby,
        onLeave = leave,
        onStubPhase = viewModel::stubSetPhase,
        modifier = modifier,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onStartGame: () -> Unit,
    onSendMessage: (String) -> Unit,          // for GuessingScreen, once it's built
    onSendStroke: (Stroke) -> Unit,         // for DrawingScreen, once it's built
    onClearCanvas: () -> Unit,          // for DrawingScreen, once it's built
    onContinueGame: () -> Unit,
    onCloseRoom: () -> Unit,
    onGoToLobby: () -> Unit,
    onLeave: () -> Unit,
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit, // STUB: placeholder buttons
    modifier: Modifier = Modifier,
) {
    Column {
        TopBar(onLeave)

        when (state.phase) {
            GamePhase.Waiting -> LobbyScreen(
                state = state,
                onStart = onStartGame,
                modifier = modifier,
            )

            GamePhase.Drawing -> if (state.isMyTurnToDraw) {
                DrawingScreen(state, onSendStroke, onClearCanvas, modifier, onStubPhase)
            } else {
                GuessingScreen(state, onSendMessage, modifier, onStubPhase)
            }

            GamePhase.RoundOver -> EndOfRoundScreen(state, onContinueGame, modifier, onStubPhase)

            GamePhase.GameOver -> EndOfGameScreen(
                state,
                onCloseRoom,
                onGoToLobby,
                modifier,
                onStubPhase
            )
        }
    }
}

/*
 * Pieces for DrawingScreen and GuessingScreen, from the first version of this file.
 * Not used yet: move them into those screens' files when they're built.
 */

@Composable
private fun TopBar(onLeave: () -> Unit) {
    // Top bar: Leave button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
}

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

@Composable
private fun GameScreenPreview(state: GameUiState) {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            GameScreen(
                state = state,
                onStartGame = {},
                onSendMessage = {},
                onSendStroke = {},
                onClearCanvas = {},
                onContinueGame = {},
                onCloseRoom = {},
                onGoToLobby = {},
                onLeave = {},
                onStubPhase = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, name = "Game, lobby, light")
@Composable
fun GameScreenLobbyPreview() {
    GameScreenPreview(previewGuesserState.copy(phase = GamePhase.Waiting))
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Game, guessing, dark"
)
@Composable
fun GameScreenGuessingPreview() {
    GameScreenPreview(previewGuesserState)
}

@Preview(showBackground = true, name = "Game, end of round")
@Composable
fun GameScreenEndOfRoundPreview() {
    GameScreenPreview(previewGuesserState.copy(phase = GamePhase.RoundOver))
}
