package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.Line
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.decodeServer
import it.inkorporate.protocol.encodeClient
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
import kotlinx.serialization.SerializationException

/*
 * Turns the game into messages and messages into the game.
 *
 *   GameViewModel ──calls──▶ GameClient ──JSON text──▶ GameSocket ──▶ server
 *        ▲                       │
 *        └──── state ◀───────────┘  (each server message, folded into one GameSession)
 *
 * Sending: one function per thing the player can do. Each one builds a ClientMessage
 * (shared/.../protocol/Protocol.kt), encodes it as JSON and sends it through the socket.
 *
 * Receiving: from the moment it's created, GameClient reads socket.incoming in `scope`,
 * decodes each message and folds it into `state`. GameViewModel turns that into GameUiState.
 *
 * The server decides everything (phase, who draws, scores). After a send, GameClient
 * waits for the server's answer to change `state`, with one exception: the drawer's
 * own lines and clears, which the server doesn't echo back to the drawer.
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
        scope.launch {
            socket.incoming.collect { text -> handleMessage(text) }
        }
    }

    // --- Room ---

    /** Ask for a new room with this player as host. Answer: room_joined (with the code), or an error. */
    suspend fun createRoom(name: String, avatar: Avatar) =
        send(ClientMessage.CreateRoom(name, avatar))

    /** Ask to join the room with this 6-digit code. Answer: room_joined, or an error. */
    suspend fun joinRoom(code: String, name: String, avatar: Avatar) =
        send(ClientMessage.JoinRoom(code, name, avatar))

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

    // --- Game flow (host only; the server checks) ---

    /** Start the game from the lobby. Answer: round_start, or not_enough_players. */
    suspend fun startGame() = send(ClientMessage.StartGame)

    /** Start the next round from the end of round screen. Answer: round_start. */
    suspend fun startNextRound() = send(ClientMessage.NextRound)

    /** After the game ends, take everyone back to the lobby. Answer: room_update. */
    suspend fun returnToLobby() = send(ClientMessage.ReturnToLobby)

    // --- During a round ---

    /** Send a chat message. Guesses are chat messages: the server checks them against the word. */
    suspend fun sendChat(text: String) = send(ClientMessage.Chat(text))

    /** Drawer only: send one finished line, and show it on this phone right away. */
    suspend fun sendLine(line: Line) {
        _state.update { it.copy(lines = it.lines + line) }
        send(ClientMessage.DrawLine(line))
    }

    /** Drawer only: clear the canvas for everyone, this phone included. */
    suspend fun clearCanvas() {
        _state.update { it.copy(lines = emptyList()) }
        send(ClientMessage.ClearCanvas)
    }

    /** Forget the last error, once the screen has shown it. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // --- Sending ---

    /**
     * Encode [message] and send it. Opens the connection first if it isn't open, and
     * waits up to CONNECT_TIMEOUT_MS for it. Returns false (and sets state.error) if
     * the message couldn't be sent.
     */
    private suspend fun send(message: ClientMessage): Boolean {
        socket.connect()
        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            socket.status.first { it == ConnectionStatus.Connected }
        }
        val sent = connected != null && socket.send(encodeClient(message))
        if (!sent) _state.update { it.copy(error = "Couldn't reach the server. Check your connection.") }
        return sent
    }

    // --- Receiving ---

    /** Decode one server message and fold it into [state]. */
    private fun handleMessage(text: String) {
        val message = try {
            decodeServer(text)
        } catch (e: SerializationException) {
            return // not JSON, or a type this version of the app doesn't know: skip it
        }
        _state.update { session -> session.apply(message) }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}

/**
 * One game, as this phone knows it. Built only from what the server said.
 * Every field has a default, so GameSession() is "not in a room".
 */
data class GameSession(
    val myPlayerId: String? = null,         // which player in room.players is this phone
    val room: Room? = null,                 // null until room_joined, and after leaving
    val word: String? = null,               // the secret word: drawer only, until round_end
    val wordLength: Int = 0,                // for the guessers' blanks
    val revealedWord: String? = null,       // the word, shown to everyone at round_end
    val secondsLeft: Int = 0,
    val lines: List<Line> = emptyList(),    // this round's canvas, oldest first
    val chat: List<ChatEntry> = emptyList(),// this round's chat, oldest first
    val roundScores: Map<String, Int> = emptyMap(), // playerId to points from the last round
    val closedReason: String? = null,       // set when the room closes
    val error: String? = null,              // the last thing that went wrong, to show once
) {
    /** The new session after [message]. Pure: no sending, so it's easy to test. */
    fun apply(message: ServerMessage): GameSession = when (message) {
        is ServerMessage.RoomJoined -> GameSession(myPlayerId = message.playerId, room = message.room)

        is ServerMessage.RoomUpdate ->
            if (message.room.phase == RoomPhase.LOBBY) {
                // Back in the lobby (or still there): no round, so nothing on the canvas.
                copy(room = message.room, word = null, wordLength = 0, revealedWord = null,
                    lines = emptyList(), chat = emptyList(), roundScores = emptyMap())
            } else {
                copy(room = message.room)
            }

        is ServerMessage.RoundStart -> copy(
            room = message.room,
            word = message.word,
            wordLength = message.wordLength,
            revealedWord = null,
            secondsLeft = message.turnSeconds,
            lines = emptyList(),
            chat = emptyList(),
            roundScores = emptyMap(),
        )

        is ServerMessage.TimeLeft -> copy(secondsLeft = message.seconds)
        is ServerMessage.DrawLine -> copy(lines = lines + message.line)
        ServerMessage.ClearCanvas -> copy(lines = emptyList())
        is ServerMessage.Chat -> copy(chat = chat + ChatEntry(message.playerId, message.text))

        is ServerMessage.CorrectGuess ->
            copy(chat = chat + ChatEntry(playerId = null, text = "${nameOf(message.playerId)} guessed it!"))

        is ServerMessage.RoundEnd -> copy(
            room = message.room,
            word = null,
            revealedWord = message.word,
            secondsLeft = 0,
            roundScores = message.roundScores,
        )

        is ServerMessage.GameEnd -> copy(room = message.room, word = null, secondsLeft = 0)
        is ServerMessage.RoomClosed -> GameSession(closedReason = message.reason)
        is ServerMessage.ErrorMessage -> copy(error = message.message)
    }

    /** A player's name, from their id. */
    fun nameOf(playerId: String?): String =
        room?.players?.firstOrNull { it.playerId == playerId }?.name ?: "Someone"
}

/** One line in the chat. playerId is null for the server's own notes ("Jerry guessed it!"). */
data class ChatEntry(
    val playerId: String?,
    val text: String,
)
