package it.inkorporate.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins the JSON for a few messages, so a rename on one side shows up as a failing test. */
class ProtocolMessagesTest {

    private val room = Room(
        code = "012345",
        phase = RoomPhase.TURN_OVER,
        hostId = "p1",
        players = listOf(Player("p1", "Jorge")),
        round = 1,
        totalRounds = 3,
        drawerId = "p1",
    )
    private val part = StrokePart(strokeId = 1, points = listOf(Point(0.1f, 0.9f)), color = "#000000")

    @Test
    fun `a message with no fields is just its type`() {
        assertEquals("""{"type":"start_game"}""", encodeClient(ClientMessage.StartGame))
        assertEquals("""{"type":"start_drawing"}""", encodeClient(ClientMessage.StartDrawing))
    }

    @Test
    fun `chat from the phone`() {
        assertEquals("""{"type":"chat","text":"barn"}""", encodeClient(ClientMessage.Chat("barn")))
    }

    @Test
    fun `a stroke part carries its id and whether it's the last part`() {
        assertEquals(
            """{"type":"stroke","part":{"strokeId":1,"points":[{"x":0.1,"y":0.9}],"color":"#000000","width":8.0,"done":true}}""",
            encodeClient(ClientMessage.Stroke(part)),
        )
    }

    @Test
    fun `turn_start never carries the word`() {
        val json = encodeServer(ServerMessage.TurnStart(room, wordLength = 4, turnSeconds = 80))
        assertTrue("\"word\"" !in json, json)
    }

    @Test
    fun `phases go out in snake_case`() {
        val json = encodeServer(ServerMessage.RoomUpdate(room.copy(phase = RoomPhase.GETTING_READY)))
        assertTrue("\"phase\":\"getting_ready\"" in json, json)
    }

    @Test
    fun `every client message survives encoding and decoding`() {
        val messages = listOf(
            ClientMessage.CreateRoom("Jorge", Avatar()),
            ClientMessage.JoinRoom("012345", "Jerry", Avatar(inkColor = "#E5484D")),
            ClientMessage.LeaveRoom,
            ClientMessage.CloseRoom,
            ClientMessage.StartGame,
            ClientMessage.StartDrawing,
            ClientMessage.Stroke(part),
            ClientMessage.Undo,
            ClientMessage.ClearCanvas,
            ClientMessage.Chat("barn"),
            ClientMessage.NextTurn,
            ClientMessage.ReturnToLobby,
        )
        messages.forEach { assertEquals(it, decodeClient(encodeClient(it))) }
    }

    @Test
    fun `every server message survives encoding and decoding`() {
        val messages = listOf(
            ServerMessage.RoomJoined("p1", room),
            ServerMessage.RoomUpdate(room),
            ServerMessage.YourWord("barn", secondsToStart = 10),
            ServerMessage.TurnStart(room, 4, 80),
            ServerMessage.TimeLeft(12),
            ServerMessage.Stroke(part),
            ServerMessage.Undo,
            ServerMessage.ClearCanvas,
            ServerMessage.Chat("p1", "hi"),
            ServerMessage.CorrectGuess("p1"),
            ServerMessage.TurnEnd("barn", mapOf("p1" to 100), room),
            ServerMessage.GameEnd(room),
            ServerMessage.RoomClosed("The host left."),
            ServerMessage.ErrorMessage(ErrorCode.ROOM_FULL, "That room is full."),
        )
        messages.forEach { assertEquals(it, decodeServer(encodeServer(it))) }
    }
}
