package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
