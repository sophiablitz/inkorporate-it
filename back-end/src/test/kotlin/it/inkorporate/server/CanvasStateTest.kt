package it.inkorporate.server

import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.ServerMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasStateTest {
    /** One finished stroke batch from a phone: a single black dot in the middle. */
    private fun batch() = ClientMessage.Stroke(
        strokeId = 1,
        points = listOf(listOf(0.5f, 0.5f, 0f)),
        color = "#000000",
        size = 2,
        done = true,
    )

    @Test
    fun `reset empties the canvas`() {
        val canvas = CanvasState()
        canvas.addBatch(canvas.newStrokeId(), batch())
        canvas.reset()
        assertTrue(canvas.snapshot().strokes.isEmpty())
    }

    @Test
    fun `strokeIds keep counting after reset`() {
        val canvas = CanvasState()
        val before = canvas.newStrokeId()
        canvas.reset()
        assertTrue(canvas.newStrokeId() > before)
    }

    @Test
    fun `handle returns a stroke under the server's id`() {
        val canvas = CanvasState()
        canvas.newStrokeId() // server id 1 is already used, so the phone's stroke 1 should become server id 2
        val relay = canvas.handle(batch(), HashMap()) as ServerMessage.Stroke
        assertEquals(2, relay.strokeId)
        assertEquals(1, canvas.snapshot().strokes.size)
    }

    @Test
    fun `handle frees the phone's strokeId once the stroke is done`() {
        val canvas = CanvasState()
        val idMap = HashMap<Int, Int>()
        val first = canvas.handle(batch().copy(done = false), idMap) as ServerMessage.Stroke
        val last = canvas.handle(batch(), idMap) as ServerMessage.Stroke
        assertEquals(first.strokeId, last.strokeId) // both batches are the same stroke
        assertTrue(idMap.isEmpty())
        val next = canvas.handle(batch(), idMap) as ServerMessage.Stroke
        assertTrue(next.strokeId > last.strokeId) // the phone's next "stroke 1" is a new stroke
    }

    @Test
    fun `handle returns undo and clear and updates the canvas`() {
        val canvas = CanvasState()
        canvas.handle(batch(), HashMap())
        assertEquals(ServerMessage.Undo, canvas.handle(ClientMessage.Undo, HashMap()))
        assertTrue(canvas.snapshot().strokes.isEmpty())
        canvas.handle(batch(), HashMap())
        assertEquals(ServerMessage.Clear, canvas.handle(ClientMessage.Clear, HashMap()))
        assertTrue(canvas.snapshot().strokes.isEmpty())
    }

    @Test
    fun `handle ignores messages that are not drawing`() {
        assertNull(CanvasState().handle(ClientMessage.StartGame, HashMap()))
    }
}
