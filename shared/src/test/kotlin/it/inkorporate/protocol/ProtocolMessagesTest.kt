package it.inkorporate.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the JSON for a few messages, so a rename on one side shows up as a failing test. */
class ProtocolMessagesTest {

    @Test
    fun `a message with no fields is just its type`() {
        assertEquals("""{"type":"start_game"}""", encodeClient(ClientMessage.StartGame))
    }

    @Test
    fun `chat from the phone`() {
        assertEquals("""{"type":"chat","text":"barn"}""", encodeClient(ClientMessage.Chat("barn")))
    }

    @Test
    fun `guessers' round_start leaves the word out`() {
        val room = Room(code = "012345", phase = RoomPhase.DRAWING, hostId = "p1", players = emptyList())
        val json = encodeServer(ServerMessage.RoundStart(room, wordLength = 4, turnSeconds = 80))

        assert("\"word\"" !in json) { json }
        assertEquals(ServerMessage.RoundStart(room, 4, 80), decodeServer(json))
    }

    @Test
    fun `every server message survives encoding and decoding`() {
        val room = Room(
            code = "012345",
            phase = RoomPhase.ROUND_OVER,
            hostId = "p1",
            players = listOf(Player("p1", "Jorge")),
            drawerId = "p1",
        )
        val line = Line(listOf(Point(0.1f, 0.9f)), color = "#000000")
        val messages = listOf(
            ServerMessage.RoomJoined("p1", room),
            ServerMessage.RoomUpdate(room),
            ServerMessage.RoundStart(room, 4, 80, word = "barn"),
            ServerMessage.TimeLeft(12),
            ServerMessage.DrawLine(line),
            ServerMessage.ClearCanvas,
            ServerMessage.Chat("p1", "hi"),
            ServerMessage.CorrectGuess("p1"),
            ServerMessage.RoundEnd("barn", mapOf("p1" to 100), room),
            ServerMessage.GameEnd(room),
            ServerMessage.RoomClosed("The host left."),
            ServerMessage.ErrorMessage(ErrorCode.ROOM_FULL, "That room is full."),
        )
        messages.forEach { assertEquals(it, decodeServer(encodeServer(it))) }
    }
}
