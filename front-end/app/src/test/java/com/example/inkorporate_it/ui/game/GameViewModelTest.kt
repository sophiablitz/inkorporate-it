package com.example.inkorporate_it.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.inkorporate_it.data.network.ChatEntry
import com.example.inkorporate_it.data.network.GameSession
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.model.BlobColors
import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.StrokePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How GameViewModel turns what the server said (GameSession) into what the screen draws.
 * Adapted from Sophia's GameViewModelTest on front-end/game-client.
 */
class GameViewModelTest {

    private val drawingRoom = Room(
        code = "012345",
        phase = RoomPhase.DRAWING,
        hostId = "p1",
        players = listOf(
            Player("p1", "Jorge", it.inkorporate.protocol.Avatar(inkColor = "#E5484D"), score = 40),
            Player("p2", "Jerry", score = 90),
        ),
        round = 2,
        totalRounds = 3,
        drawerId = "p2",
        guessedIds = listOf("p1"),
    )

    @Test
    fun aGuesserSeesBlanksThePlayersAndTheCanvas() {
        val session = GameSession(
            myPlayerId = "p1",
            room = drawingRoom,
            wordLength = 4,
            secondsLeft = 42,
            strokes = listOf(StrokePart(1, listOf(Point(0.25f, 0.5f)), color = "#3B4FE0", width = 12f)),
            chat = listOf(ChatEntry("p2", "hi"), ChatEntry(null, "Jorge guessed it!")),
        )

        val ui = session.toUiState()

        assertEquals(GamePhase.Drawing, ui.phase)
        assertTrue(ui.isHost)
        assertFalse(ui.isMyTurnToDraw)
        assertFalse(ui.isGettingReady)
        assertEquals("_ _ _ _", ui.wordHint)
        assertEquals(42, ui.secondsLeft)
        assertEquals(2, ui.round)

        val (jorge, jerry) = ui.players
        assertEquals(BlobColors.Red, jorge.avatar.inkColor)
        assertTrue(jorge.isHost && jorge.hasGuessed && !jorge.isDrawing)
        assertTrue(jerry.isDrawing && !jerry.isHost)

        assertEquals(Stroke(listOf(Offset(0.25f, 0.5f)), BlobColors.Indigo, 12f), ui.strokes.single())
        assertEquals(listOf(ChatMessage("Jerry", "hi"), ChatMessage("", "Jorge guessed it!")), ui.chat)
    }

    @Test
    fun whileGettingReadyTheDrawerSeesTheWordAndGuessersSeeWhoIsDrawing() {
        val ready = drawingRoom.copy(phase = RoomPhase.GETTING_READY)
        val drawer = GameSession(myPlayerId = "p2", room = ready, word = "barn", secondsToStart = 10).toUiState()
        val guesser = GameSession(myPlayerId = "p1", room = ready).toUiState()

        assertTrue(drawer.isGettingReady && drawer.isMyTurnToDraw)
        assertEquals("barn", drawer.wordHint)
        assertEquals(10, drawer.secondsToStart)
        assertEquals("Jerry is getting ready", guesser.wordHint)
        assertEquals("Jerry", guesser.drawerName)
    }

    @Test
    fun theEndOfATurnShowsTheWordAndEachPlayersPoints() {
        val over = drawingRoom.copy(phase = RoomPhase.TURN_OVER)
        val ui = GameSession(myPlayerId = "p1", room = over, revealedWord = "barn", turnScores = mapOf("p1" to 151)).toUiState()

        assertEquals(GamePhase.RoundOver, ui.phase)
        assertEquals("barn", ui.revealedWord)
        assertEquals(listOf(151, 0), ui.players.map { it.pointsThisTurn })
    }

    @Test
    fun colorsAndAvatarsSurviveTheTripToTheServerAndBack() {
        BlobColors.all.forEach { color -> assertEquals(color, hexToColor(colorToHex(color))) }

        val wire = Avatar(hatId = "beanie", eyesId = "wide", inkColor = BlobColors.Teal).toWire()
        assertEquals(it.inkorporate.protocol.Avatar("beanie", "wide", "#2BB5C9"), wire)
    }

    @Test
    fun aBadColorIsBlack() {
        assertEquals(Color.Black, hexToColor("not a color"))
    }
}
