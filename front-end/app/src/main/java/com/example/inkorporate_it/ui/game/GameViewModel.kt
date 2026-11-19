package com.example.inkorporate_it.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * Holds the game's state and handles what the player does in it.
 * GameScreen reads uiState and calls the on...() functions. It never changes state itself.
 *
 * Created with a join code (see factory below):
 * - gameCode = null   → createGame(): this phone starts a new room and is the host
 * - gameCode = "012345" → joinGame("012345"): this phone joins that room
 *
 * Codes are 6 digits kept as text, so a leading 0 is never lost.
 *
 * STUB: nothing talks to the server yet. Later, this class also takes GameStateRepository,
 * ChatRepository and DrawingRepository, and builds uiState by combining their flows:
 *
 *     val uiState = combine(gameState.state, chat.messages, drawing.strokes) { ... }
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState())
 */
class GameViewModel(gameCode: String?) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())

    /** What GameScreen draws. Read-only from outside. */
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Runs once, when the ViewModel is created (not on rotation: the same ViewModel is kept).
    init {
        if (gameCode == null) createGame() else joinGame(gameCode)
    }

    /** Start a new room as its host. TODO: ask the server to create a room. */
    private fun createGame() {
        // STUB: a fake room with two players, so the lobby's Start button is enabled (it needs 2+).
        _uiState.value = GameUiState(
            roomCode = "STUB",
            isHost = true,
            players = listOf(
                GamePlayer(name = "You", isHost = true),
                GamePlayer(name = "Stub player"),
            ),
        )
    }

    /** Join someone else's room. TODO: send the code to the server; show an error if it's wrong. */
    private fun joinGame(code: String) {
        // STUB: pretend the code worked. Guests have no Start button: they wait for the host.
        _uiState.value = GameUiState(
            roomCode = code,
            isHost = false,
            players = listOf(
                GamePlayer(name = "Stub host", isHost = true),
                GamePlayer(name = "You"),
            ),
        )
    }

    /** The player tapped Leave or used Back. TODO: tell the server this player left. */
    fun leaveGame() {
        // TODO
    }

    /** The host tapped Start in the lobby. TODO: ask the server to start the game. */
    fun onStartGame() {
        // STUB: start round 1 with this phone drawing. The server will decide who draws.
        _uiState.update { it.copy(round = 1, totalRounds = 3) }
        stubSetPhase(GamePhase.Drawing, isMyTurnToDraw = true)
    }

    /** The host tapped Start in the end of round screen. TODO: ask the server to start the next round. */
    fun onStartNextRound() {
        // TODO
    }

    /** The host's call to return to the lobby to start a new game. TODO: send server a message to go to lobby */
    fun onGoToLobby() {
        // TODO
    }
    
    /** The host's call to close the room. TODO: send server message to close room */
    fun onCloseRoom() {
        // TODO
    }


    /** The player sent a message. TODO: send it through ChatRepository. */
    fun onSendMessage(text: String) {
        // TODO
    }

    /** The drawer finished a line. TODO: send it through DrawingRepository. */
    fun onSendStroke(stroke: Stroke) {
        // TODO
    }

    /** The drawer tapped Clear. TODO: clear the canvas for everyone through DrawingRepository. */
    fun onClearCanvas() {
        // TODO
    }

    /**
     * STUB ONLY: jump to a phase, so the placeholder screens' buttons can walk through
     * the game. Delete this once the server sends the real phase changes.
     */
    fun stubSetPhase(phase: GamePhase, isMyTurnToDraw: Boolean = false) {
        _uiState.update { it.copy(phase = phase, isMyTurnToDraw = isMyTurnToDraw) }
    }

    companion object {
        /**
         * Builds a GameViewModel for a join code (null = create a new game).
         * Needed because the ViewModel takes a constructor argument, and plain viewModel()
         * only knows how to build ViewModels with no arguments.
         */
        fun factory(gameCode: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(gameCode) }
        }
    }
}
