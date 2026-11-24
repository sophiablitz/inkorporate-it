package com.example.inkorporate_it.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.inkorporate_it.InkorporateApp
import com.example.inkorporate_it.data.network.GameClient
import com.example.inkorporate_it.data.network.GameSession
import com.example.inkorporate_it.data.network.GameSocket
import com.example.inkorporate_it.model.Avatar
import it.inkorporate.protocol.Line
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.RoomPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import it.inkorporate.protocol.Avatar as WireAvatar

/*
 * Holds the game's state and handles what the player does in it.
 * GameScreen reads uiState and calls the on...() functions. It never changes state itself.
 *
 *   GameScreen ──on...()──▶ GameViewModel ──▶ GameClient ──▶ GameSocket ──▶ server
 *        ▲                       │
 *        └──── uiState ◀─────────┘  (GameClient.state, turned into GameUiState)
 *
 * Created with a join code and this player's name and avatar (see factory below):
 * - gameCode = null     → create_room: this phone starts a new room and is the host
 * - gameCode = "012345" → join_room: this phone joins that room
 * Either way, the name and avatar are what the other players see for this phone.
 *
 * Codes are 6 digits kept as text, so a leading 0 is never lost.
 *
 * STUB: the server doesn't answer these messages yet. Until it does, the screen starts
 * with a fake room and the placeholder buttons call stubSetPhase(), so the team can
 * click through the whole game. As soon as the server answers, its state replaces the
 * fake one.
 */
class GameViewModel(
    gameCode: String?,
    private val username: String,
    private val avatar: Avatar,
    socket: GameSocket,
) : ViewModel() {

    /** Listens for as long as this ViewModel lives; the socket itself stays open after. */
    private val client = GameClient(socket, viewModelScope)

    private val _uiState = MutableStateFlow(
        if (gameCode == null) stubCreatedRoom() else stubJoinedRoom(gameCode)
    )

    /** What GameScreen draws. Read-only from outside. */
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Runs once, when the ViewModel is created (not on rotation: the same ViewModel is kept).
    init {
        viewModelScope.launch {
            client.state.collect { session -> onServerUpdate(session) }
        }
        viewModelScope.launch {
            if (gameCode == null) {
                client.createRoom(username, avatar.toWire())
            } else {
                client.joinRoom(gameCode, username, avatar.toWire())
            }
        }
    }

    /** The player tapped Leave or used Back. GameScreen pops the screen right after. */
    fun leaveGame() {
        viewModelScope.launch { client.leaveRoom() }
    }

    /** The host tapped Start in the lobby. */
    fun onStartGame() {
        viewModelScope.launch { client.startGame() }
        // STUB: start round 1 with this phone drawing, until the server sends round_start.
        _uiState.update { it.copy(round = 1, totalRounds = 3) }
        stubSetPhase(GamePhase.Drawing, isMyTurnToDraw = true)
    }

    /** The host tapped Start on the end of round screen. */
    fun onStartNextRound() {
        viewModelScope.launch { client.startNextRound() }
    }

    /** The host's call to return to the lobby to start a new game. */
    fun onGoToLobby() {
        viewModelScope.launch { client.returnToLobby() }
    }

    /** The host's call to close the room. */
    fun onCloseRoom() {
        viewModelScope.launch { client.closeRoom() }
    }

    /** The player sent a message. Guesses are chat messages too. */
    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { client.sendChat(trimmed) }
    }

    /**
     * The drawer finished a line. Its points must be 0..1 fractions of the canvas
     * (Offset(x / width, y / height)), so every screen size draws it the same.
     */
    fun onSendStroke(stroke: Stroke) {
        viewModelScope.launch { client.sendLine(stroke.toWire()) }
    }

    /** The drawer tapped Clear. */
    fun onClearCanvas() {
        viewModelScope.launch { client.clearCanvas() }
    }

    /** The screen showed state.errorMessage; don't show it again. */
    fun onErrorShown() {
        client.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * STUB ONLY: jump to a phase, so the placeholder screens' buttons can walk through
     * the game. Delete this once the server sends the real phase changes.
     */
    fun stubSetPhase(phase: GamePhase, isMyTurnToDraw: Boolean = false) {
        _uiState.update { it.copy(phase = phase, isMyTurnToDraw = isMyTurnToDraw) }
    }

    /**
     * The server said something. Once it has sent a room, the screen shows only the
     * server's state. Before that (STUB), keep the fake room and just show any error.
     */
    private fun onServerUpdate(session: GameSession) {
        if (session.room != null) {
            _uiState.value = session.toUiState()
        } else {
            val error = session.error ?: session.closedReason?.let { "The room closed: $it" }
            _uiState.update { it.copy(errorMessage = error) }
        }
    }

    // STUB: a fake room with two players, so the lobby's Start button is enabled (it needs 2+).
    private fun stubCreatedRoom() = GameUiState(
        roomCode = "STUB",
        isHost = true,
        players = listOf(
            GamePlayer(name = username, avatar = avatar, isHost = true),
            GamePlayer(name = "Stub player"),
        ),
    )

    // STUB: pretend the code worked. Guests have no Start button: they wait for the host.
    private fun stubJoinedRoom(code: String) = GameUiState(
        roomCode = code,
        isHost = false,
        players = listOf(
            GamePlayer(name = "Stub host", isHost = true),
            GamePlayer(name = username, avatar = avatar),
        ),
    )

    companion object {
        /**
         * Builds a GameViewModel for a join code (null = create a new game) and this player.
         * Needed because the ViewModel takes constructor arguments, and plain viewModel()
         * only knows how to build ViewModels with no arguments.
         * The socket is the app's one connection, from InkorporateApp.
         */
        fun factory(
            gameCode: String?,
            username: String,
            avatar: Avatar,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as InkorporateApp
                GameViewModel(gameCode, username, avatar, app.socket)
            }
        }
    }
}

