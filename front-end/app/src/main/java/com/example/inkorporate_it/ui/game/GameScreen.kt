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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.ui.component.LeaveButton
import com.example.inkorporate_it.ui.component.LeaveGameDialog
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * The game: one screen that shows a different composable for each part of the game.
 *
 *   Lobby ──Start──▶ Drawing / Guessing ──▶ End of round ──▶ Drawing / Guessing ...
 *                                                       └──▶ End of game ──▶ Lobby
 *
 * In two parts:
 * 1. GameScreen(gameCode, username, avatar, onLeave): what the app's NavHost calls. Creates the
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
 * - username, avatar: this player, as set on Home. Sent to the room when creating or joining.
 * - onLeave: go back out of the game (MainActivity pops it off the back stack).
 *   The Leave button and the Back gesture first show LeaveGameDialog; onLeave runs after
 *   GameViewModel.leaveGame(), once the player confirms.
 */
@Composable
fun GameScreen(
    gameCode: String?,
    username: String,
    avatar: Avatar,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Creates the ViewModel the first time, with this game's code and player; after that
    // (rotation, dark mode) it returns the same one. It's tied to this screen's place on the
    // back stack, so it's cleared when the player leaves the game.
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModel.factory(gameCode, username, avatar),
    )

    // collectAsStateWithLifecycle stops listening while the app is in the background.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Whether the "Leave the game?" dialog is showing. rememberSaveable keeps it open
    // when the phone rotates.
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }

    // The Leave button and the Back gesture both ask first, so a stray tap or swipe
    // doesn't drop the player out of the game.
    val askToLeave = { showLeaveDialog = true }
    // Without this, Back would close the game without asking or telling the ViewModel.
    BackHandler(onBack = askToLeave)

    if (showLeaveDialog) {
        LeaveGameDialog(
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveGame()
                onLeave()
            },
            onDismiss = { showLeaveDialog = false },
        )
    }

    GameScreen(
        state = state,
        onStartGame = viewModel::onStartGame,
        onSendMessage = viewModel::onSendMessage,
        onSendStroke = viewModel::onSendStroke,
        onClearCanvas = viewModel::onClearCanvas,
        onContinueGame = viewModel::onStartNextRound,
        onCloseRoom = viewModel::onCloseRoom,
        onGoToLobby = viewModel::onGoToLobby,
        onLeave = askToLeave,
        onStubPhase = viewModel::stubSetPhase,
        currentPlayerName = username,
        modifier = modifier,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onStartGame: () -> Unit,
    onSendMessage: (String) -> Unit,  // for GuessingScreen, once it's built
    onSendStroke: (Stroke) -> Unit,  // for DrawingScreen, once it's built
    onClearCanvas: () -> Unit,  // for DrawingScreen, once it's built
    onContinueGame: () -> Unit,  // for EndOfRound host to continue came after a round
    onCloseRoom: () -> Unit,  // for EndOfGame host to close a room
    onGoToLobby: () -> Unit,  // for EndOfGame host to return to lobby in same room
    onLeave: () -> Unit,  // for player to leave a game
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit, // STUB: placeholder buttons
    modifier: Modifier = Modifier,
    currentPlayerName: String = "",  // this phone's name, so End of game can highlight its row
) {
    Column {
        // End of game has its own title and uses the whole height, so it skips the top bar.
        if (state.phase != GamePhase.GameOver) {
            TopBar(
                title = when (state.phase) {
                    GamePhase.Waiting -> "Lobby"
                    GamePhase.Drawing -> state.wordHint
                    GamePhase.RoundOver -> "Round Results"
                    GamePhase.GameOver -> "Results"
                },
                onLeave = onLeave,
                // Only Drawing / Guessing has a round in progress and a clock running.
                // The other phases leave these null, so the top bar shows the brand instead.
                round = state.round.takeIf { state.phase == GamePhase.Drawing },
                totalRounds = state.totalRounds.takeIf { state.phase == GamePhase.Drawing },
                secondsLeft = state.secondsLeft.takeIf { state.phase == GamePhase.Drawing },
            )
        }

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
                state = state,
                onCloseRoom = onCloseRoom,
                onGoToLobby = onGoToLobby,
                modifier = modifier,
                currentPlayerName = currentPlayerName,
                onStubPhase = onStubPhase,
            )
        }
    }
}

/*
 * Pieces for DrawingScreen and GuessingScreen, from the first version of this file.
 * Not used yet: move them into those screens' files when they're built.
 */

/**
 * The Leave button, the title, and on the right either the round and timer or the brand.
 * - round, totalRounds, secondsLeft: when all three are given, the right side shows
 *   "Round 2 of 4" above the time left ("0:42"). If any is null, it shows "Inkorporate.it".
 */
@Composable
private fun TopBar(
    title: String,
    onLeave: () -> Unit,
    round: Int? = null,
    totalRounds: Int? = null,
    secondsLeft: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeaveButton(onClick = onLeave)
        Spacer(Modifier.width(12.dp))
        // weight(1f): the title takes the space in the middle and cuts off with "…"
        // instead of pushing the right side off the screen.
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        if (round != null && totalRounds != null && secondsLeft != null) {
            RoundAndTimer(round, totalRounds, secondsLeft)
        } else {
            Text(
                text = "Inkorporate.it",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF451FCF),
            )
        }
    }
}

/** "Round 2 of 4" above the time left, right-aligned. */
@Composable
private fun RoundAndTimer(round: Int, totalRounds: Int, secondsLeft: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatSeconds(secondsLeft),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Round $round of $totalRounds",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 42 -> "0:42", 75 -> "1:15". Never shows a negative time. */
internal fun formatSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
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

@Preview(showBackground = true, name = "Drawing")
@Composable
fun GameScreenDrawingPreview() {
    GameScreenPreview(
        previewGuesserState.copy(
            phase = GamePhase.Drawing,
            isMyTurnToDraw = true,
            wordHint = "TREE"
        )
    )
}

@Preview(showBackground = true, name = "Game, end of round")
@Composable
fun GameScreenEndOfRoundPreview() {
    GameScreenPreview(previewGuesserState.copy(phase = GamePhase.RoundOver))
}
