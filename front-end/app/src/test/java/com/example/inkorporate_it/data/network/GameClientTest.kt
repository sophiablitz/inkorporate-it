package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Avatar
import it.inkorporate.protocol.ClientMessage
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
 * GameClient on a FakeGameSocket: the menu and the state are the real code, the test
 * plays the server with plain messages. Dispatchers.Unconfined handles each server
 * message as soon as it's sent. Adapted from Sophia's GameClientTest.
 */
class GameClientTest {

    private val socket = FakeGameSocket()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val client = GameClient(socket, scope)

    private val drawing = Room(
        code = "012345",
        phase = RoomPhase.DRAWING,
        hostId = "p1",
        players = listOf(Player("p1", "Jorge"), Player("p2", "Jerry")),
        drawerId = "p1",
    )

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun createRoomConnectsAndSendsCreateRoom() = runBlocking {
        assertTrue(client.createRoom("Jorge", Avatar()))
        assertEquals(1, socket.connectCalls)
        assertEquals(listOf(ClientMessage.CreateRoom("Jorge", Avatar())), socket.sent)
    }

    @Test
    fun everyServerMessageReachesTheState() = runBlocking {
        socket.serverSends(ServerMessage.RoomJoined("p1", drawing))
        socket.serverSends(ServerMessage.YourWord("barn", 10))
        assertEquals("barn", client.state.value.word)
        assertEquals("p1", client.state.value.myPlayerId)
    }

    @Test
    fun theDrawersStrokeShowsRightAwayWithTheNextId() = runBlocking {
        socket.serverSends(ServerMessage.RoomJoined("p1", drawing))
        client.sendStroke(listOf(Point(0.5f, 0.5f)), "#E5484D", 8f)
        client.sendStroke(listOf(Point(0.6f, 0.6f)), "#E5484D", 8f)

        assertEquals(listOf(1, 2), client.state.value.strokes.map { it.strokeId })
        val sent = socket.sent.filterIsInstance<ClientMessage.Stroke>().map { it.part.strokeId }
        assertEquals(listOf(1, 2), sent)
    }

    @Test
    fun undoAndClearChangeTheDrawersCanvasToo() = runBlocking {
        client.sendStroke(listOf(Point(0.5f, 0.5f)), "#000000", 8f)
        client.sendStroke(listOf(Point(0.6f, 0.6f)), "#000000", 8f)
        client.undo()
        assertEquals(1, client.state.value.strokes.size)
        client.clearCanvas()
        assertTrue(client.state.value.strokes.isEmpty())
        assertEquals(ClientMessage.ClearCanvas, socket.sent.last())
    }

    @Test
    fun startDrawingAndNextTurnSendTheirMessages() = runBlocking {
        client.startDrawing()
        client.nextTurn()
        assertEquals(listOf(ClientMessage.StartDrawing, ClientMessage.NextTurn), socket.sent)
    }

    @Test
    fun leaveRoomSendsLeaveRoomAndForgetsTheGame() = runBlocking {
        socket.serverSends(ServerMessage.RoomJoined("p1", drawing))
        client.leaveRoom()
        assertEquals(ClientMessage.LeaveRoom, socket.sent.single())
        assertNull(client.state.value.room)
    }
}
