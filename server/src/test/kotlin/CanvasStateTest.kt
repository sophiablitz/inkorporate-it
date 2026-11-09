package com.blitz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CanvasState] — the server's opaque stroke store. These need no sockets,
 * so they're fast and deterministic. They pin down the step-4 additions (owner tagging and
 * undo) plus the pre-existing reassembly/snapshot behavior the relay depends on.
 */
class CanvasStateTest {

    @Test
    fun `begin then points then end reassembles a stroke in the snapshot`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-16777216,"size":12.0,"epsilon":0.1}""")
        c.apply("""{"type":"points","id":"s1","points":[{"x":1.0,"y":2.0,"t":0}]}""")
        c.apply("""{"type":"points","id":"s1","points":[{"x":3.0,"y":4.0,"t":10}]}""")
        c.apply("""{"type":"end","id":"s1"}""")

        val snap = c.snapshotJson()
        assertTrue(snap.contains("\"type\":\"snapshot\""))
        assertTrue(snap.contains("\"x\":1.0"), "first point present")
        assertTrue(snap.contains("\"x\":3.0"), "second point present")
        assertTrue(snap.contains("\"brush\":\"Marker\""), "header carried through")
    }

    @Test
    fun `clear empties the canvas`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""")
        c.apply("""{"type":"points","id":"s1","points":[{"x":1.0,"y":1.0,"t":0}]}""")
        c.apply("""{"type":"clear"}""")
        assertEquals("""{"type":"snapshot","strokes":[]}""", c.snapshotJson())
    }

    @Test
    fun `legacy single-frame stroke is stored`() {
        val c = CanvasState()
        c.apply("""{"brush":"Marker","color":-1,"size":5.0,"epsilon":0.1,"points":[{"x":7.0,"y":8.0,"t":0}]}""")
        assertTrue(c.snapshotJson().contains("\"x\":7.0"))
    }

    @Test
    fun `owner is recorded in the snapshot when supplied`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p1")
        c.apply("""{"type":"points","id":"s1","points":[{"x":1.0,"y":1.0,"t":0}]}""")
        assertTrue(c.snapshotJson().contains("\"owner\":\"p1\""))
    }

    @Test
    fun `owner is omitted when not supplied`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""")
        c.apply("""{"type":"points","id":"s1","points":[{"x":1.0,"y":1.0,"t":0}]}""")
        assertFalse(c.snapshotJson().contains("\"owner\""))
    }

    @Test
    fun `undoLast removes the owner's most recent stroke only`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p1")
        c.apply("""{"type":"points","id":"s1","points":[{"x":1.0,"y":1.0,"t":0}]}""")
        c.apply("""{"type":"begin","id":"s2","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p1")
        c.apply("""{"type":"points","id":"s2","points":[{"x":2.0,"y":2.0,"t":0}]}""")

        val removed = c.undoLast("p1")
        assertEquals("s2", removed, "most recent stroke is undone")
        val snap = c.snapshotJson()
        assertTrue(snap.contains("\"x\":1.0"), "older stroke remains")
        assertFalse(snap.contains("\"x\":2.0"), "undone stroke is gone")
    }

    @Test
    fun `undoLast only touches the requesting owner's strokes`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"a1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p1")
        c.apply("""{"type":"points","id":"a1","points":[{"x":1.0,"y":1.0,"t":0}]}""")
        c.apply("""{"type":"begin","id":"b1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p2")
        c.apply("""{"type":"points","id":"b1","points":[{"x":9.0,"y":9.0,"t":0}]}""")

        // p2 undoes: only b1 should go, p1's a1 stays even though it's older.
        assertEquals("b1", c.undoLast("p2"))
        val snap = c.snapshotJson()
        assertTrue(snap.contains("\"x\":1.0"), "other owner's stroke untouched")
        assertFalse(snap.contains("\"x\":9.0"))
    }

    @Test
    fun `undoLast returns null when the owner has no strokes`() {
        val c = CanvasState()
        c.apply("""{"type":"begin","id":"s1","brush":"Marker","color":-1,"size":5.0,"epsilon":0.1}""", owner = "p1")
        assertNull(c.undoLast("nobody"))
    }
}
