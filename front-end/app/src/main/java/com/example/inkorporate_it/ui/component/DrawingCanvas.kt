package com.example.inkorporate_it.ui.component


import android.content.res.Configuration
import android.graphics.Matrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.withSave
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Minimum touch target recommended by Material / Android accessibility guidance. */
private val MinTouchTarget = 48.dp

/** Ink colors are fixed (not themed) so a drawing looks the same in light and dark mode. */
private data class InkColor(val color: Color, val name: String)

private val inkColors = listOf(
    InkColor(Color(0xFF1C1A24), "Black"),
    InkColor(Color(0xFF451FCF), "Purple"),
    InkColor(Color(0xFFBA1A1A), "Red"),
    InkColor(Color(0xFF007A5A), "Green"),
    InkColor(Color(0xFF2B6CB0), "Blue"),
)

/** Brush color used until the user picks one, so they can still draw right away. */
private val DefaultInkColor = inkColors.first().color

/** Brush size options: stroke size, visual dot size in the toolbar, and a TalkBack label. */
private data class SizeOption(val size: Float, val dot: Dp, val label: String)

private val sizeOptions = listOf(
    SizeOption(6f, 8.dp, "Small brush"),
    SizeOption(12f, 14.dp, "Medium brush"),
    SizeOption(20f, 20.dp, "Large brush"),
)

// ---------------------------------------------------------------------------
// State holder
// ---------------------------------------------------------------------------

/**
 * Everything the drawing needs to remember: the finished strokes plus the tool settings.
 *
 * The caller owns it and passes it to [DrawingCanvas]. To keep the drawing when the screen
 * rotates, create it inside a ViewModel (a plain `remember { DrawingState() }` is lost on
 * rotation, because the Activity is recreated). A game screen can also read [strokes]
 * directly, for example to send the drawing to other players.
 */
@Stable
class DrawingState(initialSize: Float = 12f) {

    /** Finished (dry) strokes. A snapshot list, so changes redraw the canvas immediately. */
    val strokes = mutableStateListOf<Stroke>()

    /** null until the user picks a color; the brush falls back to black meanwhile. */
    var color by mutableStateOf<Color?>(null)

    var size by mutableFloatStateOf(initialSize)

    /** Whether the row of 5 color choices is open. */
    var isPaletteVisible by mutableStateOf(false)

    fun undo() {
        strokes.removeLastOrNull()
    }

    fun clear() {
        strokes.clear()
    }
}

// ---------------------------------------------------------------------------
// DrawingCanvas: toolbar + palette + ink canvas
// ---------------------------------------------------------------------------

/**
 * Reusable drawing area built on Jetpack Ink.
 *
 * @param state holds the strokes and tool settings; keep it in a ViewModel to survive rotation.
 * @param isDrawing true: toolbar is visible and the user can draw. false: toolbar is hidden
 *   and the canvas becomes read-only (finished strokes still show).
 * @param showClear set false on screens that shouldn't let the user wipe the drawing.
 * @param watermark faint text shown while the canvas is empty; null for none.
 */
