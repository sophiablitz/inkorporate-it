package com.example.inkorporate_it

import android.graphics.Matrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.inkorporate_it.ui.theme.bodyFontFamily

@Composable
fun SingleDrawScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    // Ink State
    val strokes = remember { mutableStateListOf<Stroke>() }
    var pendingStrokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1C1A24)) }
    var selectedSize by remember { mutableFloatStateOf(12f) } // Marker sizes: 6f, 12f, 20f
    var showColorPalette by remember { mutableStateOf(false) }

    // Renderer & Matrix
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val identityMatrix = remember { Matrix() }

    // Create current Jetpack Ink Marker Brush
    val currentBrush = remember(selectedColor, selectedSize) {
        Brush.createWithComposeColor(
            family = StockBrushes.marker(),
            color = selectedColor,
            size = selectedSize,
            epsilon = 0.1f
        )
    }
    val updatedBrush by rememberUpdatedState(currentBrush)

    // Synchronously clear pending handoff strokes once main strokes state updates
    SideEffect {
        if (pendingStrokes.isNotEmpty()) {
            pendingStrokes = if (strokes.isEmpty()) {
                emptyList()
            } else {
                pendingStrokes.filter { pending -> strokes.none { it === pending } }
            }
        }
    }

    val strokesToDraw = remember(strokes, pendingStrokes) {
        if (pendingStrokes.isEmpty()) strokes else strokes + pendingStrokes
    }

    // Watermark visibility: true only when no strokes have been drawn
    val hasDrawn = strokesToDraw.isNotEmpty()

    val colorOptions = listOf(
        Color(0xFF1C1A24), // Black
        Color(0xFF451FCF), // Purple
        Color(0xFFBA1A1A), // Red
        Color(0xFF007A5A), // Green
        Color(0xFF2B6CB0)  // Blue
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F8))
            .padding(16.dp)
    ) {
        // --- 1. Top Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1C1A24))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1C1A24)
                    )
                }
            }

            // Title Header
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Single Draw",
                    fontFamily = bodyFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1A24)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4EE2C0))
                )
            }

            Spacer(modifier = Modifier.size(46.dp))
        }

        // --- 2. Toolbar (Enlarged Buttons & Size Toggle) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1C1A24))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Marker Selector Tool Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF451FCF),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1C1A24))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Marker",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Marker",
                            fontFamily = bodyFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Size Selector (Enlarged Dots: 8dp, 14dp, 20dp)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF2F4F8),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1C1A24))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(6f to 8.dp, 12f to 14.dp, 20f to 20.dp).forEach { (size, dotSize) ->
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedSize == size) Color(0xFF1C1A24) else Color.LightGray
                                    )
                                    .clickable { selectedSize = size }
                            )
                        }
                    }
                }

                // Color Picker Button (Enlarged 42dp)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(2.dp, Color(0xFF1C1A24), CircleShape)
                        .clickable { showColorPalette = !showColorPalette }
                )

                // Trash / Clear Canvas Button (Enlarged 42dp)
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            strokes.clear()
                            pendingStrokes = emptyList()
                        },
                    shape = CircleShape,
                    color = Color(0xFFFFE8E8),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1C1A24))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Canvas",
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Expandable Color Palette
        AnimatedVisibility(visible = showColorPalette) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorOptions.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 3.dp else 1.dp,
                                color = if (selectedColor == color) Color(0xFF451FCF) else Color(0xFF1C1A24),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = color
                                showColorPalette = false
                            }
                    )
                }
            }
        }

        // --- 3. Fit Canvas Surface Perfectly ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF1C1A24))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Layer 1: Dry Ink Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    strokesToDraw.fastForEach { stroke ->
                        nativeCanvas.withSave {
                            canvasStrokeRenderer.draw(
                                canvas = this,
                                stroke = stroke,
                                strokeToScreenTransform = identityMatrix
                            )
                        }
                    }
                }

                // Layer 2: Real-time Wet Ink Capture (Uses updatedBrush seamlessly)
                InProgressStrokes(
                    defaultBrush = currentBrush,
                    nextBrush = { updatedBrush },
                    onStrokesFinished = { newStrokes ->
                        pendingStrokes = pendingStrokes + newStrokes
                        strokes.addAll(newStrokes)
                    }
                )

                // Watermark Logo ("INKORPORATE-IT") - Fades away on draw
                Box(modifier = Modifier.align(Alignment.Center)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !hasDrawn,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "INKORPORATE-IT",
                            fontFamily = bodyFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD1D5DB).copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Overlays inside Canvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pressure 100%",
                        fontFamily = bodyFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "FREE DRAW AREA",
                    fontFamily = bodyFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

                Text(
                    text = "Tap to draw",
                    fontFamily = bodyFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SingleDrawScreenPreview() {
    InkorporateitTheme {
        SingleDrawScreen()
    }
}