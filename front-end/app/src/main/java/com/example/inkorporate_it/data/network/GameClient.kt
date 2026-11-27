package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.StrokePart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/*
 * The game's menu (#187): one function per thing the player can do, and the game state
 * built from everything the server says back.
 *
 *   GameViewModel ──calls──▶ GameClient ──ClientMessage──▶ GameSocket ──▶ server
 *        ▲                       │
 *        └──── state ◀───────────┘  (each ServerMessage, folded into one GameSession)
 *
 * It knows nothing about JSON: the socket turns messages into JSON and back (Sophia's
 * call: serialization stays inside the socket). Adapted from Sophia's GameClient on
 * front-end/game-client.
 *
 * The server decides everything (phase, who draws, scores). After a send, GameClient
 * waits for the server's answer to change `state`, with one exception: the drawer's own
 * strokes, undos and clears, which the server doesn't echo back to the drawer.
 *
 * - socket: the app's one connection (InkorporateApp.socket). It outlives this client.
 * - scope: how long to keep listening. GameViewModel passes viewModelScope, so listening
 *   stops when the player leaves the game screen, while the connection stays open.
 */
class GameClient(
    private val socket: GameSocket,
    scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(GameSession())

    /** Everything the server has told this phone about the current game. */
    val state: StateFlow<GameSession> = _state.asStateFlow()

    /** Whether the phone is connected right now. */
    val connection: StateFlow<ConnectionStatus> = socket.status

    init {
        // Listen before anything is sent, so no answer is missed.
        scope.launch {
            socket.incoming.collect { message -> _state.update { it.apply(message) } }
        }
    }

    // --- Room ---

    /** Ask for a new room with this player as host. Answer: room_joined (with the code), or an error. */
    suspend fun createRoom(name: String, avatar: Avatar) = send(ClientMessage.CreateRoom(name, avatar))

    /** Ask to join the room with this 6-digit code. Answer: room_joined, or an error. */
    suspend fun joinRoom(code: String, name: String, avatar: Avatar) = send(ClientMessage.JoinRoom(code, name, avatar))

    /**
     * Tell the server this player left, then forget the game.
     * NonCancellable: the player is leaving the screen, which cancels the caller's
     * scope, and the message should still go out.
     */
    suspend fun leaveRoom() {
        withContext(NonCancellable) { send(ClientMessage.LeaveRoom) }
        _state.value = GameSession()
    }

    /** Host only: close the room for everyone. Answer: room_closed. */
    suspend fun closeRoom() = send(ClientMessage.CloseRoom)

    // --- Game flow (the server checks who's allowed) ---

    /** Host only, in the lobby. Answer: a room_update with the first drawer getting ready. */
    suspend fun startGame() = send(ClientMessage.StartGame)

    /** Drawer only, after seeing their word (#160). Answer: turn_start. */
    suspend fun startDrawing() = send(ClientMessage.StartDrawing)

    /** Host only, on a turn's results: go on without waiting. */
    suspend fun nextTurn() = send(ClientMessage.NextTurn)

    /** Host only, after the game ends: everyone back to the lobby. */
    suspend fun returnToLobby() = send(ClientMessage.ReturnToLobby)

    // --- During a turn ---

    /** A chat message. Guesses are chat messages: the server checks them against the word. */
    suspend fun sendChat(text: String) = send(ClientMessage.Chat(text))

    /**
     * Drawer only: one finished stroke, with points as 0..1 fractions of the canvas.
     * It shows on this phone right away, then goes to the server as one part.
     * (The protocol also allows sending a stroke in parts while the finger moves.)
     */
    suspend fun sendStroke(points: List<Point>, color: String, width: Float) {
        val id = (_state.value.strokes.maxOfOrNull { it.strokeId } ?: 0) + 1
        val part = StrokePart(id, points, color, width, done = true)
        _state.update { it.withStroke(part) }
        send(ClientMessage.Stroke(part))
    }

    /** Drawer only: remove the newest stroke, on this phone and everyone else's. */
    suspend fun undo() {
        _state.update { it.copy(strokes = it.strokes.dropLast(1)) }
        send(ClientMessage.Undo)
    }

    /** Drawer only: clear the canvas for everyone, this phone included. */
    suspend fun clearCanvas() {
        _state.update { it.copy(strokes = emptyList()) }
        send(ClientMessage.ClearCanvas)
    }

    /** Forget the last error, once the screen has shown it. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // --- Sending ---

    /**
     * Send [message]. Opens the connection first if it isn't open, and waits up to
     * CONNECT_TIMEOUT_MS for it. Returns false (and sets state.error) if it couldn't be sent.
     */
    private suspend fun send(message: ClientMessage): Boolean {
        socket.connect()
        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            socket.status.first { it == ConnectionStatus.Connected }
        }
        val sent = connected != null && socket.send(message)
        if (!sent) _state.update { it.copy(error = "Couldn't reach the server. Check your connection.") }
        return sent
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}