@Composable
fun DrawingCanvas(
    state: DrawingState,
    modifier: Modifier = Modifier,
    isDrawing: Boolean = false,
    showClear: Boolean = true,
    watermark: String? = null
) {
    val outline = MaterialTheme.colorScheme.onSurface

    // Rebuilt only when the color or size changes.
    val brushColor = state.color ?: DefaultInkColor
    val currentBrush = remember(brushColor, state.size) {
        Brush.createWithComposeColor(
            family = StockBrushes.marker(),
            color = brushColor,
            size = state.size,
            epsilon = 0.1f
        )
    }
    // Lets the wet-ink layer pick up the latest brush without being recreated.
    val updatedBrush by rememberUpdatedState(currentBrush)

    Column(modifier = modifier) {

        // Toolbar is only available while the user is drawing.
        AnimatedVisibility(
            visible = isDrawing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DrawingToolbar(state = state, showClear = showClear)
        }

        // The paper stays white in both themes on purpose: ink colors are fixed, and black ink
        // would be invisible on a dark themed surface.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(3.dp, outline)
        ) {
            InkSurface(
                strokes = state.strokes,
                brush = currentBrush,
                nextBrush = { updatedBrush },
                isDrawing = isDrawing,
                watermark = watermark,
                onStrokesFinished = { state.strokes.addAll(it) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Toolbar
// ---------------------------------------------------------------------------

@Composable
private fun DrawingToolbar(state: DrawingState, showClear: Boolean) {
    val outline = MaterialTheme.colorScheme.onSurface

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brush size: one pill holding three dots (each dot is a 48dp target)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sizeOptions.forEach { option ->
                            val selected = state.size == option.size
                            Box(
                                modifier = Modifier
                                    .size(MinTouchTarget)
                                    .clip(CircleShape)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { state.size = option.size }
                                    )
                                    .semantics { contentDescription = option.label },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(option.dot)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) outline
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                        .then(
                                            if (selected) Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            ) else Modifier
                                        )
                                )
                            }
                        }
                    }
                }

                // Color button: opens or closes the palette
                val colorName = inkColors.firstOrNull { it.color == state.color }?.name
                Box(
                    modifier = Modifier
                        .size(MinTouchTarget)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Choose color") {
                            state.isPaletteVisible = !state.isPaletteVisible
                        }
                        .semantics {
                            contentDescription = "Brush color: ${colorName ?: "none selected"}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            // Neutral placeholder until a color is picked.
                            .background(state.color ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, outline, CircleShape)
                    )
                }

                // Undo: removes the last stroke
                val canUndo = state.strokes.isNotEmpty()
                Box(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .widthIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            enabled = canUndo,
                            role = Role.Button,
                            onClickLabel = "Undo last stroke",
                            onClick = state::undo
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canUndo) outline else outline.copy(alpha = 0.38f)
                    )
                }

                // Clear canvas (optional per screen)
                if (showClear) {
                    Box(
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(role = Role.Button, onClickLabel = "Clear canvas") {
                                state.clear()
                            }
                            .semantics { contentDescription = "Clear canvas" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null, // described by the Box above
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Palette: the 5 fixed ink colors
        AnimatedVisibility(visible = state.isPaletteVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                inkColors.forEach { ink ->
                    val selected = state.color == ink.color
                    Box(
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .clip(CircleShape)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = {
                                    state.color = ink.color
                                    state.isPaletteVisible = false
                                }
                            )
                            .semantics { contentDescription = ink.name },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ink.color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ink layers
// ---------------------------------------------------------------------------

/** Dry ink (finished strokes) + wet ink (live drawing) + optional watermark. */
@Composable
private fun InkSurface(
    strokes: List<Stroke>,
    brush: Brush,
    nextBrush: () -> Brush,
    isDrawing: Boolean,
    watermark: String?,
    onStrokesFinished: (List<Stroke>) -> Unit
) {
    // Created once and reused across recompositions.
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val identityMatrix = remember { Matrix() }

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: finished strokes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            strokes.fastForEach { stroke ->
                nativeCanvas.withSave {
                    canvasStrokeRenderer.draw(
                        canvas = this,
                        stroke = stroke,
                        strokeToScreenTransform = identityMatrix
                    )
                }
            }
        }

        // Layer 2: real-time wet ink. Not composed when drawing is off, so the canvas is read-only.
        if (isDrawing) {
            InProgressStrokes(
                defaultBrush = brush,
                nextBrush = nextBrush,
                onStrokesFinished = onStrokesFinished
            )
        }

        // Watermark: fades out once something has been drawn.
        if (watermark != null) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AnimatedVisibility(
                    visible = strokes.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = watermark,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFFD1D5DB).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrawingCanvasPreview() {
    InkorporateitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DrawingCanvas(
                state = remember { DrawingState().apply { isPaletteVisible = true } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                watermark = "INKORPORATE-IT"
            )
        }
    }
}