package it.inkorporate

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// Section 1 (Rooms and connections). This file will hold RoomRegistry,
// PlayerConnection and GameRoom. For now (#77) it's only the registry and a
// bare GameRoom that #78 fills in.

// TODO: use ROOM_CODE_LENGTH from shared/ once the protocol is in the server build.
private const val CODE_LENGTH = 6

// TODO: use MAX_PLAYERS from shared/ once the protocol has it.
private const val MAX_PLAYERS = 8

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
 * [onClosed] runs once the room closes, which for now means the last player
 * left (#84). [RoomRegistry.create] sets it to remove the room, so empty rooms
 * don't pile up on the server. A room made on its own (like in a test) does
 * nothing when it closes.
 *
 * Keep this a plain class, not a data class. [RoomRegistry.remove] relies on two
 * rooms with the same code still being different rooms.
 */
class GameRoom(
    val code: String,
    val host: PlayerConnection,
    private val onClosed: (GameRoom) -> Unit = {},
) {
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

    /**
     * Add [player] to the end of the room, unless it's full. Returns false when
     * the room already has [MAX_PLAYERS], so join_room can send ROOM_FULL (#81).
     * The "is it full?" check and the add happen inside the same lock. If they
     * didn't, two phones could both see 7 players, both get in, and make 9.
     */
    fun addPlayer(player: PlayerConnection): Boolean = synchronized(this) {
        if (joined.size >= MAX_PLAYERS) false else joined.add(player)
    }

    /**
     * Take [player] out of the room. If they were the last one, call [onClosed].
     *
     * "Was removed AND is now empty" means removing the same player twice
     * (say, leave_room and then the disconnect) only calls [onClosed] once.
     * onClosed runs after the synchronized block, so the room isn't locked
     * while the registry does its part.
     */
    fun removePlayer(player: PlayerConnection) {
        val lastOneOut = synchronized(this) { joined.remove(player) && joined.isEmpty() }
        if (lastOneOut) onClosed(this)
    }
}

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
     * [host] becomes the room's first player.
     *
     * putIfAbsent checks and adds in one step, so two phones creating rooms at
     * the same moment can never end up with the same code. It returns null when
     * the code was free (our room went in), or the room already there when it
     * wasn't, in which case we try another code.
     */
    fun create(host: PlayerConnection): GameRoom {
        while (true) {
            val room = GameRoom(newCode(), host, onClosed = { remove(it) })
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
