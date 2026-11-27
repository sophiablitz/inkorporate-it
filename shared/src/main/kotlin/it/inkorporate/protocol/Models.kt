package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The shared objects that protocol messages carry (players, rooms, strokes, error codes).
 * The messages themselves are in Protocol.kt.
 *
 * Built from protocol v0 (#11) and Sophia's draft on front-end/game-client. Change it
 * with the leads' sign-off, since the app and the server both depend on these exact
 * field names.
 *
 * Plain Kotlin only (no Compose Color or Offset): the server uses these too.
 *
 * Words used everywhere in the protocol:
 *  - turn: one player draws one word.
 *  - round: every player has drawn once. RoomSettings.rounds counts these.
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
    /** Waiting for the host to start. */
    @SerialName("lobby") LOBBY,

    /** The drawer is looking at their word (#160). The timer hasn't started. */
    @SerialName("getting_ready") GETTING_READY,

    /** The drawer is drawing and everyone else is guessing. The timer is running. */
    @SerialName("drawing") DRAWING,

    /** A turn just ended: the word is revealed, with that turn's points. */
    @SerialName("turn_over") TURN_OVER,

    /** The last turn of the last round is over: final scores. */
    @SerialName("game_over") GAME_OVER,
}

@Serializable
data class RoomSettings(
    val rounds: Int = 3,          // each round, every player draws once
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
    val round: Int = 0,                         // 0 in the lobby, then 1..settings.rounds
    val totalRounds: Int = 0,                   // settings.rounds once the game starts
    val drawerId: String? = null,               // who draws this turn, null in the lobby
    val guessedIds: List<String> = emptyList(), // players who guessed the word this turn
    val settings: RoomSettings = RoomSettings(),
)

/** One point of a stroke. x and y run from 0 to 1 (fractions of the canvas), so any screen size draws it the same. */
@Serializable
data class Point(val x: Float, val y: Float)

/**
 * Part of one stroke (finger down to finger up). color is hex, like "#000000".
 *
 * A stroke can arrive in several parts with the same strokeId, the last one with
 * done = true, so other phones can watch it being drawn (protocol v0's live strokes).
 * For now the app sends each stroke as one part, done = true, when the finger lifts.
 * Adding live parts later doesn't change the protocol.
 *
 * strokeId is picked by the drawer's phone, counting up from 1 each turn. Only the
 * drawer draws, and the canvas is emptied at the start of each turn, so the ids can't
 * clash.
 */
@Serializable
data class StrokePart(
    val strokeId: Int,
    val points: List<Point>,
    val color: String,
    val width: Float = 8f,
    val done: Boolean = true,
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
    @SerialName("not_in_room") NOT_IN_ROOM,               // a game message before create_room or join_room
    @SerialName("wrong_phase") WRONG_PHASE,               // e.g. start_drawing while nobody is getting ready
    @SerialName("bad_message") BAD_MESSAGE,
}

const val MAX_PLAYERS = 8
const val MIN_PLAYERS_TO_START = 2

/** Longest name allowed. The server trims names and cuts them to this length. */
const val MAX_NAME_LENGTH = 16

/** Room codes are exactly this many digits. */
const val ROOM_CODE_LENGTH = 6

/** How long the drawer can look at their word before the turn starts on its own (#160). */
const val SECONDS_TO_START = 10

/** How long the turn's results show before the next turn starts on its own. The host can skip ahead. */
const val TURN_RESULTS_SECONDS = 8

/** Longest chat message or guess. The server cuts longer ones. */
const val MAX_CHAT_LENGTH = 100

/** Most points in one stroke part. The server drops bigger parts (from Sophia's review of #13). */
const val MAX_POINTS_PER_PART = 200

/** Most points on the canvas in one turn. The server ignores strokes past this, so the canvas can't grow forever. */
const val MAX_POINTS_PER_TURN = 20_000
