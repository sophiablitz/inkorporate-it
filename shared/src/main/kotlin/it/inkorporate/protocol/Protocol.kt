package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * inkorporate.it message protocol v0
 *
 * Shared by the Android app and the Ktor server. Every message is one JSON
 * object with a "type" field. The "type" value comes from @SerialName.
 *
 * This is the ONLY copy of the protocol. The front-end and back-end will both
 * build from this file, so they can never disagree on field names. (Not wired
 * into either build yet.) Change it only with the team's sign-off.
 *
 * Needs kotlinx.serialization: the "org.jetbrains.kotlin.plugin.serialization"
 * Gradle plugin (same version as Kotlin) and "kotlinx-serialization-json".
 *
 * Rules:
 *  - The server is the boss. Phones send what the player did, the server
 *    decides what is true (scores, timer, whether a guess is right).
 *  - Guessers never receive the secret word until turn_end.
 *  - Stroke coordinates are normalized 0..1, not pixels.
 */

// ---------------------------------------------------------------------------
// JSON setup. Use this same instance on both the phone and the server.
// ---------------------------------------------------------------------------

val ProtocolJson = Json {
    classDiscriminator = "type"   // puts @SerialName into a "type" field
    ignoreUnknownKeys = true      // an older app won't crash on a newer field
    explicitNulls = false         // null fields are left out of the JSON
    encodeDefaults = true
}

fun encodeClient(msg: ClientMessage): String = ProtocolJson.encodeToString(ClientMessage.serializer(), msg)
fun decodeClient(text: String): ClientMessage = ProtocolJson.decodeFromString(ClientMessage.serializer(), text)
fun encodeServer(msg: ServerMessage): String = ProtocolJson.encodeToString(ServerMessage.serializer(), msg)
fun decodeServer(text: String): ServerMessage = ProtocolJson.decodeFromString(ServerMessage.serializer(), text)

// ---------------------------------------------------------------------------
// Shared objects
// ---------------------------------------------------------------------------

@Serializable
data class Avatar(
    val hatId: String,
    val eyesId: String,
    val inkColor: String,          // hex, like "#000000"
)

@Serializable
data class Player(
    val playerId: String,          // unique, because two players can share a name
    val name: String,
    val avatar: Avatar,
    val isHost: Boolean,
    val score: Int = 0,
)

@Serializable
enum class RoomState {
    @SerialName("lobby") LOBBY,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("finished") FINISHED,
    @SerialName("closed") CLOSED,
}

@Serializable
data class RoomSettings(
    val rounds: Int = 3,
    val turnSeconds: Int = 80,
    val maxPlayers: Int = MAX_PLAYERS,
)

@Serializable
data class Room(
    val code: String,              // 6 digits
    val state: RoomState,
    val hostId: String,
    val players: List<Player>,     // in join order, which is also the turn order
    val settings: RoomSettings = RoomSettings(),
)

const val MAX_PLAYERS = 8
const val MIN_PLAYERS_TO_START = 2
const val WORD_OPTION_COUNT = 3

@Serializable
enum class ErrorCode {
    ROOM_NOT_FOUND,
    ROOM_FULL,
    GAME_ALREADY_STARTED,
    NOT_HOST,
    NOT_DRAWER,
    BAD_MESSAGE,
    NOT_ENOUGH_PLAYERS,   // start_game with fewer than MIN_PLAYERS_TO_START
}

// ---------------------------------------------------------------------------
// Phone to server
// ---------------------------------------------------------------------------

@Serializable
sealed class ClientMessage {

    /** Host taps Create game. */
    @Serializable @SerialName("create_room")
    data class CreateRoom(val name: String, val avatar: Avatar) : ClientMessage()

    /** Player enters a code and taps Join. */
    @Serializable @SerialName("join_room")
    data class JoinRoom(val code: String, val name: String, val avatar: Avatar) : ClientMessage()

    /** Player taps Leave. If the host leaves, the room closes. */
    @Serializable @SerialName("leave_room")
    data object LeaveRoom : ClientMessage()

    /** Host taps Start. Server rejects with NOT_HOST or if under 2 players. */
    @Serializable @SerialName("start_game")
    data object StartGame : ClientMessage()

    /** Drawer picks one of the 3 options from word_options. */
    @Serializable @SerialName("choose_word")
    data class ChooseWord(val index: Int) : ClientMessage()

    /**
     * Drawer is drawing, sent in batches (about every 50 ms).
     * One stroke (finger down to finger up) is several batches with the same
     * strokeId. Each point is [x, y, msSinceStrokeStart], x and y in 0..1.
     * The last batch has done = true. size is 1, 2 or 3 (thin, medium, thick).
     * brush is optional: "pressure_pen", "marker", "highlighter" or
     * "dashed_line". Left out (or unknown) means the app's default pen.
     * Eraser for MVP: a stroke in the background color.
     */
    @Serializable @SerialName("stroke")
    data class Stroke(
        val strokeId: Int,
        val points: List<List<Float>>,
        val color: String,
        val size: Int,
        val done: Boolean,
        val brush: String? = null,
    ) : ClientMessage()

