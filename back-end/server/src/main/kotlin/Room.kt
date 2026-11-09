package com.blitz

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

/**
 * One shared drawing surface and the people connected to it.
 *
 * A [Room] bundles the state for one game/board: the connected [sessions], the [canvas]
 * they're drawing on, and the [presence] roster of who's there. Each is its own
 * collaborator — the canvas stays an opaque stroke store that knows nothing about users,
 * while names/colors/ids live in [presence]. Game rules (turns, scoring) are still NOT
 * here; they'll arrive as another collaborator so this stays a thin container.
 *
 * Two planes of traffic flow through a room (see [Protocol]):
 *   - DATA: stroke frames, forwarded verbatim via [relay]; the room doesn't interpret them.
 *   - CONTROL: presence events (hello/roster/join/leave), handled via [hello]/[broadcast].
 *
 * Thread-safety: [sessions] is a concurrent set because clients join and leave on different
 * threads; [canvas] and [presence] do their own internal synchronization.
 */
class Room(val code: String) {
    /** Everyone currently connected to this room (whether or not they've said `hello`). */
    private val sessions = ConcurrentHashMap.newKeySet<DefaultWebSocketServerSession>()

    /** The shared drawing, so a late joiner can be caught up. */
    private val canvas = CanvasState()

    /** Who's in the room: server-assigned ids plus client-supplied names/colors. */
    private val presence = Presence()

    /** Number of clients currently in the room. */
    val size: Int get() = sessions.size

    /** Register a newly connected client. Call once, when the socket opens. */
    fun join(session: DefaultWebSocketServerSession) {
        sessions += session
    }

    /**
     * Remove a client on disconnect. If the client had identified via `hello`, its
     * [PlayerInfo] is returned so the caller can broadcast a `leave`; null otherwise.
     * Safe to call from a `finally`; runs on clean disconnect and error.
     */
    fun leave(session: DefaultWebSocketServerSession): PlayerInfo? {
        sessions -= session
        return presence.remove(session)
    }

    /** The current canvas as a single snapshot frame, for catching up a new client. */
    fun snapshotJson(): String = canvas.snapshotJson()

    /** The current roster as a control frame, the presence analog of the canvas snapshot. */
    fun rosterJson(): String =
        controlJson.encodeToString<ControlMessage>(Roster(presence.roster()))

    /**
     * Record a client's `hello` and return the server-assigned [PlayerInfo]. The caller is
     * responsible for sending this client the current [rosterJson] and broadcasting a
     * [Join] to the others.
     */
    fun hello(session: DefaultWebSocketServerSession, name: String, color: Int): PlayerInfo =
        presence.hello(session, name, color)

    /**
     * Apply one raw stroke frame to the shared canvas, then forward it verbatim to every
     * OTHER client. The sender already renders its own stroke locally, so it's excluded.
     * Frame bytes are not modified — this is the opaque data path.
     *
     * The stroke is tagged with the sender's player id (if they've identified) so the
     * server knows who owns it, for attribution and undo. Ownership comes from presence,
     * never from the frame, so it can't be spoofed.
     */
    suspend fun relay(from: DefaultWebSocketServerSession, text: String) {
        val owner = presence.playerOf(from)?.id
        canvas.apply(text, owner)
        broadcast(from, text)
    }

    /**
     * Undo [from]'s most recent stroke. Removes it from the canvas and returns its id so
     * the caller can tell the room to drop it; returns null if the sender has no strokes
     * (or never identified). The requester can only undo their OWN work.
     */
    fun undo(from: DefaultWebSocketServerSession): String? {
        val owner = presence.playerOf(from)?.id ?: return null
        return canvas.undoLast(owner)
    }

    /** Send a raw text frame to every client EXCEPT [from]. Used for both planes. */
    suspend fun broadcast(from: DefaultWebSocketServerSession, text: String) {
        for (other in sessions) {
            if (other != from) other.send(Frame.Text(text))
        }
    }
}
