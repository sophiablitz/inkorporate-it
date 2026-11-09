package com.blitz

import io.ktor.server.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Who is in a room, as opposed to what is drawn in it ([CanvasState]).
 *
 * Presence is deliberately a sibling of the canvas, not an extension of it: the canvas
 * stays an opaque stroke store that knows nothing about users, while identity, names, and
 * colors live here. A [Room] owns one of each.
 *
 * Identity rules:
 *   - The server assigns each connection an authoritative [PlayerInfo.id] ("p1", "p2", ...).
 *     The client never supplies its own id; it only offers a display name and color via a
 *     `hello` frame, which are treated as untrusted labels.
 *   - A connection has no player until [hello] is called. Until then it's connected (it can
 *     receive the snapshot/roster) but isn't part of the roster broadcast.
 *
 * Thread-safety: joins/leaves happen on different threads, so the map is concurrent and the
 * id counter is atomic.
 */
class Presence {
    private val players = ConcurrentHashMap<DefaultWebSocketServerSession, PlayerInfo>()
    private val nextId = AtomicLong(0)

    /** Everyone currently identified in the room (those who have sent `hello`). */
    fun roster(): List<PlayerInfo> = players.values.toList()

    /** The player for [session], or null if it hasn't sent `hello` yet. */
    fun playerOf(session: DefaultWebSocketServerSession): PlayerInfo? = players[session]

    /**
     * Record a `hello` for [session], assigning a fresh server-side id and storing the
     * client-supplied [name]/[color] as the player's labels. Returns the new [PlayerInfo].
     * Calling again for the same session replaces its labels but keeps nothing from the
     * client's side authoritative.
     */
    fun hello(session: DefaultWebSocketServerSession, name: String, color: Int): PlayerInfo {
        val id = "p${nextId.incrementAndGet()}"
        val info = PlayerInfo(id = id, name = name, color = color)
        players[session] = info
        return info
    }

    /**
     * Drop [session] from presence. Returns the [PlayerInfo] it had, or null if it never
     * sent `hello` (nothing to announce a leave for).
     */
    fun remove(session: DefaultWebSocketServerSession): PlayerInfo? = players.remove(session)
}
