package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * inkorporate.it protocol v0: the shared objects that messages carry
 * (players, rooms, settings, error codes). The messages themselves are in
 * Protocol.kt. Change it only with the team's sign-off.
 */

// ---------------------------------------------------------------------------
// Shared objects
// ---------------------------------------------------------------------------

/**
 * The look of a player's ink blob. The defaults are what a new player gets,
 * the same on the phone and the server.
 * inkColor is a hex string on the wire (the server can't use Compose Color).
 * The app parses it for drawing.
 */
@Serializable
data class Avatar(
    val hatId: String = "none",
    val eyesId: String = "round",
    val inkColor: String = "#3B4FE0",   // hex, like "#000000"
)

/**
 * A player in a room. Only exists after room_joined: the server assigns
 * playerId and score. Before that (on Home), the phone just has a name and
 * an avatar, which is what create_room and join_room send.
 * Who hosts is on the Room (hostId), not here: check playerId == room.hostId.
 */
@Serializable
data class Player(
    val playerId: String,          // unique, because two players can share a name
    val name: String,
    val avatar: Avatar,
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
    @SerialName("room_not_found") ROOM_NOT_FOUND,
    @SerialName("room_full") ROOM_FULL,
    @SerialName("game_already_started") GAME_ALREADY_STARTED,
    @SerialName("not_host") NOT_HOST,
    @SerialName("not_drawer") NOT_DRAWER,
    @SerialName("bad_message") BAD_MESSAGE,
    @SerialName("not_enough_players") NOT_ENOUGH_PLAYERS,   // start_game with fewer than MIN_PLAYERS_TO_START
}
