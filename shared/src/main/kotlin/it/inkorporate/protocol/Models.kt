package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The shared objects that protocol messages carry (players, rooms, lines, error codes).
 * The messages themselves are in Protocol.kt.
 *
 * FIRST DRAFT: written so the app's GameClient has something real to send. Expect it to
 * be revised; change it with the team's sign-off, since the app and the server both
 * depend on these exact field names.
 *
 * Plain Kotlin only (no Compose Color or Offset): the server uses these too.
 */

/**
 * The look of a player's ink blob. The defaults are what a new player gets, the same
 * as the Home screen's defaults. inkColor is a hex string like "#3B4FE0".
 */
@Serializable
data class Avatar(
    val hatId: String = "none",
    val eyesId: String = "round",
    val inkColor: String = "#3B4FE0",
)

/**
 * A player in a room. Exists only after the server answers create_room or join_room:
 * the server picks playerId (two players can share a name).
 * Who hosts is on the Room (hostId), not here.
 */
@Serializable
data class Player(
    val playerId: String,
    val name: String,
    val avatar: Avatar = Avatar(),
    val score: Int = 0,
)

/** Where the room is in the game. Every phone shows the screen for this phase. */
@Serializable
enum class RoomPhase {
    @SerialName("lobby") LOBBY,
    @SerialName("drawing") DRAWING,
    @SerialName("round_over") ROUND_OVER,
    @SerialName("game_over") GAME_OVER,
}

@Serializable
data class RoomSettings(
    val rounds: Int = 3,
    val turnSeconds: Int = 80,
    val maxPlayers: Int = MAX_PLAYERS,
)

/**
 * The whole room as the server sees it. Sent whenever any of it changes.
 * It never contains the secret word.
 */
@Serializable
data class Room(
    val code: String,                           // ROOM_CODE_LENGTH digits
    val phase: RoomPhase,
    val hostId: String,
    val players: List<Player>,                  // in join order, which is also the turn order
    val round: Int = 0,                         // 0 in the lobby, then 1..totalRounds
    val totalRounds: Int = 0,
    val drawerId: String? = null,               // who is drawing this round, null in the lobby
    val guessedIds: List<String> = emptyList(), // players who guessed the word this round
    val settings: RoomSettings = RoomSettings(),
)

/** One point of a line. x and y run from 0 to 1 (fractions of the canvas), so any screen size draws it the same. */
@Serializable
data class Point(val x: Float, val y: Float)

/** One continuous line on the canvas, finger down to finger up. color is hex, like "#000000". */
@Serializable
data class Line(
    val points: List<Point>,
    val color: String,
    val width: Float = 8f,
)

@Serializable
enum class ErrorCode {
    @SerialName("room_not_found") ROOM_NOT_FOUND,
    @SerialName("room_full") ROOM_FULL,
    @SerialName("game_already_started") GAME_ALREADY_STARTED,
    @SerialName("not_host") NOT_HOST,
    @SerialName("not_drawer") NOT_DRAWER,
    @SerialName("not_enough_players") NOT_ENOUGH_PLAYERS, // start_game with fewer than MIN_PLAYERS_TO_START
    @SerialName("invalid_name") INVALID_NAME,             // name is empty after trimming
    @SerialName("bad_message") BAD_MESSAGE,
}

const val MAX_PLAYERS = 8
const val MIN_PLAYERS_TO_START = 2

/** Longest name allowed. The server trims names and cuts them to this length. */
const val MAX_NAME_LENGTH = 16

/** Room codes are exactly this many digits. */
const val ROOM_CODE_LENGTH = 6
