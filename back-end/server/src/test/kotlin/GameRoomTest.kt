package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

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
}
