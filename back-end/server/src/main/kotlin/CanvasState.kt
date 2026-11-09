package com.blitz

import java.util.concurrent.ConcurrentHashMap

/**
 * The server's view of the shared drawing, kept so a late joiner can be caught up.
 *
 * The server never renders anything and treats stroke contents as opaque. It only
 * reads the two fields it needs to maintain state — `type` and `id` — and otherwise
 * stores the raw JSON pieces clients send, reassembling them into whole strokes for
 * the snapshot it sends on connect.
 *
 * Live-streaming protocol (JSON text frames, one message per frame):
 *   begin  : {"type":"begin","id":"s1","brush":"Marker","color":-16777216,"size":12.0,"epsilon":0.1}
 *   points : {"type":"points","id":"s1","points":[{"x":1.0,"y":2.0,"t":0}, ...]}
 *   end    : {"type":"end","id":"s1"}
 *   clear  : {"type":"clear"}
 * Back-compat: a frame with no "type" but with "points" is one complete stroke.
 *
 * Snapshot (server -> a newly connected client only):
 *   {"type":"snapshot","strokes":[<complete stroke object>, ...]}
 * Each stroke object has the begin fields plus the accumulated "points" array, i.e.
 * exactly the shape the old single-frame protocol used, so clients can render it with
 * the same code path. In-progress strokes are included so you can join mid-draw.
 */
class CanvasState {
    /**
     * A stroke being assembled on the server: the header fields from `begin` (or from a
     * legacy full stroke) and every point seen so far. `points` is the raw JSON for each
     * point object, concatenated verbatim, so we never lose precision or reorder fields.
     */
    private class StrokeBuilder(val id: String, var header: String, val owner: String?) {
        val points = StringBuilder()
        private var count = 0

        fun addPointsJsonArray(arrayBody: String) {
            val trimmed = arrayBody.trim()
            if (trimmed.isEmpty()) return
            if (count > 0) points.append(',')
            points.append(trimmed)
            count++
        }

        /**
         * Reassemble a complete stroke object: header fields + the points array, plus the
         * server-assigned `owner` so a late joiner knows who drew it (attribution). The
         * owner is included only when known.
         */
        fun toStrokeJson(): String {
            val ownerField = if (owner != null) ",\"owner\":\"$owner\"" else ""
            return "{$header$ownerField,\"points\":[$points]}"
        }
    }

    // Insertion-ordered so the snapshot replays strokes in the order they were drawn.
    private val strokes = LinkedHashMap<String, StrokeBuilder>()
    private val lock = Any()
    private var anonymousCounter = 0L

    /**
     * Update canvas state from one raw client frame. Unknown shapes are ignored.
     *
     * [owner] is the server-assigned player id of the sender, attached to new strokes for
     * attribution and undo. It is supplied by the caller (not read from the frame) so a
     * client can't claim to have drawn someone else's stroke. Pass null when the sender
     * hasn't identified via `hello`.
     */
    fun apply(json: String, owner: String? = null) {
        val type = extractStringField(json, "type")
        when (type) {
            "clear" -> synchronized(lock) { strokes.clear() }

            "begin" -> {
                val id = extractStringField(json, "id") ?: return
                val header = headerFields(json)
                synchronized(lock) { strokes[id] = StrokeBuilder(id, header, owner) }
            }

            "points" -> {
                val id = extractStringField(json, "id") ?: return
                val arrayBody = extractArrayBody(json, "points") ?: return
                synchronized(lock) { strokes[id]?.addPointsJsonArray(arrayBody) }
            }

            "end" -> {
                // Keep the finished stroke in the canvas; nothing else to do.
            }

            // No recognized type. Legacy single-frame stroke: has points, no begin/end.
            null -> {
                val arrayBody = extractArrayBody(json, "points") ?: return
                synchronized(lock) {
                    val id = extractStringField(json, "id") ?: "legacy-${anonymousCounter++}"
                    val header = headerFields(json)
                    val b = StrokeBuilder(id, header, owner)
                    b.addPointsJsonArray(arrayBody)
                    strokes[id] = b
                }
            }
        }
    }

