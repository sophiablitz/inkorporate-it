package it.inkorporate

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// Section 1 
// After my changes post code review, this file holds RoomRegistry, PlayerConnection and GameRoom.

// TODO: use ROOM_CODE_LENGTH from shared/ once the protocol is in the server build.
private const val CODE_LENGTH = 6

// How many codes create() tries before giving up, so it can never loop forever
// if the codes somehow run out. With a million codes it never gets close.
private const val MAX_CREATE_ATTEMPTS = 100

/**
 * One phone that has joined a room. The server picks [id]; two players can
 * share a [name], so [id] is what tells them apart.
 *
 * TODO: add avatar once shared/ is in the server build, and the WebSocket
 * session once broadcast and sendTo arrive.
 *
 * A plain class, not a data class, for the same reason as [GameRoom]: two
 * players called "Sam" are still two different players.
 */
class PlayerConnection(val id: String, val name: String) {
    var score: Int = 0
}

/**
 * One game. The [host] is the player who created it, and is always the first
 * entry in [players]. Still to come: state (#85), broadcast and sendTo.
 *
 * Keep this a plain class, not a data class. [RoomRegistry.remove] relies on two
 * rooms with the same code still being different rooms.
 */
class GameRoom(val code: String, val host: PlayerConnection) {
    private val joined = mutableListOf(host)

    /**
     * Everyone in the room, in join order (which is also the turn order).
     * It is a copy, so nobody can change the room from outside, and so it
     * cannot change under you while you loop over it.
     */
    val players: List<PlayerConnection>
        get() = synchronized(this) { joined.toList() }

    // Two phones can join or leave the same room at the same moment, and a
    // plain list breaks if two threads change it at once. synchronized lets
    // only one of them in at a time.
    fun addPlayer(player: PlayerConnection) {
        synchronized(this) { joined.add(player) }
    }

    fun removePlayer(player: PlayerConnection) {
        synchronized(this) { joined.remove(player) }
    }
}

/**
 * Every live room on the server, by code. There is one registry for the whole
 * server, and every phone's connection uses it at the same time, so it has to
 * be safe when two phones create or leave rooms at the same moment.
 */
class RoomRegistry {
    companion object {
        /** A random room code: exactly CODE_LENGTH digits, leading zeros allowed. */
        fun randomRoomCode(random: Random = Random.Default): String =
            buildString { repeat(CODE_LENGTH) { append(random.nextInt(10)) } }
    }
    private val rooms = ConcurrentHashMap<String, GameRoom>()

    val size: Int get() = rooms.size

    /**
     * Make a new room under a code nobody else has, and return it.
     * [host] becomes the room's first player.
     *
     * putIfAbsent checks and adds in one step, so two phones creating rooms at
     * the same moment can never end up with the same code. It returns null when
     * the code was free (our room went in), or the room already there when it
     * wasn't, in which case we try another code.
     */
    fun create(host: PlayerConnection): GameRoom {
        repeat(MAX_CREATE_ATTEMPTS) {
            val room = GameRoom(randomRoomCode(), host)
            if (rooms.putIfAbsent(room.code, room) == null) return room
        }
        error("No free room code after $MAX_CREATE_ATTEMPTS tries")
    }

    /** The room with this code, or null if there isn't one (join_room sends ROOM_NOT_FOUND). */
    fun find(code: String): GameRoom? = rooms[code]

    /**
     * Take [room] out of the registry.
     *
     * It takes the room, not just the code, on purpose: if this room was already
     * removed and a new room got the same code, removing by code alone would
     * delete the new room. The two-argument remove only removes the entry if the
     * code still points at this exact room.
     */
    fun remove(room: GameRoom) {
        rooms.remove(room.code, room)
    }
}
