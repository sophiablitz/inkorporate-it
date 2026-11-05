package com.blitz.inklings.net

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.blitz.inklings.ui.stockBrushes
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

/**
 * Client for the inklings-server `/relay` route, now with LIVE streaming.
 *
 * Instead of only sending a stroke once it is finished, a stroke is streamed as it is
 * drawn so other screens watch it appear point by point:
 *
 *   begin  -> [RelayListener] / server: {"type":"begin","id":..,brush/color/size/epsilon}
 *   points -> {"type":"points","id":..,"points":[{x,y,t,p?}, ...]}   (repeatedly)
 *   end    -> {"type":"end","id":..}
 *   clear  -> {"type":"clear"}
 *
 * On connect the server sends {"type":"snapshot","strokes":[..]} so a late joiner sees
 * everything already on the canvas, including strokes still in progress.
 *
 * Incoming events are delivered through [listener] on the main thread so they can drive
 * Compose state directly.
 *
 * NOTE: with localhost, the server is ONLY reachable through adb. `localhost` on a device
 * means the device itself, so each device needs an adb tunnel to the PC:
 *
 *     adb reverse tcp:8080 tcp:8080          (add -s <device-id> if several are connected)
 *
 * The tunnel is lost when the device disconnects, the emulator restarts, or adb restarts —
 * run it again. Without it, Logcat shows "connection to ... failed". Assumes the server is
 * on port 8080 (inklings-server's application.yaml).
 */