    /** Drawer taps undo. Removes the whole last stroke. */
    @Serializable @SerialName("undo")
    data object Undo : ClientMessage()

    /** Drawer taps clear. */
    @Serializable @SerialName("clear")
    data object Clear : ClientMessage()

    /**
     * Any chat message. This is also how guessing works.
     * The drawer can't chat during their turn: the app hides chat for them,
     * and the server ignores any chat the drawer sends anyway (no error).
     */
    @Serializable @SerialName("chat")
    data class Chat(val text: String) : ClientMessage()

    /** Host taps Play again on the end screen. Everyone goes back to the lobby. */
    @Serializable @SerialName("play_again")
    data object PlayAgain : ClientMessage()
}

// ---------------------------------------------------------------------------
// Server to phone
// ---------------------------------------------------------------------------

@Serializable
sealed class ServerMessage {

    /** To the joiner only, after create or join succeeds. playerId is "you". */
    @Serializable @SerialName("room_joined")
    data class RoomJoined(val playerId: String, val room: Room) : ServerMessage()

    /** To everyone, when players, scores or room state change. */
    @Serializable @SerialName("room_state")
    data class RoomStateUpdate(val room: Room) : ServerMessage()

    /** To one phone, when something it asked for failed. */
    @Serializable @SerialName("error")
    data class Error(val code: ErrorCode, val message: String) : ServerMessage()

    /** To everyone except the drawer, while the drawer picks a word. */
    @Serializable @SerialName("drawer_choosing")
    data class DrawerChoosing(val drawerId: String, val round: Int, val totalRounds: Int) : ServerMessage()

    /** To the drawer only. */
    @Serializable @SerialName("word_options")
    data class WordOptions(val words: List<String>, val secondsToChoose: Int) : ServerMessage()

    /**
     * To everyone, once the drawer picks. word is filled in ONLY on the
     * drawer's copy. Guessers get null, which is left out of the JSON.
     */
    @Serializable @SerialName("turn_start")
    data class TurnStart(
        val drawerId: String,
        val wordLength: Int,
        val turnSeconds: Int,
        val word: String? = null,
    ) : ServerMessage()

    /**
     * Relay of the drawer's stroke, to everyone except the drawer.
     * strokeId here is assigned by the server, so it is unique on the canvas
     * even if a phone restarts and starts counting from 1 again.
     */
    @Serializable @SerialName("stroke")
    data class Stroke(
        val strokeId: Int,
        val points: List<List<Float>>,
        val color: String,
        val size: Int,
        val done: Boolean,
        val brush: String? = null,
    ) : ServerMessage()

    /**
     * To one phone only, right after it joins: everything already on the
     * canvas, oldest first, so someone joining mid-turn sees the drawing so far.
     * Each entry has ALL the stroke's points received so far. A stroke still
     * being drawn has done = false, and its next batches arrive as normal
     * stroke messages with the same strokeId.
     * Added after the Nov 5 session (late joiners), not in the original v0 list.
     */
    @Serializable @SerialName("canvas_snapshot")
    data class CanvasSnapshot(val strokes: List<Stroke>) : ServerMessage()

    /** Relay of undo, to everyone except the drawer. */
    @Serializable @SerialName("undo")
    data object Undo : ServerMessage()

    /** Relay of clear, to everyone except the drawer. */
    @Serializable @SerialName("clear")
    data object Clear : ServerMessage()

    /**
     * To everyone: a wrong guess or normal message.
     * A correct guess is NEVER sent as chat, so the word can't leak.
     */
    @Serializable @SerialName("chat")
    data class Chat(val playerId: String, val text: String) : ServerMessage()

    /** To everyone. Does not include the word. */
    @Serializable @SerialName("correct_guess")
    data class CorrectGuess(val playerId: String) : ServerMessage()

    /** To everyone, when the timer runs out or everyone has guessed. */
    @Serializable @SerialName("turn_end")
    data class TurnEnd(
        val word: String,
        val turnScores: Map<String, Int>,   // playerId to points earned this turn
        val room: Room,
    ) : ServerMessage()

    /** To everyone, after the last turn of the last round. */
    @Serializable @SerialName("game_end")
    data class GameEnd(
        val standings: List<String>,        // playerIds, highest score first
        val room: Room,
    ) : ServerMessage()

    /** To everyone, when the host leaves. */
    @Serializable @SerialName("room_closed")
    data class RoomClosed(val reason: String) : ServerMessage()
}

// ---------------------------------------------------------------------------
// Helpers both sides will want
// ---------------------------------------------------------------------------

/** Server: turn a drawer's stroke into the relay message. */
fun ClientMessage.Stroke.toRelay() = ServerMessage.Stroke(strokeId, points, color, size, done, brush)

/** Phone: convert a server stroke back into the shape the canvas code uses. */
fun ServerMessage.Stroke.toClientShape() = ClientMessage.Stroke(strokeId, points, color, size, done, brush)
