package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.RoomPhase
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

/** The real GameClient playing against the stub server, the way the app does while USE_STUB_SERVER is true. */
class StubServerSocketTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val client = GameClient(StubServerSocket(scope), scope)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `creating a room makes you host with a bot`() = runBlocking {
        client.createRoom("Jorge", Avatar())

        val session = client.state.value
        val room = session.room!!
        assertEquals(RoomPhase.LOBBY, room.phase)
        assertEquals(session.myPlayerId, room.hostId)
        assertEquals(listOf("Jorge", "Stub player"), room.players.map { it.name })
    }

    @Test
    fun `joining keeps the code you typed and the bot hosts`() = runBlocking {
        client.joinRoom("012345", "Jerry", Avatar())

        val room = client.state.value.room!!
        assertEquals("012345", room.code)
        assertTrue(room.hostId != client.state.value.myPlayerId)
    }

    @Test
    fun `the host draws round 1 and sees the word`() = runBlocking {
        client.createRoom("Jorge", Avatar())
        client.startGame()

        val session = client.state.value
        assertEquals(RoomPhase.DRAWING, session.room!!.phase)
        assertEquals(session.myPlayerId, session.room!!.drawerId)
        assertEquals(StubServerSocket.STUB_WORD, session.word)
    }

    @Test
    fun `guessing the word in round 2 scores and ends the round`() = runBlocking {
        client.createRoom("Jorge", Avatar())
        client.startGame()
        client.startNextRound() // round 2: the bot draws

        assertNull(client.state.value.word) // guessers never get the word
        client.sendChat("box?")
        client.sendChat("BARN")

        val session = client.state.value
        assertEquals(RoomPhase.ROUND_OVER, session.room!!.phase)
        assertEquals(StubServerSocket.STUB_WORD, session.revealedWord)
        assertEquals("box?", session.chat.first().text)
        assertTrue(session.chat.none { it.text.contains("barn", ignoreCase = true) && it.playerId != null })
        assertEquals(100, session.room!!.players.first { it.playerId == session.myPlayerId }.score)
    }

    @Test
    fun `after the last round the game ends, then back to the lobby`() = runBlocking {
        client.createRoom("Jorge", Avatar())
        repeat(StubServerSocket.TOTAL_ROUNDS) { client.startNextRound() }
        client.startNextRound()
        assertEquals(RoomPhase.GAME_OVER, client.state.value.room!!.phase)

        client.returnToLobby()
        assertEquals(RoomPhase.LOBBY, client.state.value.room!!.phase)
        assertEquals(0, client.state.value.room!!.round)
    }
}
