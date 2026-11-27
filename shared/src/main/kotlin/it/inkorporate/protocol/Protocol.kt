package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Every game message the phones and the server send each other.
 *
 * Each message is one JSON object with a "type" field, whose value comes from
 * @SerialName. For example ClientMessage.Chat("barn") is {"type":"chat","text":"barn"}.
 * The objects the messages carry (Room, Player, StrokePart...) are in Models.kt.
 *
 * Built from protocol v0 (#11) and Sophia's draft on front-end/game-client.
 * Change it with the leads' sign-off.
 *
 * Rules:
 *  - The server decides what is true (phase, who draws, scores, the timer, whether a
 *    guess is right). Phones only say what the player did.
 *  - Guessers never receive the secret word until turn_end.
 *  - Stroke points are 0..1 fractions of the canvas, not pixels.
 *  - A turn is one player drawing one word. A round is every player drawing once.
 *
 * A turn, message by message:
 *  1. room_update (phase getting_ready, drawerId set) to everyone,
 *     and your_word to the drawer only.
 *  2. The drawer sends start_drawing, or SECONDS_TO_START pass.
 *  3. turn_start to everyone. time_left about once a second.
 *  4. The drawer sends stroke / undo / clear_canvas, relayed to everyone else.
 *  5. Guessers send chat: wrong guesses come back as chat, right ones as correct_guess.
 *  6. Time runs out, everyone guessed, or the drawer left: turn_end to everyone.
 *  7. After TURN_RESULTS_SECONDS, or when the host sends next_turn: the next turn,
 *     or game_end after the last turn of the last round.
 */

/**
 * The one set of JSON rules for every message. The app and the server both use it,
 * so the same message always turns into the same text on both sides.
 */
val ProtocolJson = Json {
    classDiscriminator = "type"   // puts @SerialName into a "type" field
    ignoreUnknownKeys = true      // an older app won't crash on a newer field
    explicitNulls = false         // null fields are left out of the JSON
    encodeDefaults = true         // fields with default values are still sent
}

fun encodeClient(message: ClientMessage): String =
    ProtocolJson.encodeToString(ClientMessage.serializer(), message)

fun decodeClient(text: String): ClientMessage =
    ProtocolJson.decodeFromString(ClientMessage.serializer(), text)

fun encodeServer(message: ServerMessage): String =
    ProtocolJson.encodeToString(ServerMessage.serializer(), message)

fun decodeServer(text: String): ServerMessage =
    ProtocolJson.decodeFromString(ServerMessage.serializer(), text)

// ---------------------------------------------------------------------------
// Phone to server
// ---------------------------------------------------------------------------

@Serializable
sealed class ClientMessage {

    /** Create a new room with this player as host. Answer: room_joined, or error. */
    @Serializable @SerialName("create_room")
    data class CreateRoom(val name: String, val avatar: Avatar) : ClientMessage()

    /** Join the room with this code, in the lobby only. Answer: room_joined, or error (room_not_found, room_full...). */
    @Serializable @SerialName("join_room")
    data class JoinRoom(val code: String, val name: String, val avatar: Avatar) : ClientMessage()

    /** This player leaves. If the host leaves, the room closes for everyone. */
    @Serializable @SerialName("leave_room")
    data object LeaveRoom : ClientMessage()

    /** Host only: close the room for everyone. */
    @Serializable @SerialName("close_room")
    data object CloseRoom : ClientMessage()

    /** Host only, in the lobby: start the game. Needs MIN_PLAYERS_TO_START players. */
    @Serializable @SerialName("start_game")
    data object StartGame : ClientMessage()

    /** Drawer only, while getting ready: "I've seen my word, start the timer" (#160). */
    @Serializable @SerialName("start_drawing")
    data object StartDrawing : ClientMessage()

    /** Drawer only, while drawing: part of a stroke. See StrokePart. */
    @Serializable @SerialName("stroke")
    data class Stroke(val part: StrokePart) : ClientMessage()

    /** Drawer only: remove the newest stroke. */
    @Serializable @SerialName("undo")
    data object Undo : ClientMessage()

    /** Drawer only: clear the canvas for everyone. */
    @Serializable @SerialName("clear_canvas")
    data object ClearCanvas : ClientMessage()

    /**
     * A chat message. This is also how guessing works: the server checks it against the word.
     * The drawer's chat is ignored while they draw, so they can't give the word away.
     */
    @Serializable @SerialName("chat")
    data class Chat(val text: String) : ClientMessage()

    /** Host only, on the turn's results: go on now instead of waiting TURN_RESULTS_SECONDS. */
    @Serializable @SerialName("next_turn")
    data object NextTurn : ClientMessage()

    /** Host only, after the game ends: everyone back to the lobby, scores back to 0. */
    @Serializable @SerialName("return_to_lobby")
    data object ReturnToLobby : ClientMessage()
}

// ---------------------------------------------------------------------------
// Server to phone
// ---------------------------------------------------------------------------

@Serializable
sealed class ServerMessage {

    /** To the joiner only, after create_room or join_room works. playerId is "you". */
    @Serializable @SerialName("room_joined")
    data class RoomJoined(val playerId: String, val room: Room) : ServerMessage()

    /** To everyone, whenever players, scores, the phase or who has guessed change. */
    @Serializable @SerialName("room_update")
    data class RoomUpdate(val room: Room) : ServerMessage()

    /**
     * To the drawer only, when their turn begins (#160). Everyone else gets a room_update
     * with phase getting_ready. The timer starts when the drawer sends start_drawing,
     * or after secondsToStart, whichever comes first.
     */
    @Serializable @SerialName("your_word")
    data class YourWord(val word: String, val secondsToStart: Int) : ServerMessage()

    /**
     * To everyone, when the drawing starts. Guessers use wordLength for the blanks.
     * There's no word in it: the drawer already got it in your_word.
     */
    @Serializable @SerialName("turn_start")
    data class TurnStart(val room: Room, val wordLength: Int, val turnSeconds: Int) : ServerMessage()

    /** To everyone, about once a second while drawing. */
    @Serializable @SerialName("time_left")
    data class TimeLeft(val seconds: Int) : ServerMessage()

    /** The drawer's stroke part, to everyone except the drawer. */
    @Serializable @SerialName("stroke")
    data class Stroke(val part: StrokePart) : ServerMessage()

    /** The drawer removed their newest stroke. To everyone except the drawer. */
    @Serializable @SerialName("undo")
    data object Undo : ServerMessage()

    /** The drawer cleared the canvas. To everyone except the drawer. */
    @Serializable @SerialName("clear_canvas")
    data object ClearCanvas : ServerMessage()

    /** To everyone: a chat message or wrong guess. A correct guess is NEVER sent as chat. */
    @Serializable @SerialName("chat")
    data class Chat(val playerId: String, val text: String) : ServerMessage()

    /** To everyone: this player guessed the word. Does not include the word. */
    @Serializable @SerialName("correct_guess")
    data class CorrectGuess(val playerId: String) : ServerMessage()

    /** To everyone, when the timer runs out, everyone has guessed, or the drawer left. Reveals the word. */
    @Serializable @SerialName("turn_end")
    data class TurnEnd(
        val word: String,
        val turnScores: Map<String, Int>, // playerId to points earned this turn
        val room: Room,
    ) : ServerMessage()

    /** To everyone, after the last turn of the last round. room.players has the final scores. */
    @Serializable @SerialName("game_end")
    data class GameEnd(val room: Room) : ServerMessage()

    /** To everyone, when the host closes the room or leaves. */
    @Serializable @SerialName("room_closed")
    data class RoomClosed(val reason: String) : ServerMessage()

    /** To one phone, when something it asked for failed. Not named Error, so it doesn't hide Kotlin's Error. */
    @Serializable @SerialName("error")
    data class ErrorMessage(val code: ErrorCode, val message: String) : ServerMessage()
}
