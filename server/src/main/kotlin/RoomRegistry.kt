package com.blitz

import io.ktor.server.websocket.*
import java.util.concurrent.ConcurrentHashMap

/**
 * All active rooms on the server, keyed by room code.
 *
 * This is what makes "many games at once" work: instead of one global [Room], a client
 * connects with a room code and gets routed to the [Room] for that code, creating it on
 * first use. Rooms are ephemeral — when the last client leaves, the room is discarded so
 * abandoned games don't pile up in memory. A code that's used again later simply gets a
 * fresh, empty room.
 *
 * Thread-safety: clients join and leave on different threads, so the backing map is
 * concurrent and the create/remove transitions are done atomically via [compute] so we
 * never race between "last client left, remove it" and "new client arrived, reuse it".
 */
class RoomRegistry {
    private val rooms = ConcurrentHashMap<String, Room>()

    /** Number of rooms currently alive. Exposed mainly for logging/debugging. */
    val size: Int get() = rooms.size

    /**
     * Add [session] to the room named [code], creating the room if it doesn't exist yet,
     * and return that [Room]. Pair every call with [leave] in a `finally`.
     */
    fun join(code: String, session: DefaultWebSocketServerSession): Room {
        val room = rooms.computeIfAbsent(code) { Room(it) }
        room.join(session)
        return room
    }

    /**
     * Remove [session] from the room named [code]. If that empties the room, the room is
     * dropped from the registry. The emptiness check and removal happen inside [compute]
     * so a client joining at the same instant can't be stranded in a room we just deleted.
     *
     * Returns the departed client's [PlayerInfo] if it had identified via `hello` (so the
     * caller can broadcast a `leave` to whoever remains), or null otherwise. This is the
     * single authoritative removal point — callers must NOT also call [Room.leave].
     */
    fun leave(code: String, session: DefaultWebSocketServerSession): PlayerInfo? {
        var departed: PlayerInfo? = null
        rooms.compute(code) { _, room ->
            if (room == null) return@compute null
            departed = room.leave(session)
            // Returning null removes the entry; keep it while anyone is still connected.
            if (room.size == 0) null else room
        }
        return departed
    }
}