// --- Server objects → screen objects (and back) ---

/** What the screen shows for this game session. Only call it once session.room isn't null. */
internal fun GameSession.toUiState(): GameUiState {
    val room = requireNotNull(room)
    val wordShown = word ?: List(wordLength) { "_" }.joinToString(" ")
    return GameUiState(
        phase = when (room.phase) {
            RoomPhase.LOBBY -> GamePhase.Waiting
            RoomPhase.DRAWING -> GamePhase.Drawing
            RoomPhase.ROUND_OVER -> GamePhase.RoundOver
            RoomPhase.GAME_OVER -> GamePhase.GameOver
        },
        roomCode = room.code,
        isHost = room.hostId == myPlayerId,
        players = room.players.map { player ->
            GamePlayer(
                name = player.name,
                avatar = Avatar(
                    hatId = player.avatar.hatId,
                    eyesId = player.avatar.eyesId,
                    inkColor = hexToColor(player.avatar.inkColor),
                ),
                score = player.score,
                isHost = player.playerId == room.hostId,
                isDrawing = player.playerId == room.drawerId,
                hasGuessed = player.playerId in room.guessedIds,
            )
        },
        isMyTurnToDraw = myPlayerId != null && room.drawerId == myPlayerId,
        wordHint = wordShown,
        secondsLeft = secondsLeft,
        round = room.round,
        totalRounds = room.totalRounds,
        strokes = lines.map { line ->
            Stroke(
                points = line.points.map { Offset(it.x, it.y) },
                color = hexToColor(line.color),
                width = line.width,
            )
        },
        chat = chat.map { ChatMessage(sender = if (it.playerId == null) "" else nameOf(it.playerId), text = it.text) },
        errorMessage = error,
    )
}

internal fun Avatar.toWire() = WireAvatar(hatId = hatId, eyesId = eyesId, inkColor = colorToHex(inkColor))

internal fun Stroke.toWire() = Line(
    points = points.map { Point(it.x, it.y) },
    color = colorToHex(color),
    width = width,
)

/** Color → "#RRGGBB". */
internal fun colorToHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

/** "#RRGGBB" → Color. Anything else is black. */
internal fun hexToColor(hex: String): Color {
    val rgb = hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16) ?: 0L
    return Color(0xFF000000 or rgb)
}
