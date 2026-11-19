package it.inkorporate.server

import it.inkorporate.protocol.ClientMessage
import kotlin.test.Test
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
}
