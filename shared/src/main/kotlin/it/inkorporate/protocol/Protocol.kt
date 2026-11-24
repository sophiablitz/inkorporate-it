package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Every message the phones and the server send each other.
 *
 * Each message is one JSON object with a "type" field, whose value comes from
 * @SerialName. For example ClientMessage.Chat("barn") is {"type":"chat","text":"barn"}.
 * The objects the messages carry (Room, Player, Line...) are in Models.kt.
 *
 * FIRST DRAFT: expect it to be revised. Change it with the team's sign-off.
 *
 * Rules:
 *  - The server decides what is true (phase, who draws, scores, the timer, whether a
 *    guess is right). Phones only say what the player did.
 *  - Guessers never receive the secret word until round_end.
 *  - Line points are 0..1 fractions of the canvas, not pixels.
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

    /** Join the room with this code. Answer: room_joined, or error (room_not_found, room_full...). */
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

    /** Host only, on the end of round screen: start the next round. */
    @Serializable @SerialName("next_round")
    data object NextRound : ClientMessage()

    /** Host only, on the end of game screen: take everyone back to the lobby. */
    @Serializable @SerialName("return_to_lobby")
    data object ReturnToLobby : ClientMessage()

    /** A chat message. This is also how guessing works: the server checks it against the word. */
    @Serializable @SerialName("chat")
    data class Chat(val text: String) : ClientMessage()

    /** Drawer only: one finished line. */
    @Serializable @SerialName("draw_line")
    data class DrawLine(val line: Line) : ClientMessage()

    /** Drawer only: clear the canvas for everyone. */
    @Serializable @SerialName("clear_canvas")
    data object ClearCanvas : ClientMessage()
}

// ---------------------------------------------------------------------------
// Server to phone
// ---------------------------------------------------------------------------

@Serializable
sealed class ServerMessage {

    /** To the joiner only, after create_room or join_room works. playerId is "you". */
    @Serializable @SerialName("room_joined")
    data class RoomJoined(val playerId: String, val room: Room) : ServerMessage()

    /** To everyone, whenever players, scores or the phase change. */
    @Serializable @SerialName("room_update")
    data class RoomUpdate(val room: Room) : ServerMessage()

    /**
     * To everyone, when a round starts. word is filled in ONLY on the drawer's copy;
     * guessers get null (left out of the JSON) and use wordLength for the blanks.
     */
    @Serializable @SerialName("round_start")
    data class RoundStart(
        val room: Room,
        val wordLength: Int,
        val turnSeconds: Int,
        val word: String? = null,
    ) : ServerMessage()

    /** To everyone, about once a second while drawing. */
    @Serializable @SerialName("time_left")
    data class TimeLeft(val seconds: Int) : ServerMessage()

    /** The drawer's line, to everyone except the drawer. */
    @Serializable @SerialName("draw_line")
    data class DrawLine(val line: Line) : ServerMessage()

    /** The drawer cleared the canvas. To everyone except the drawer. */
    @Serializable @SerialName("clear_canvas")
    data object ClearCanvas : ServerMessage()

    /** To everyone: a chat message or wrong guess. A correct guess is NEVER sent as chat. */
    @Serializable @SerialName("chat")
    data class Chat(val playerId: String, val text: String) : ServerMessage()

    /** To everyone: this player guessed the word. Does not include the word. */
    @Serializable @SerialName("correct_guess")
    data class CorrectGuess(val playerId: String) : ServerMessage()

    /** To everyone, when the timer runs out or everyone has guessed. Reveals the word. */
    @Serializable @SerialName("round_end")
    data class RoundEnd(
        val word: String,
        val roundScores: Map<String, Int>, // playerId to points earned this round
        val room: Room,
    ) : ServerMessage()

    /** To everyone, after the last round. room.players has the final scores. */
    @Serializable @SerialName("game_end")
    data class GameEnd(val room: Room) : ServerMessage()

    /** To everyone, when the host closes the room or leaves. */
    @Serializable @SerialName("room_closed")
    data class RoomClosed(val reason: String) : ServerMessage()

    /** To one phone, when something it asked for failed. Not named Error, so it doesn't hide Kotlin's Error. */
    @Serializable @SerialName("error")
    data class ErrorMessage(val code: ErrorCode, val message: String) : ServerMessage()
}
