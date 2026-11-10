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
