package com.example.inkorporate_it.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.inkorporate_it.data.network.ChatEntry
import com.example.inkorporate_it.data.network.GameSession
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.model.BlobColors
import it.inkorporate.protocol.Line
import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** How GameViewModel turns what the server said (GameSession) into what the screen draws. */
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
    fun `a guesser sees blanks, the players and the canvas`() {
        val session = GameSession(
            myPlayerId = "p1",
            room = drawingRoom,
            wordLength = 4,
            secondsLeft = 42,
            lines = listOf(Line(listOf(Point(0.25f, 0.5f)), color = "#3B4FE0", width = 12f)),
            chat = listOf(ChatEntry("p2", "hi"), ChatEntry(null, "Jorge guessed it!")),
        )

        val ui = session.toUiState()

        assertEquals(GamePhase.Drawing, ui.phase)
        assertEquals("012345", ui.roomCode)
        assertTrue(ui.isHost)
        assertFalse(ui.isMyTurnToDraw)
        assertEquals("_ _ _ _", ui.wordHint)
        assertEquals(42, ui.secondsLeft)
        assertEquals(2, ui.round)
        assertEquals(3, ui.totalRounds)

        val (jorge, jerry) = ui.players
        assertEquals(BlobColors.Red, jorge.avatar.inkColor)
        assertTrue(jorge.isHost && jorge.hasGuessed && !jorge.isDrawing)
        assertTrue(jerry.isDrawing && !jerry.isHost)

        assertEquals(Stroke(listOf(Offset(0.25f, 0.5f)), BlobColors.Indigo, 12f), ui.strokes.single())
        assertEquals(listOf(ChatMessage("Jerry", "hi"), ChatMessage("", "Jorge guessed it!")), ui.chat)
    }

    @Test
    fun `the drawer sees the whole word`() {
        val ui = GameSession(myPlayerId = "p2", room = drawingRoom, word = "barn", wordLength = 4).toUiState()

        assertTrue(ui.isMyTurnToDraw)
        assertFalse(ui.isHost)
        assertEquals("barn", ui.wordHint)
    }

    @Test
    fun `colors and avatars survive the trip to the server and back`() {
        BlobColors.all.forEach { color -> assertEquals(color, hexToColor(colorToHex(color))) }

        val wire = Avatar(hatId = "beanie", eyesId = "wide", inkColor = BlobColors.Teal).toWire()
        assertEquals(it.inkorporate.protocol.Avatar("beanie", "wide", "#2BB5C9"), wire)
    }

    @Test
    fun `a bad color is black`() {
        assertEquals(Color.Black, hexToColor("not a color"))
    }
}