class StrokeRelay(
    private val url: String = "ws://localhost:8080/relay",
    private val listener: RelayListener,
) {
    /** Events coming from other clients (and the server's snapshot), on the main thread. */
    interface RelayListener {
        /** A remote stroke started. [id] identifies it until [onRemoteEnd]. */
        fun onRemoteBegin(id: String, brush: Brush)
        /** More points for a remote in-progress stroke. */
        fun onRemotePoints(id: String, batch: MutableStrokeInputBatch)
        /** A remote stroke finished; [stroke] is the completed stroke. */
        fun onRemoteEnd(id: String, stroke: Stroke)
        /** Everyone's canvas was cleared. */
        fun onClear()
        /** Catch-up on connect: the strokes already on the canvas. */
        fun onSnapshot(strokes: List<Stroke>)
    }

    private val client = OkHttpClient()
    private val mainThread = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null
    private val localIds = AtomicLong(0)

    fun connect() {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "connected to $url")
            }

            // OkHttp calls this on a background thread.
            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = runCatching { JSONObject(text) }.getOrNull() ?: run {
                    Log.w(TAG, "ignoring non-JSON message: $text")
                    return
                }
                mainThread.post { dispatch(obj, text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // No auto-reconnect in this minimal version: restart the app to retry.
                Log.w(TAG, "connection to $url failed", t)
            }
        })
    }

    /** Route one incoming message to the right listener callback. */
    private fun dispatch(obj: JSONObject, raw: String) {
        when (obj.optString("type")) {
            "snapshot" -> {
                val arr = obj.optJSONArray("strokes") ?: JSONArray()
                val strokes = ArrayList<Stroke>(arr.length())
                for (i in 0 until arr.length()) {
                    runCatching { strokeFromJson(arr.getJSONObject(i)) }
                        .onSuccess { strokes.add(it) }
                        .onFailure { Log.w(TAG, "bad stroke in snapshot", it) }
                }
                listener.onSnapshot(strokes)
            }
            "begin" -> {
                val id = obj.optString("id").ifEmpty { return }
                listener.onRemoteBegin(id, brushFromJson(obj))
            }
            "points" -> {
                val id = obj.optString("id").ifEmpty { return }
                listener.onRemotePoints(id, batchFromPoints(obj.optJSONArray("points")))
            }
            "end" -> {
                val id = obj.optString("id").ifEmpty { return }
                // The full stroke for an `end` is rebuilt by the caller from the points
                // it accumulated; we only need to signal completion. To keep the listener
                // simple we also allow an optional inline stroke in the end frame.
                val stroke = runCatching { strokeFromJson(obj) }.getOrNull()
                if (stroke != null) listener.onRemoteEnd(id, stroke) else listener.onRemoteEnd(id, EMPTY_SENTINEL)
            }
            "clear" -> listener.onClear()
            // Legacy single-frame stroke: full stroke, no type.
            else -> {
                val stroke = runCatching { strokeFromJson(obj) }
                    .onFailure { Log.w(TAG, "ignoring message that isn't a stroke: $raw", it) }
                    .getOrNull() ?: return
                val id = "legacy-${localIds.incrementAndGet()}"
                listener.onRemoteEnd(id, stroke)
            }
        }
    }

    // --- sending: stream a local stroke live --------------------------------------------

    /** Start streaming a new local stroke. Returns an id to pass to [streamPoints]/[endStroke]. */
    fun beginStroke(brush: Brush): String {
        val id = "a-${localIds.incrementAndGet()}"
        val brushName = brushLabel(brush)
        val msg = JSONObject().apply {
            put("type", "begin")
            put("id", id)
            put("brush", brushName)
            put("color", brush.colorIntArgb)
            put("size", brush.size.toDouble())
            put("epsilon", brush.epsilon.toDouble())
        }
        sendRaw(msg.toString())
        return id
    }

    /** Stream one or more new points for an in-progress local stroke. */
    fun streamPoints(id: String, points: List<StreamPoint>) {
        if (points.isEmpty()) return
        val arr = JSONArray()
        for (p in points) {
            arr.put(JSONObject().apply {
                put("x", p.x.toDouble())
                put("y", p.y.toDouble())
                put("t", p.t)
                if (p.pressure != null) put("p", p.pressure.toDouble())
            })
        }
        val msg = JSONObject().apply {
            put("type", "points")
            put("id", id)
            put("points", arr)
        }
        sendRaw(msg.toString())
    }

    /** Finish an in-progress local stroke. */
    fun endStroke(id: String) {
        sendRaw(JSONObject().apply { put("type", "end"); put("id", id) }.toString())
    }

    /** Broadcast a canvas clear to everyone. */
    fun sendClear() {
        sendRaw(JSONObject().apply { put("type", "clear") }.toString())
    }

    /**
     * Send one already-finished stroke as a single legacy frame. Use this for input that
     * isn't streamed live (e.g. stylus), so it still reaches other screens and the server's
     * snapshot. Finger strokes are streamed with [beginStroke]/[streamPoints]/[endStroke]
     * instead and should NOT also be sent here.
     */
    fun sendStrokeComplete(stroke: Stroke) {
        sendRaw(encode(stroke))
    }

    /** One streamed input point. */
    data class StreamPoint(val x: Float, val y: Float, val t: Long, val pressure: Float?)

    private fun sendRaw(text: String) {
        val sent = socket?.send(text) ?: false
        if (!sent) Log.w(TAG, "not sent (not connected): $text")
    }

    fun close() {
        socket?.close(1000, "screen closed")
        socket = null
    }

    companion object {
        private const val TAG = "StrokeRelay"

        /** Sentinel for an `end` with no inline stroke; the receiver rebuilds from points. */
        private val EMPTY_SENTINEL: Stroke by lazy {
            Stroke(
                Brush.createWithColorIntArgb(
                    family = stockBrushes.first().family,
                    colorIntArgb = 0,
                    size = 1f,
                    epsilon = 0.1f,
                ),
                MutableStrokeInputBatch(),
            )
        }

        private fun brushLabel(brush: Brush): String =
            stockBrushes.firstOrNull { it.family == brush.family }?.label
                ?: stockBrushes.first().label

        private fun brushFromJson(obj: JSONObject): Brush {
            val family = stockBrushes.firstOrNull { it.label == obj.optString("brush") }?.family
                ?: stockBrushes.first().family
            return Brush.createWithColorIntArgb(
                family = family,
                colorIntArgb = obj.optInt("color", 0),
                size = obj.optDouble("size", 5.0).toFloat(),
                epsilon = obj.optDouble("epsilon", 0.1).toFloat(),
            )
        }

        private fun batchFromPoints(points: JSONArray?): MutableStrokeInputBatch {
            val batch = MutableStrokeInputBatch()
            if (points == null) return batch
            for (i in 0 until points.length()) {
                val p = points.getJSONObject(i)
                batch.add(
                    type = InputToolType.TOUCH,
                    x = p.getDouble("x").toFloat(),
                    y = p.getDouble("y").toFloat(),
                    elapsedTimeMillis = p.getLong("t"),
                    pressure = if (p.has("p")) p.getDouble("p").toFloat() else StrokeInput.NO_PRESSURE,
                )
            }
            return batch
        }

        private fun strokeFromJson(obj: JSONObject): Stroke =
            Stroke(brushFromJson(obj), batchFromPoints(obj.optJSONArray("points")))

        /**
         * Stroke -> JSON for the legacy single-frame protocol. Kept for compatibility and
         * for callers that only have a finished stroke.
         */
        fun encode(stroke: Stroke): String {
            val brush = stroke.brush
            val points = JSONArray()
            val inputs = stroke.inputs
            for (i in 0 until inputs.size) {
                val input = inputs.get(i)
                points.put(JSONObject().apply {
                    put("x", input.x.toDouble())
                    put("y", input.y.toDouble())
                    put("t", input.elapsedTimeMillis)
                    if (input.pressure != StrokeInput.NO_PRESSURE) put("p", input.pressure.toDouble())
                })
            }
            return JSONObject().apply {
                put("brush", brushLabel(brush))
                put("color", brush.colorIntArgb)
                put("size", brush.size.toDouble())
                put("epsilon", brush.epsilon.toDouble())
                put("points", points)
            }.toString()
        }

        /** JSON -> Stroke. Unknown brush names fall back to the first stock brush. */
        fun decode(json: String): Stroke = strokeFromJson(JSONObject(json))
    }
}
