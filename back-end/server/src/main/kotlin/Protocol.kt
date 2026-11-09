package com.blitz

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The CONTROL plane of the relay protocol.
 *
 * The relay carries two kinds of traffic:
 *   - DATA plane: stroke frames (begin/points/end/clear). The server treats these as
 *     opaque bytes and forwards them verbatim; [CanvasState] only peeks at a couple of
 *     fields. These are NOT modeled here and intentionally stay string-based.
 *   - CONTROL plane: who's in the room and what's happening (hello/roster/join/leave).
 *     These carry nested data (a list of players, each with a name and color), which the
 *     hand-rolled extractors in [CanvasState] can't parse. So the control plane — and only
 *     the control plane — uses kotlinx.serialization.
 *
 * All messages share the same `"type"` discriminator the data plane already uses, so both
 * planes travel over the same socket and a client dispatches on one field. The control
 * `type` values (hello/roster/join/leave) are disjoint from the data ones
 * (begin/points/end/clear/snapshot), so there's no ambiguity.
 */
@Serializable
sealed interface ControlMessage {
    val type: String
}

/**
 * A public view of one connected player. `color` is an ARGB int (same convention as the
 * stroke frames). Never carries anything secret — this is broadcast to the whole room.
 */
@Serializable
data class PlayerInfo(
    val id: String,
    val name: String,
    val color: Int,
)

/**
 * Client -> server, sent once right after connecting: the player's chosen display name
 * and color. The server assigns the authoritative id; anything the client sends for its
 * own id is ignored. `name`/`color` are untrusted labels.
 */
@Serializable
@SerialName("hello")
data class Hello(
    val name: String = "anon",
    val color: Int = 0,
) : ControlMessage {
    override val type: String get() = "hello"
}

/**
 * Server -> a newly joined client: everyone already in the room (including the recipient),
 * the presence analog of the canvas snapshot. Lets a joiner render the player list at once.
 */
@Serializable
@SerialName("roster")
data class Roster(
    val players: List<PlayerInfo>,
) : ControlMessage {
    override val type: String get() = "roster"
}

/** Server -> other clients: a new player arrived. */
@Serializable
@SerialName("join")
data class Join(
    val player: PlayerInfo,
) : ControlMessage {
    override val type: String get() = "join"
}

/** Server -> other clients: a player left (by id). */
@Serializable
@SerialName("leave")
data class Leave(
    val id: String,
) : ControlMessage {
    override val type: String get() = "leave"
}

/**
 * Client -> server: "undo my last stroke." Deliberately carries no target — the server
 * undoes the most recent stroke OWNED by the requesting connection, so one client can't
 * undo another's work. The server replies to the whole room with an [Undo] naming the
 * stroke that was actually removed.
 */
@Serializable
@SerialName("undo")
class UndoRequest : ControlMessage {
    override val type: String get() = "undo"
}

/**
 * Server -> all clients: remove the stroke with [strokeId] from the canvas. Sent after an
 * undo (and reusable later for a drawer clearing a mistake). Clients drop that stroke by id.
 * The wire `type` is `"undoAck"`, distinct from the inbound `"undo"` request, so the two
 * are never confused.
 */
@Serializable
@SerialName("undoAck")
data class Undo(
    val strokeId: String,
) : ControlMessage {
    override val type: String get() = "undoAck"
}

/**
 * JSON codec for control messages. `classDiscriminator = "type"` makes the polymorphic
 * tag field literally `"type"`, matching the data-plane convention. `ignoreUnknownKeys`
 * keeps us tolerant of extra fields a client might send.
 */
val controlJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Cheap, allocation-light check of whether a raw frame is a CONTROL message a client sends
 * inbound (`hello` or `undo`), so the relay can route it without fully parsing every stroke
 * frame. Everything else — begin/points/end/clear and legacy strokes — stays on the opaque
 * data path. We match the type token exactly to avoid tripping on a brush named "undo" etc.
 */
fun isClientControlFrame(raw: String): Boolean =
    raw.contains("\"type\":\"hello\"") || raw.contains("\"type\":\"undo\"")
