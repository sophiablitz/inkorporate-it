package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.Line
import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GameClient with FakeGameSocket: the test plays the server. The JSON is the real
 * protocol's. Dispatchers.Unconfined handles each server message as soon as it's sent.
 */
class GameClientTest {

    private val socket = FakeGameSocket()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val client = GameClient(socket, scope)

    private val lobby = Room(
        code = "012345",
        phase = RoomPhase.LOBBY,
        hostId = "p1",
        players = listOf(Player("p1", "Jorge"), Player("p2", "Jerry")),
    )

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `createRoom connects and sends create_room`() = runBlocking {
        val sent = client.createRoom("Jorge", Avatar())

        assertTrue(sent)
        assertEquals(1, socket.connectCalls)
        assertEquals(
            """{"type":"create_room","name":"Jorge","avatar":{"hatId":"none","eyesId":"round","inkColor":"#3B4FE0"}}""",
            socket.sent.single(),
        )
    }

    @Test
    fun `room_joined sets this player and the room`() = runBlocking {
        socket.serverSends(
            """{"type":"room_joined","playerId":"p1","room":{"code":"012345","phase":"lobby","hostId":"p1",
               "players":[{"playerId":"p1","name":"Jorge"},{"playerId":"p2","name":"Jerry"}]}}"""
        )

        assertEquals("p1", client.state.value.myPlayerId)
        assertEquals("012345", client.state.value.room?.code)
        assertEquals(2, client.state.value.room?.players?.size)
    }

    @Test
    fun `round_start clears the canvas and keeps the word from guessers`() = runBlocking {
        socket.serverSends("""{"type":"draw_line","line":{"points":[{"x":0.1,"y":0.2}],"color":"#000000"}}""")
        socket.serverSends(
            """{"type":"round_start","wordLength":4,"turnSeconds":80,"room":{"code":"012345",
               "phase":"drawing","hostId":"p1","players":[],"round":1,"totalRounds":3,"drawerId":"p2"}}"""
        )

        val session = client.state.value
        assertNull(session.word)
        assertEquals(4, session.wordLength)
        assertEquals(80, session.secondsLeft)
        assertTrue(session.lines.isEmpty())
    }

    @Test
    fun `chat, lines and clear from the server update the state`() = runBlocking {
        socket.serverSends("""{"type":"room_update","room":{"code":"012345","phase":"drawing","hostId":"p1","players":[{"playerId":"p2","name":"Jerry"}]}}""")
        socket.serverSends("""{"type":"chat","playerId":"p2","text":"barn"}""")
        socket.serverSends("""{"type":"correct_guess","playerId":"p2"}""")
        socket.serverSends("""{"type":"draw_line","line":{"points":[{"x":0.1,"y":0.2}],"color":"#000000"}}""")

        assertEquals(listOf(ChatEntry("p2", "barn"), ChatEntry(null, "Jerry guessed it!")), client.state.value.chat)
        assertEquals(1, client.state.value.lines.size)

        socket.serverSends("""{"type":"clear_canvas"}""")
        assertTrue(client.state.value.lines.isEmpty())
    }

    @Test
    fun `the drawer sees their own line right away`() = runBlocking {
        val line = Line(listOf(Point(0.5f, 0.5f)), color = "#E5484D")

        client.sendLine(line)

        assertEquals(listOf(line), client.state.value.lines)
        assertTrue(socket.sent.single().startsWith("""{"type":"draw_line""""))
    }

    @Test
    fun `an error from the server is kept until cleared`() = runBlocking {
        socket.serverSends("""{"type":"error","code":"room_not_found","message":"No room with that code."}""")
        assertEquals("No room with that code.", client.state.value.error)

        client.clearError()
        assertNull(client.state.value.error)
    }

    @Test
    fun `messages the app doesn't understand are skipped`() = runBlocking {
        socket.serverSends("""{"type":"room_joined","playerId":"p1","room":{"code":"012345","phase":"lobby","hostId":"p1","players":[]}}""")
        socket.serverSends("""{"type":"something_new","x":1}""")
        socket.serverSends("not json")

        assertEquals("p1", client.state.value.myPlayerId)
    }

    @Test
    fun `leaveRoom sends leave_room and forgets the game`() = runBlocking {
        socket.serverSends("""{"type":"room_joined","playerId":"p1","room":{"code":"012345","phase":"lobby","hostId":"p1","players":[]}}""")

        client.leaveRoom()

        assertEquals("""{"type":"leave_room"}""", socket.sent.single())
        assertNull(client.state.value.room)
    }

    @Test
    fun `room_closed forgets the room and says why`() = runBlocking {
        socket.serverSends("""{"type":"room_update","room":{"code":"012345","phase":"lobby","hostId":"p1","players":[]}}""")
        socket.serverSends("""{"type":"room_closed","reason":"The host left."}""")

        assertNull(client.state.value.room)
        assertEquals("The host left.", client.state.value.closedReason)
    }

    @Test
    fun `going back to the lobby clears the last round`() {
        val afterGame = GameSession(
            room = lobby.copy(phase = RoomPhase.GAME_OVER),
            revealedWord = "barn",
            lines = listOf(Line(listOf(Point(0f, 0f)), "#000000")),
            chat = listOf(ChatEntry("p2", "box?")),
        )

        val session = afterGame.apply(ServerMessage.RoomUpdate(lobby))

        assertEquals(lobby, session.room)
        assertNull(session.revealedWord)
        assertTrue(session.lines.isEmpty())
        assertTrue(session.chat.isEmpty())
    }
}
