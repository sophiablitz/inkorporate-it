package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.StrokePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** GameSession.apply(): what one phone knows after each server message. Pure, so no coroutines. */
class GameSessionTest {

    private val lobby = Room(
        code = "012345",
        phase = RoomPhase.LOBBY,
        hostId = "p1",
        players = listOf(Player("p1", "Jorge"), Player("p2", "Jerry")),
        totalRounds = 3,
    )
    private val gettingReady = lobby.copy(phase = RoomPhase.GETTING_READY, round = 1, drawerId = "p1")
    private val drawing = gettingReady.copy(phase = RoomPhase.DRAWING)

    private fun part(id: Int, done: Boolean = true) = StrokePart(id, listOf(Point(0.1f, 0.2f)), "#000000", done = done)

    private fun GameSession.after(vararg messages: ServerMessage) = messages.fold(this) { s, m -> s.apply(m) }

    @Test
    fun roomJoinedSetsThisPlayerAndTheRoom() {
        val session = GameSession().apply(ServerMessage.RoomJoined("p2", lobby))
        assertEquals("p2", session.myPlayerId)
        assertEquals("012345", session.room?.code)
    }

    @Test
    fun onlyTheDrawerKnowsTheWord() {
        val drawer = GameSession(myPlayerId = "p1", room = lobby)
            .after(ServerMessage.RoomUpdate(gettingReady), ServerMessage.YourWord("barn", 10))
        val guesser = GameSession(myPlayerId = "p2", room = lobby).after(ServerMessage.RoomUpdate(gettingReady))

        assertEquals("barn", drawer.word)
        assertEquals(10, drawer.secondsLeft) // the viewing time, until time_left counts it down
        assertTrue(drawer.isMyTurnToDraw)
        assertNull(guesser.word)
    }

    @Test
    fun aNewTurnClearsTheLastOne() {
        val endOfTurn = GameSession(myPlayerId = "p2", room = drawing)
            .after(
                ServerMessage.Stroke(part(1)),
                ServerMessage.Chat("p2", "box?"),
                ServerMessage.TurnEnd("barn", mapOf("p2" to 151), drawing.copy(phase = RoomPhase.TURN_OVER)),
            )
        assertEquals("barn", endOfTurn.revealedWord)
        assertEquals(1, endOfTurn.strokes.size) // the drawing stays up on the results screen

        val next = endOfTurn.apply(ServerMessage.RoomUpdate(gettingReady.copy(drawerId = "p2")))
        assertTrue(next.strokes.isEmpty())
        assertTrue(next.chat.isEmpty())
        assertNull(next.revealedWord)
        assertTrue(next.turnScores.isEmpty())
    }

    @Test
    fun anotherUpdateWhileGettingReadyKeepsTheWord() {
        val drawer = GameSession(myPlayerId = "p1", room = lobby)
            .after(ServerMessage.RoomUpdate(gettingReady), ServerMessage.YourWord("barn", 10))
            .apply(ServerMessage.RoomUpdate(gettingReady.copy(players = lobby.players + Player("p3", "Ana"))))
        assertEquals("barn", drawer.word)
    }

    @Test
    fun turnStartGivesTheBlanksAndTheTimer() {
        val session = GameSession(myPlayerId = "p2", room = gettingReady)
            .apply(ServerMessage.TurnStart(drawing, wordLength = 4, turnSeconds = 80))
        assertEquals(4, session.wordLength)
        assertEquals(80, session.secondsLeft)
        assertEquals(RoomPhase.DRAWING, session.phase)
    }

    @Test
    fun strokePartsJoinUpAndUndoRemovesTheNewest() {
        val session = GameSession(room = drawing).after(
            ServerMessage.Stroke(part(1, done = false)),
            ServerMessage.Stroke(part(1)),
            ServerMessage.Stroke(part(2)),
            ServerMessage.Undo,
        )
        assertEquals(1, session.strokes.size)
        assertEquals(2, session.strokes.single().points.size)
    }

    @Test
    fun aCorrectGuessIsANoteInTheChat() {
        val session = GameSession(room = drawing).apply(ServerMessage.CorrectGuess("p2"))
        assertEquals(listOf(ChatEntry(null, "Jerry guessed it!")), session.chat)
    }

    @Test
    fun roomClosedForgetsTheRoomAndSaysWhy() {
        val session = GameSession(myPlayerId = "p2", room = lobby).apply(ServerMessage.RoomClosed("The host left."))
        assertNull(session.room)
        assertEquals("The host left.", session.closedReason)
    }
}
