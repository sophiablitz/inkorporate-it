package com.blitz.inklings.ui

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

val LOUPE_SIZE = 140.dp
val LOUPE_MARGIN = 16.dp
private const val ZOOM = 2f

/**
 * Magnified view of the canvas around [focus], so finger drawing stays visible under the fingertip.
 * [frame] changes whenever [liveStroke] gains input, since the stroke itself isn't snapshot state.
 */
@Composable
fun DrawingLoupe(
    finishedStrokes: List<Stroke>,
    liveStroke: InProgressStroke?,
    focus: Offset,
    frame: Int,
    renderer: CanvasStrokeRenderer,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val zoomMatrix = remember { Matrix().apply { setScale(ZOOM, ZOOM) } }
    val markerColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .size(LOUPE_SIZE)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.background)
            .border(2.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        // Read frame so each new input point redraws the loupe
        frame

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            canvas.save()
            canvas.translate(size.width / 2, size.height / 2)
            canvas.scale(ZOOM, ZOOM)
            canvas.translate(-focus.x, -focus.y)
            finishedStrokes.forEach { stroke ->
                renderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = zoomMatrix
                )
            }
            liveStroke?.let {
                renderer.draw(
                    canvas = canvas,
                    inProgressStroke = it,
                    strokeToScreenTransform = zoomMatrix
                )
            }
            canvas.restore()
        }

        // Ring marking the point under the finger
        drawCircle(
            color = markerColor,
            radius = 6.dp.toPx(),
            center = center,
            style = DrawStroke(width = 1.5.dp.toPx())
        )
    }
}