    /**
     * Remove [owner]'s most recently drawn stroke, if any, and return its id so the caller
     * can tell every client to drop it. Returns null if that owner has no strokes left.
     *
     * "Most recent" uses the insertion order of [strokes] (a [LinkedHashMap]), which is the
     * order strokes were begun — the same order the snapshot replays them in.
     */
    fun undoLast(owner: String): String? = synchronized(lock) {
        val lastId = strokes.entries.lastOrNull { it.value.owner == owner }?.key ?: return null
        strokes.remove(lastId)
        lastId
    }

    /** The current canvas as a single snapshot frame for a newly connected client. */
    fun snapshotJson(): String {
        val body = synchronized(lock) {
            strokes.values.joinToString(",") { it.toStrokeJson() }
        }
        return "{\"type\":\"snapshot\",\"strokes\":[$body]}"
    }

    // --- tiny field extractors -------------------------------------------------------
    // The frames are flat, machine-generated JSON, so we read the few fields we need
    // without pulling in a JSON library. These are deliberately conservative: anything
    // they can't parse cleanly is treated as absent, and the raw frame is still relayed.

    /** The header is every top-level field except `type`, `id`, and `points`. */
    private fun headerFields(json: String): String {
        val keep = listOf("brush", "color", "size", "epsilon")
        val parts = ArrayList<String>()
        for (key in keep) {
            val raw = extractRawField(json, key) ?: continue
            parts.add("\"$key\":$raw")
        }
        return parts.joinToString(",")
    }

    /** Value of a string field, unescaped only for the simple ids/types we emit. */
    private fun extractStringField(json: String, key: String): String? {
        val raw = extractRawField(json, key) ?: return null
        val t = raw.trim()
        if (t.length >= 2 && t.first() == '"' && t.last() == '"') {
            return t.substring(1, t.length - 1)
        }
        return null
    }

    /**
     * Raw (unparsed) value for `"key": <value>` where value is a string, number, or
     * boolean — i.e. a scalar that ends at the next comma or closing brace at depth 0.
     * Returns null for objects/arrays; use [extractArrayBody] for arrays.
     */
    private fun extractRawField(json: String, key: String): String? {
        var i = findKeyValueStart(json, key) ?: return null
        val start = i
        if (start >= json.length) return null
        if (json[start] == '{' || json[start] == '[') return null
        var end = start
        while (end < json.length) {
            val c = json[end]
            if (c == ',' || c == '}' || c == ']') break
            end++
        }
        val value = json.substring(start, end).trim()
        return value.ifEmpty { null }
    }

    /** The inside of an array field `"key":[ ... ]`, matching nested brackets. */
    private fun extractArrayBody(json: String, key: String): String? {
        var i = findKeyValueStart(json, key) ?: return null
        if (i >= json.length || json[i] != '[') return null
        var depth = 0
        val start = i + 1
        var j = i
        while (j < json.length) {
            when (json[j]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return json.substring(start, j)
                }
            }
            j++
        }
        return null
    }

    /**
     * Find `"key"` used as an object key — i.e. immediately followed (modulo whitespace)
     * by ':' — and return the index of the start of its value. Skips occurrences where
     * the same text appears as a value, e.g. the `"points"` inside `"type":"points"`.
     */
    private fun findKeyValueStart(json: String, key: String): Int? {
        val marker = "\"$key\""
        var from = 0
        while (true) {
            val at = json.indexOf(marker, from)
            if (at < 0) return null
            val afterKey = at + marker.length
            val valueStart = skipColon(json, afterKey)
            if (valueStart != null) return valueStart
            from = afterKey
        }
    }

    /** Advance past whitespace and a single ':' after a key. */
    private fun skipColon(json: String, from: Int): Int? {
        var i = from
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != ':') return null
        i++
        while (i < json.length && json[i].isWhitespace()) i++
        return i
    }
}
