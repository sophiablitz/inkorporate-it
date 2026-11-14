package it.inkorporate.server

import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.ServerMessage

/**
 * The server's copy of the canvas, so someone who joins mid-drawing can be caught up with a
 * `canvas_snapshot` (protocol v0).
 *
 * Strokes are stored oldest first under a server-assigned strokeId. Phones number their own
 * strokes from 1, which would collide (two phones, or one phone after a restart), so the
 * server gives every stroke its own id and relays it under that id.
 *
 * Not thread-safe by itself: the /game route calls it while holding its lock.
 */
class CanvasState {
    private val strokes = LinkedHashMap<Int, ServerMessage.Stroke>()
    private var nextId = 1

    /** A fresh server-side strokeId. */
    fun newStrokeId(): Int = nextId++

    /**
     * Add one batch from a phone, stored under [serverId]. Returns the message to relay to
     * the other phones (the same batch, renumbered).
     */
    fun addBatch(serverId: Int, batch: ClientMessage.Stroke): ServerMessage.Stroke {
        val relay = ServerMessage.Stroke(
            strokeId = serverId,
            points = batch.points,
            color = batch.color,
            size = batch.size.coerceIn(1, 3),
            done = batch.done,
            brush = batch.brush,
        )
        val existing = strokes[serverId]
        strokes[serverId] = if (existing == null) {
            relay
        } else {
            existing.copy(points = existing.points + batch.points, done = batch.done)
        }
        return relay
    }

    /** Remove the newest stroke. */
    fun undo() {
        val last = strokes.keys.lastOrNull() ?: return
        strokes.remove(last)
    }

    fun clear() = strokes.clear()

    /**
     * Empties the canvas when a new turn starts. [clear] is for the drawer's Clear button.
     * strokeIds are not reset, so new strokes never reuse an old id.
     */
    fun reset() = strokes.clear()

    /**
     * Handles one drawing message (stroke, undo or clear) from a phone and returns what to
     * send to the other phones. Returns null for anything that isn't drawing.
     * Called for every batch as it arrives, so the other phones see the drawing live.
     *
     * [idMap] is that phone's own strokeId -> server strokeId, for strokes it is still drawing.
     */
    fun handle(msg: ClientMessage, idMap: MutableMap<Int, Int>): ServerMessage? = when (msg) {
        is ClientMessage.Stroke -> {
            val serverId = idMap.getOrPut(msg.strokeId) { newStrokeId() }
            if (msg.done) idMap.remove(msg.strokeId)
            addBatch(serverId, msg)
        }
        ClientMessage.Undo -> ServerMessage.Undo.also { undo() }
        ClientMessage.Clear -> ServerMessage.Clear.also { clear() }
        else -> null
    }

    /** Everything on the canvas, for a phone that just joined. */
    fun snapshot() = ServerMessage.CanvasSnapshot(strokes.values.toList())
}
