package com.example.inkorporate_it.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * The app's one connection to the game server.
 *
 *   GameViewModel ──▶ GameClient ──▶ GameSocket ──▶ server
 *        ▲                │
 *        └── StateFlows ◀─┘   (the server's messages, turned into state)
 *
 * GameViewModel calls the send functions and combines the flows into GameUiState:
 *
 *     val uiState = combine(client.room, client.chat, client.strokes) { ... }
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState())
 *
 * Sending: one function per thing the player can do. Each one turns the call into a
 * protocol message (shared/.../protocol/Protocol.kt) and sends it through the socket.
 *
 * Receiving: once connected, GameClient reads socket.incoming in `scope`, decodes each
 * message, and updates one of the flows below (room, chat or strokes).
 *
 * The server decides everything (phase, who draws, scores). GameClient never changes
 * the room itself after a send: it waits for the server's reply to update the flows.
 *
 * STUB: shared/.../protocol is still empty, so nothing is encoded or decoded yet and the
 * flows never change. The types at the bottom of this file are placeholders for it.
 */
class GameClient(
    private val socket: GameSocket,
    private val scope: CoroutineScope,
) {

    // --- State from the server (read-only outside this class) ---

    private val _connection = MutableStateFlow(ConnectionState.Disconnected)

    /** Whether the phone is connected. */
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _room = MutableStateFlow<RoomSnapshot?>(null)

    /** The room as the server last described it, or null when not in a room. */
    val room: StateFlow<RoomSnapshot?> = _room.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatLine>>(emptyList())

    /** The chat and guesses for the current round, oldest first. */
    val chat: StateFlow<List<ChatLine>> = _chat.asStateFlow()

    private val _strokes = MutableStateFlow<List<StrokeData>>(emptyList())

    /** Every line on the canvas this round, in the order they were drawn. */
    val strokes: StateFlow<List<StrokeData>> = _strokes.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** One-off errors to show once (wrong code, room full, lost connection). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()


    // --- Room ---

    /** Ask the server for a new room with this player as host. Store the reply as a connection and
     * listen to the flow of updates from the server. Reply: the room, with its code.
     * Connect to the server and start reading its messages.
     * TODO: connection = Connecting; socket.connect(url); connection = Connected;
     *       scope.launch { socket.incoming.collect { handleMessage(it) } };
     *       on failure, connection = Failed and emit to errors.
     */
    suspend fun createRoom(playerName: String): String {
        TODO("Make connection and return room code.")
    }

    /** Ask to join the room with this 6-digit code. Reply: the room, or an error. */
    suspend fun joinRoom(code: String, playerName: String) {
        // TODO: send the join-room message
    }

    /** Tell the server this player left the room. */
    suspend fun leaveRoom() {
        // TODO: send the leave-room message, then clear room, chat and strokes
    }

    /** Host only: close the room for everyone. Leave the room and close the connection.
    TODO: socket.close(); reset every flow. */
    suspend fun closeRoom() {
        // TODO: send the close-room message
    }

    // --- Game flow (host only; the server checks) ---

    /** Start the game from the lobby. */
    suspend fun startGame() {
        // TODO: send the start-game message
    }

    /** Start the next round from the end of round screen. */
    suspend fun startNextRound() {
        // TODO: send the next-round message
    }

    /** After the game ends, take everyone back to the lobby for a new game. */
    suspend fun returnToLobby() {
        // TODO: send the return-to-lobby message
    }

    // --- During a round ---

    /** Send a chat message. Guesses are chat messages: the server checks them against the word. */
    suspend fun sendChat(text: String) {
        // TODO: send the chat message
    }

    /** Drawer only: send one finished line. */
    suspend fun sendStroke(stroke: StrokeData) {
        // TODO: send the stroke message
    }

    /** Drawer only: clear the canvas for everyone. */
    suspend fun clearCanvas() {
        // TODO: send the clear-canvas message
    }

    // --- Receiving ---

    /**
     * Turn one server message into state.
     * TODO: decode `text` into a protocol message, then by its type:
     *   room update  → _room.value = ...
     *   chat message → _chat.update { it + ... }
     *   stroke       → _strokes.update { it + ... }
     *   canvas clear / new round → _strokes.value = emptyList()
     *   error        → _errors.tryEmit(...)
     */
    @Suppress("unused") // called from connect() once it's built
    private fun handleMessage(text: String) {
        // TODO
    }
}

/** Where the connection is. */
enum class ConnectionState { Disconnected, Connecting, Connected, Failed }

/*
 * PLACEHOLDERS until shared/.../protocol/Models.kt has the real models.
 * Plain Kotlin types only (no Compose Color or Offset), so they can move to :shared
 * unchanged. GameViewModel turns them into GameUiState's types.
 */

/** The room, as the server describes it. */
data class RoomSnapshot(
    val code: String,
    val players: List<PlayerSnapshot>,
    val phase: RoomPhase,
    val round: Int = 0,
    val totalRounds: Int = 0,
    val drawerId: String? = null,     // who is drawing this round
    val wordHint: String = "",        // the whole word for the drawer, "_ _ _" for everyone else
    val secondsLeft: Int = 0,
)

enum class RoomPhase { Lobby, Drawing, RoundOver, GameOver }

data class PlayerSnapshot(
    val id: String,
    val name: String,
    val score: Int = 0,
    val isHost: Boolean = false,
    val isMe: Boolean = false,        // true for the player on this phone
    val hasGuessed: Boolean = false,
)

data class ChatLine(
    val senderName: String,
    val text: String,
    val isSystem: Boolean = false,    // "Jerry guessed it!" and other server notes
)

/** One line on the canvas. Points are (x, y) pairs; color is ARGB, like Color.toArgb(). */
data class StrokeData(
    val points: List<Pair<Float, Float>>,
    val colorArgb: Int,
    val width: Float,
)
