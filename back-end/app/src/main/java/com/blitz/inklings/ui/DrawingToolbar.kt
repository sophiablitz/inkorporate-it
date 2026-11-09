package com.blitz.inklings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes

data class BrushSize(val label: String, val size: Float)

val brushSizes = listOf(
    BrushSize("Fine", 2f),
    BrushSize("Medium", 5f),
    BrushSize("Thick", 10f),
    BrushSize("Bold", 18f),
)

val brushColors = listOf(
    Color.Black,
    Color.Red,
    Color.Blue,
    Color(0xFF009900),   // Green
    Color(0xFFFF6600),   // Orange
    Color(0xFF8800CC),   // Purple
)

data class StockBrushItem(val label: String, val family: BrushFamily)

val stockBrushes = listOf(
    StockBrushItem("Pressure Pen", StockBrushes.pressurePen()),
    StockBrushItem("Marker", StockBrushes.marker()),
    StockBrushItem("Highlighter", StockBrushes.highlighter()),
    StockBrushItem("Dashed Line", StockBrushes.dashedLine()),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingToolbar(
    selectedColor: Color,
    selectedSize: Float,
    selectedBrushFamily: BrushFamily,
    onColorSelected: (Color) -> Unit,
    onSizeSelected: (Float) -> Unit,
    onBrushFamilySelected: (BrushFamily) -> Unit,
    onClear: () -> Unit,
) {
    var brushMenuExpanded by remember { mutableStateOf(false) }
    var colorMenuExpanded by remember { mutableStateOf(false) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Inklings") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        actions = {
            // Brush selection button
            Box {
                TextButton(onClick = { brushMenuExpanded = true }) {
                    Text(stockBrushes.find { it.family == selectedBrushFamily }?.label ?: "Brush")
                }
                DropdownMenu(
                    expanded = brushMenuExpanded,
                    onDismissRequest = { brushMenuExpanded = false }
                ) {
                    stockBrushes.forEach { brushItem ->
                        DropdownMenuItem(
                            text = { Text(brushItem.label) },
                            onClick = {
                                onBrushFamilySelected(brushItem.family)
                                brushMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            // Color chip button
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { colorMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = colorMenuExpanded,
                    onDismissRequest = { colorMenuExpanded = false }
                ) {
                    brushColors.forEach { color ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(colorName(color))
                                }
                            },
                            onClick = {
                                onColorSelected(color)
                                colorMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stroke thickness button
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { sizeMenuExpanded = true }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((selectedSize / 2).coerceIn(1f, 9f).dp)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
                DropdownMenu(
                    expanded = sizeMenuExpanded,
                    onDismissRequest = { sizeMenuExpanded = false }
                ) {
                    brushSizes.forEach { brushSize ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(brushSize.label)
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height((brushSize.size / 2).coerceIn(1f, 9f).dp)
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            },
                            onClick = {
                                onSizeSelected(brushSize.size)
                                sizeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Clear button
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear canvas"
                )
            }
        }
    )
}

private fun colorName(color: Color): String = when (color) {
    Color.Black -> "Black"
    Color.Red -> "Red"
    Color.Blue -> "Blue"
    Color(0xFF009900) -> "Green"
    Color(0xFFFF6600) -> "Orange"
    Color(0xFF8800CC) -> "Purple"
    else -> "Custom"
}
