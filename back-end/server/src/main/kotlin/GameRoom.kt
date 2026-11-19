package it.inkorporate

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// Section 1 (Rooms and connections). This file will hold RoomRegistry,
// PlayerConnection and GameRoom. For now (#77) it's only the registry and a
// bare GameRoom that #78 fills in.

// TODO: use ROOM_CODE_LENGTH from shared/ once the protocol is in the server build.
private const val CODE_LENGTH = 6

/**
 * One game. #78 adds the host, players, state, broadcast and sendTo.
 *
 * Keep this a plain class, not a data class. [RoomRegistry.remove] relies on two
 * rooms with the same code still being different rooms.
 */
class GameRoom(val code: String)

/**
 * A random room code: exactly [CODE_LENGTH] digits, leading zeros allowed
 * ("004213" is valid). It's a String, not an Int, so the zeros survive.
 *
 * Only makes a code. Making sure nobody else already has it is
 * [RoomRegistry.create]'s job.
 */
fun randomRoomCode(random: Random = Random.Default): String =
    buildString { repeat(CODE_LENGTH) { append(random.nextInt(10)) } }

/**
 * Every live room on the server, by code. There is one registry for the whole
 * server, and every phone's connection uses it at the same time, so it has to
 * be safe when two phones create or leave rooms at the same moment.
 *
 * [newCode] is where codes come from. It defaults to [randomRoomCode]. Tests
 * pass their own list of codes to force a collision.
 */
class RoomRegistry(private val newCode: () -> String = { randomRoomCode() }) {
    private val rooms = ConcurrentHashMap<String, GameRoom>()

    val size: Int get() = rooms.size

    /**
     * Make a new room under a code nobody else has, and return it.
     * #78 adds the host as a parameter.
     *
     * putIfAbsent checks and adds in one step, so two phones creating rooms at
     * the same moment can never end up with the same code. It returns null when
     * the code was free (our room went in), or the room already there when it
     * wasn't, in which case we try another code.
     */
    fun create(): GameRoom {
        while (true) {
            val room = GameRoom(newCode())
            if (rooms.putIfAbsent(room.code, room) == null) return room
        }
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
