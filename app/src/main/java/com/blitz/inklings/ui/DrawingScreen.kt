package com.blitz.inklings.ui

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import com.blitz.inklings.net.StrokeRelay

@Composable
fun DrawingScreen() {
    var finishedStrokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(5f) }
    var brushFamily by remember { mutableStateOf(StockBrushes.pressurePen()) }

    var currentBrush by remember(brushFamily, selectedColor, brushSize) {
        mutableStateOf(
            Brush.createWithComposeColor(
                family = brushFamily,
                color = selectedColor,
                size = brushSize,
                epsilon = 0.1f
            )
        )
    }

    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

    // Stroke relay: strokes from other devices are added to the same list the canvas draws
    val relay = remember {
        StrokeRelay(onStrokeReceived = { stroke -> finishedStrokes = finishedStrokes + stroke })
    }
    DisposableEffect(relay) {
        relay.connect()
        onDispose { relay.close() }
    }

    // Loupe: mirrors the finger stroke being drawn so it can be shown magnified beside the finger
    val latestBrush by rememberUpdatedState(currentBrush)
    var liveStroke by remember { mutableStateOf<InProgressStroke?>(null) }
    var loupeFocus by remember { mutableStateOf(Offset.Zero) }
    var loupeFrame by remember { mutableIntStateOf(0) }
    val loupeZonePx = with(LocalDensity.current) { (LOUPE_SIZE + LOUPE_MARGIN * 2).toPx() }

    Scaffold(
        topBar = {
            DrawingToolbar(
                selectedColor = selectedColor,
                selectedSize = brushSize,
                selectedBrushFamily = brushFamily,
                onColorSelected = {
                    selectedColor = it
                },
                onSizeSelected = {
                    brushSize = it
                },
                onBrushFamilySelected = {
                    brushFamily = it
                },
                onClear = { finishedStrokes = emptyList() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Observe touches on the Initial pass without consuming, so InProgressStrokes
                // still receives every event
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var trackedId: PointerId? = null
                        var downTime = 0L
                        var lastPos = Offset.Unspecified

                        fun addInput(stroke: InProgressStroke, pos: Offset, uptime: Long) {
                            // Ink rejects consecutive duplicate inputs
                            if (pos == lastPos) return
                            val elapsed = uptime - downTime
                            val batch = MutableStrokeInputBatch()
                                .add(InputToolType.TOUCH, pos.x, pos.y, elapsed)
                            runCatching {
                                stroke.enqueueInputs(batch, MutableStrokeInputBatch())
                                stroke.updateShape(elapsed)
                            }
                            lastPos = pos
                        }

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (change in event.changes) {
                                if (trackedId == null) {
                                    val isFinger = change.type != PointerType.Stylus &&
                                        change.type != PointerType.Eraser
                                    if (change.changedToDown() && isFinger) {
                                        trackedId = change.id
                                        downTime = change.uptimeMillis
                                        lastPos = Offset.Unspecified
                                        val stroke = InProgressStroke().apply { start(latestBrush) }
                                        addInput(stroke, change.position, change.uptimeMillis)
                                        liveStroke = stroke
                                        loupeFocus = change.position
                                        loupeFrame++
                                    }
                                } else if (change.id == trackedId) {
                                    val stroke = liveStroke ?: continue
                                    if (change.changedToUp() || !change.pressed) {
                                        stroke.finishInput()
                                        liveStroke = null
                                        trackedId = null
                                    } else {
                                        change.historical.forEach {
                                            addInput(stroke, it.position, it.uptimeMillis)
                                        }
                                        addInput(stroke, change.position, change.uptimeMillis)
                                        loupeFocus = change.position
                                        loupeFrame++
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Dry layer: render completed strokes onto the native Android Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val identityMatrix = Matrix()
                drawIntoCanvas { composeCanvas ->
                    finishedStrokes.forEach { stroke ->
                        canvasStrokeRenderer.draw(
                            stroke = stroke,
                            canvas = composeCanvas.nativeCanvas,
                            strokeToScreenTransform = identityMatrix
                        )
                    }
                }
            }

            // Wet layer: capture touch input and render the active stroke
            InProgressStrokes(
                defaultBrush = currentBrush,
                nextBrush = { Brush.createWithComposeColor(
                    family = brushFamily,
                    color = selectedColor,
                    size = brushSize,
                    epsilon = 0.1f
                )},
                onStrokesFinished = { newStrokes ->
                    finishedStrokes = finishedStrokes + newStrokes
                    newStrokes.forEach(relay::send)
                }
            )

            liveStroke?.let { stroke ->
                // Jump to the other corner when the finger nears the loupe
                val fingerInCorner = loupeFocus.x < loupeZonePx && loupeFocus.y < loupeZonePx
                DrawingLoupe(
                    finishedStrokes = finishedStrokes,
                    liveStroke = stroke,
                    focus = loupeFocus,
                    frame = loupeFrame,
                    renderer = canvasStrokeRenderer,
                    modifier = Modifier
                        .align(if (fingerInCorner) Alignment.TopEnd else Alignment.TopStart)
                        .padding(LOUPE_MARGIN)
                )
            }
        }
    }
}
