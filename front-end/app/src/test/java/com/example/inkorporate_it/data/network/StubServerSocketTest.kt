package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.RoomPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real GameClient playing against the stub server, the way the app does while
 * USE_STUB_SERVER is true. Adapted from Sophia's StubServerSocketTest.
 */
class StubServerSocketTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    // 1 stub "second" is 10 ms, so a 20-second turn takes 0.2 s.
    private val stub = StubServerSocket(scope, millisPerSecond = 10)
    private val client = GameClient(stub, scope)

    @After
    fun tearDown() = scope.cancel()

    /** Waits (up to 2 s) until the game reaches [phase]. */
    private suspend fun awaitPhase(phase: RoomPhase) = withTimeout(2_000) {
        while (client.state.value.phase != phase) delay(2)
    }

    private val you get() = client.state.value.room!!.players.first { it.playerId == client.state.value.myPlayerId }

    @Test
    fun creatingARoomMakesYouHostWithTwoBots() = runBlocking {
        client.createRoom("Jorge", Avatar())
        val session = client.state.value
        assertEquals(RoomPhase.LOBBY, session.phase)
        assertEquals(session.myPlayerId, session.room!!.hostId)
        assertEquals(listOf("Jorge", "Stub player 1", "Stub player 2"), session.room!!.players.map { it.name })
    }

    @Test
    fun onYourTurnYouSeeTheWordAndTheBotsGuessItForPoints() = runBlocking {
        client.createRoom("Jorge", Avatar())
        client.startGame()
        assertEquals(RoomPhase.GETTING_READY, client.state.value.phase)
        assertEquals(StubServerSocket.STUB_WORD, client.state.value.word)

        client.startDrawing()
        awaitPhase(RoomPhase.TURN_OVER) // both bots guess, which ends the turn early

        val scores = client.state.value.turnScores
        assertEquals(StubServerSocket.GUESS_POINTS.take(2), listOf(scores["bot"], scores["guesser"]))
        assertEquals(2 * StubServerSocket.DRAWER_POINTS_PER_GUESS, scores[client.state.value.myPlayerId])
        assertEquals(120, you.score)
    }

    @Test
    fun guessingTheBotsWordScoresAndNeverShowsTheWordInChat() = runBlocking {
        client.joinRoom("012345", "Jerry", Avatar()) // "Stub player 1" hosts, so it draws first
        client.startGame()
        assertNull(client.state.value.word) // guessers never get the word
        awaitPhase(RoomPhase.DRAWING)

        client.sendChat("box?")
        client.sendChat("BARN")
        awaitPhase(RoomPhase.TURN_OVER)

        val session = client.state.value
        assertEquals(StubServerSocket.STUB_WORD, session.revealedWord)
        assertEquals("box?", session.chat.first().text)
        assertTrue(session.chat.none { it.playerId != null && it.text.equals("barn", ignoreCase = true) })
        assertTrue(you.score > 0)
    }

    @Test
    fun theGameEndsAfterEveryoneDrawsEveryRound() = runBlocking {
        client.createRoom("Jorge", Avatar())
        client.startGame()
        withTimeout(5_000) {
            while (client.state.value.phase != RoomPhase.GAME_OVER) {
                if (client.state.value.phase == RoomPhase.TURN_OVER) client.nextTurn()
                delay(2)
            }
        }
        assertEquals(2, client.state.value.room!!.round)
        assertTrue(client.state.value.room!!.players.all { it.score > 0 })

        client.returnToLobby()
        assertEquals(RoomPhase.LOBBY, client.state.value.phase)
    }

    @Test
    fun everyCountdownSendsTheTimeLeft() = runBlocking {
        client.createRoom("Jorge", Avatar())
        client.startGame()
        assertEquals(StubServerSocket.SECONDS_TO_START, client.state.value.secondsLeft) // looking at the word

        client.startDrawing()
        awaitPhase(RoomPhase.TURN_OVER) // the bots guess, which ends the turn
        withTimeout(1_000) { while (client.state.value.secondsLeft != StubServerSocket.RESULTS_SECONDS - 1) delay(1) }
        // The results count down too, from RESULTS_SECONDS.
    }

    @Test
    fun hurryTimerEndsEachStepASecondLater() = runBlocking {
        // 1 stub "second" = 400 ms: every step is longer than the 1 s each wait allows
        // (5 s = 2 s, 20 s = 8 s, 6 s = 2.4 s), while a hurried step takes only 0.4 s.
        val slowScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val slow = StubServerSocket(slowScope, millisPerSecond = 400)
        val slowClient = GameClient(slow, slowScope)
        suspend fun awaitSlow(phase: RoomPhase) = withTimeout(1_000) {
            while (slowClient.state.value.phase != phase) delay(5)
        }

        slowClient.createRoom("Jorge", Avatar())
        slowClient.startGame() // you draw first: 3 s to look at the word

        slow.hurryTimer()
        awaitSlow(RoomPhase.DRAWING)  // a second later, not 5
        slow.hurryTimer()
        assertEquals(1, slowClient.state.value.secondsLeft)
        awaitSlow(RoomPhase.TURN_OVER) // a second later, not 20
        slow.hurryTimer()
        awaitSlow(RoomPhase.GETTING_READY) // the next turn, a second later, not 6
        slowScope.cancel()
    }
}
