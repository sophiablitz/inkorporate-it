package it.inkorporate

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameRoomTest {

    private val host = PlayerConnection(id = "h", name = "Host")

    @Test
    fun `a new room has only the host`() {
        val room = GameRoom("123456", host)
        assertEquals(listOf(host), room.players)
    }

    @Test
    fun `players stay in join order`() {
        val room = GameRoom("123456", host)
        val ana = PlayerConnection(id = "a", name = "Ana")
        val ben = PlayerConnection(id = "b", name = "Ben")

        room.addPlayer(ana)
        room.addPlayer(ben)

        assertEquals(listOf(host, ana, ben), room.players)
    }

    @Test
    fun `removing a player keeps someone else with the same name`() {
        val room = GameRoom("123456", host)
        val sam1 = PlayerConnection(id = "s1", name = "Sam")
        val sam2 = PlayerConnection(id = "s2", name = "Sam")
        room.addPlayer(sam1)
        room.addPlayer(sam2)

        room.removePlayer(sam1)

        assertEquals(listOf(host, sam2), room.players)
    }

    // ---------- a room holds at most 8 players (#81) ----------

    private fun player(n: Int) = PlayerConnection(id = "p$n", name = "Player $n")

    @Test
    fun `a room takes up to 8 players`() {
        val room = GameRoom("123456", host)

        // The host is player 1, so 7 more fill it up.
        for (n in 2..8) assertTrue(room.addPlayer(player(n)), "player $n should get in")

        assertEquals(8, room.players.size)
    }

    @Test
    fun `the 9th player is turned away`() {
        val room = GameRoom("123456", host)
        for (n in 2..8) room.addPlayer(player(n))
        val ninth = player(9)

        assertFalse(room.addPlayer(ninth))
        assertFalse(ninth in room.players)
    }

    @Test
    fun `20 players joining at once still makes only 8`() {
        val room = GameRoom("123456", host)

        // 20 threads, each one a phone tapping Join at the same moment.
        List(20) { n -> thread { room.addPlayer(player(n + 2)) } }.forEach { it.join() }

        assertEquals(8, room.players.size)
    }

    // ---------- the last player leaving deletes the room (#84) ----------

    @Test
    fun `the room is deleted when the last player leaves`() {
        val registry = RoomRegistry()
        val room = registry.create(host)

        room.removePlayer(host)

        assertNull(registry.find(room.code))
    }

    @Test
    fun `the room stays while someone is still in it`() {
        val registry = RoomRegistry()
        val room = registry.create(host)
        val ana = PlayerConnection(id = "a", name = "Ana")
        room.addPlayer(ana)

        room.removePlayer(ana)

        assertSame(room, registry.find(room.code))
    }

    @Test
    fun `onClosed runs once even if the last player is removed twice`() {
        var calls = 0
        val room = GameRoom("123456", host, onClosed = { calls++ })

        room.removePlayer(host)
        room.removePlayer(host)

        assertEquals(1, calls)
    }

    // ---------- the host leaving closes the room (#83) ----------

    @Test
    fun `the room is deleted when the host leaves, even with others still in it`() {
        val registry = RoomRegistry()
        val room = registry.create(host)
        room.addPlayer(PlayerConnection(id = "a", name = "Ana"))

        room.removePlayer(host)

        assertNull(registry.find(room.code))
    }

    @Test
    fun `onClosed runs once when the host leaves and then everyone else does`() {
        var calls = 0
        val room = GameRoom("123456", host, onClosed = { calls++ })
        val ana = PlayerConnection(id = "a", name = "Ana")
        room.addPlayer(ana)

        room.removePlayer(host)   // closes the room
        room.removePlayer(ana)    // the room is now empty, but it's already closed

        assertEquals(1, calls)
    }

    // ---------- the server picks player ids (#79, #80) ----------

    @Test
    fun `every new player id is different`() {
        val ids = List(1000) { newPlayerId() }

        assertEquals(1000, ids.toSet().size)
    }

    // ---------- names are cleaned before they're used (#79, #80) ----------

    @Test
    fun `spaces around a name are dropped`() {
        assertEquals("Ana", cleanName("  Ana  "))
    }

    @Test
    fun `an empty or all-spaces name is turned away`() {
        assertNull(cleanName(""))
        assertNull(cleanName("     "))
    }

    @Test
    fun `a long name is cut to 16 characters`() {
        assertEquals("A".repeat(16), cleanName("A".repeat(20)))
    }

    @Test
    fun `a cut name doesn't end in a space`() {
        // "Bartholomew" + 5 spaces is exactly 16, so the cut lands on the spaces.
        assertEquals("Bartholomew", cleanName("Bartholomew     Jones"))
    }

    // ---------- play_again starts the scores over (#87) ----------

    @Test
    fun `resetScores sets every score back to 0 and keeps the players`() {
        val room = GameRoom("123456", host)
        val ana = PlayerConnection(id = "a", name = "Ana")
        room.addPlayer(ana)
        host.score = 300
        ana.score = 150

        room.resetScores()

        assertEquals(listOf(0, 0), room.players.map { it.score })
        assertEquals(listOf(host, ana), room.players)
    }
}
