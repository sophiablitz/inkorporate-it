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

/**
 * Minimal client for the inklings-server `/relay` route.
 *
 * Every stroke you finish is sent as JSON; every stroke someone else sends arrives
 * through [onStrokeReceived] (on the main thread, so it can update Compose state).
 *
 * The emulator reaches your PC's localhost at 10.0.2.2.
 */
class StrokeRelay(
    private val url: String = "ws://10.0.2.2:8080/relay",
    private val onStrokeReceived: (Stroke) -> Unit,
) {
    private val client = OkHttpClient()
    private val mainThread = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "connected to $url")
            }

            // OkHttp calls this on a background thread
            override fun onMessage(webSocket: WebSocket, text: String) {
                val stroke = runCatching { decode(text) }
                    .onFailure { Log.w(TAG, "ignoring message that isn't a stroke: $text", it) }
                    .getOrNull() ?: return
                mainThread.post { onStrokeReceived(stroke) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // No auto-reconnect in this minimal version: restart the app to retry
                Log.w(TAG, "connection to $url failed", t)
            }
        })
    }

    fun send(stroke: Stroke) {
        val sent = socket?.send(encode(stroke)) ?: false
        if (!sent) Log.w(TAG, "stroke not sent (not connected)")
    }

    fun close() {
        socket?.close(1000, "screen closed")
        socket = null
    }

    companion object {
        private const val TAG = "StrokeRelay"

        /**
         * Stroke -> JSON:
         * {"brush":"Marker","color":-16777216,"size":5.0,"epsilon":0.1,
         *  "points":[{"x":10.0,"y":20.0,"t":0,"p":0.5}, ...]}
         * "p" (pressure) is only present for stylus input.
         */
        fun encode(stroke: Stroke): String {
            val brush = stroke.brush
            val brushName = stockBrushes.firstOrNull { it.family == brush.family }?.label
                ?: stockBrushes.first().label
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
                put("brush", brushName)
                put("color", brush.colorIntArgb)
                put("size", brush.size.toDouble())
                put("epsilon", brush.epsilon.toDouble())
                put("points", points)
            }.toString()
        }

        /** JSON -> Stroke. Unknown brush names fall back to the first stock brush. */
        fun decode(json: String): Stroke {
            val obj = JSONObject(json)
            val family = stockBrushes.firstOrNull { it.label == obj.optString("brush") }?.family
                ?: stockBrushes.first().family
            val brush = Brush.createWithColorIntArgb(
                family = family,
                colorIntArgb = obj.getInt("color"),
                size = obj.getDouble("size").toFloat(),
                epsilon = obj.optDouble("epsilon", 0.1).toFloat(),
            )
            val batch = MutableStrokeInputBatch()
            val points = obj.getJSONArray("points")
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
            return Stroke(brush, batch)
        }
    }
}
