package it.inkorporate

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// Section 1 (Rooms and connections). This file will hold RoomRegistry,
// PlayerConnection and GameRoom. For now (#77) it's only the registry and a
// bare GameRoom that #78 fills in.

// TODO: use ROOM_CODE_LENGTH from shared/ once the protocol is in the server build.
private const val CODE_LENGTH = 6

// TODO: use MAX_PLAYERS from shared/ once the protocol has it.
private const val MAX_PLAYERS = 8

// TODO: use MAX_NAME_LENGTH from shared/ once the protocol has it.
private const val MAX_NAME_LENGTH = 16 //This is just a constant that defined the max name length of all players. Private, so that its untouchable

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
 * [onClosed] runs once the room closes: when the host leaves (#83), or when the
 * last player leaves (#84). [RoomRegistry.create] sets it to remove the room,
 * so closed rooms don't pile up on the server. A room made on its own (like in
 * a test) does nothing when it closes.
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

    /** True once the room has closed, so [onClosed] never runs twice. */
    private var closed = false
    //This is just a flag for the function that we're gonna see further bellow... 

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
     * Take [player] out of the room. The room closes, and [onClosed] runs, if
     * they were the host (#83) or the last one left (#84).
     *
     * [closed] makes sure onClosed only runs once. After the host leaves, the
     * others leave too (later they'll get room_closed), and without the flag
     * the last of them would close the room a second time. "Was removed" also
     * means removing the same player twice (say, leave_room and then the
     * disconnect) does nothing the second time.
     *
     * onClosed runs after the synchronized block, so the room isn't locked
     * while the registry does its part.
     */
    fun removePlayer(player: PlayerConnection) { // function remove the player (player param, using the PlayerConnection class) from the room (GameRoom class)
        val closesNow = synchronized(this) { //Lock this room so that no other thread can modify it while we are removing the player (The last expression is the val), this is how Kotlin blocks work
            val removed = joined.remove(player) // This is a boolean. If the player is in the room, remove them and return true, otherwise return false (They were never in the room). 
            val shouldClose = removed && !closed && (player === host || joined.isEmpty())
            // Im gonna need alot of lines to describe this: 
            // This is a tripple boolean expression, conjointed with ANDS. it checks the following: 
            // 1. removed (Did the player get removed from this room?), it's a checker for duplicate removals.
            // 2. !closed (Is the room still open or not?)... This is to ensure that the room is for one, open, and for two, that it only closes once. 
            // 3. player === host || joined.isEmpty (was the player the same host that created the room, or is the room just empty?) These are the main reasons a room closes at all: if the host leaves, or if there's nobody left inside. I'll have to check what happens if a player is left logged inside when they aren't the host, could be trouble, but that's in the future..... 
            if (shouldClose) closed = true
            // This is a removal mark. If the removal closes the room, mark this under the block immediately, and line 91 gets (closed = true). This means, in easy terms, it won't close twice ever. 
            shouldClose
        }   //this is what goes into closes now.
        if (closesNow) onClosed(this)
        //This basically tells the outside of the block "Hey, if you're listening to this, the room is closed!"
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
 * A new id for a player who creates or joins a room (#79, #80). The server
 * picks it, never the phone, so nobody can pretend to be someone else.
 *
 * A random UUID, like "3f2b9c1e-...". There are so many of them that two
 * players getting the same one won't happen in practice, so unlike room codes
 * there's nothing to check or retry.
 */
fun newPlayerId(): String = UUID.randomUUID().toString()

/**
 * The name a player typed, tidied up, or null if nothing is left of it (#79,
 * #80). create_room and join_room send INVALID_NAME when this is null.
 *
 * Spaces at both ends are dropped, then the name is cut to [MAX_NAME_LENGTH]
 * characters. The end is trimmed again after the cut, because cutting can
 * leave a space at the end ("Bartholomew     Jones" becomes "Bartholomew").
 */
fun cleanName(name: String): String? = //This is a function that basically mods the name of a person to make it readable: 
    name.trim().take(MAX_NAME_LENGTH).trimEnd().ifEmpty { null } //name (drop spaces) (Keep the firstmost 16 letters) (trim the end of the word again, for spaces in the end) (if theres nothing left, throw null and createroom / joinroom will return invalid.)
//Sidenote: why are we cutting, instead of just rejecting? 
//well, it's because it keeps the general game flow going, while the text field
//can only accept 16 characters anyway. If you guys want me to change this ask.

//If the phone sends its own Id, other people could copy another player's ID and pretend to be them. 
//There's no real retry loop... because the chance of getting a collision is small asf. 

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
