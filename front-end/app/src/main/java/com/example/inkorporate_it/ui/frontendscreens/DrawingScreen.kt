package com.example.inkorporate_it.ui.frontendscreens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


/**
 * Everything the drawing screen needs to render.
 * Held in the ViewModel so it survives rotation and other configuration changes.
 */
data class DrawingUiState(
    // Finished (dry) strokes. Stroke objects are immutable, so a plain list is safe to share.
    val strokes: List<Stroke> = emptyList(),

    // null means the user hasn't picked a color yet.
    val selectedColor: Color? = null,

    // Marker size options used by the UI: 6f (small), 12f (medium), 20f (large).
    val selectedSize: Float = 12f,

    // Whether the row of 5 color choices is expanded under the toolbar.
    val isPaletteVisible: Boolean = false,
)

class DrawingViewModel : ViewModel() {

    // Private mutable state: only the ViewModel can change it.
    private val _uiState = MutableStateFlow(DrawingUiState())

    // Read-only state exposed to the UI, which collects it and redraws on every change.
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    /** Called when the wet-ink layer finishes strokes (finger or stylus lifted). Saves them. */
    fun onStrokesFinished(newStrokes: List<Stroke>) {
        _uiState.update { it.copy(strokes = it.strokes + newStrokes) }
    }

    /** Called by the trash button. Removes every stroke from the canvas. */
    fun onClearCanvas() {
        _uiState.update { it.copy(strokes = emptyList()) }
    }

    /** Called when the user taps one of the 5 colors. Saves the choice and closes the palette. */
    fun onColorSelected(color: Color) {
        _uiState.update { it.copy(selectedColor = color, isPaletteVisible = false) }
    }

    /** Called when the user taps one of the size dots. */
    fun onSizeSelected(size: Float) {
        _uiState.update { it.copy(selectedSize = size) }
    }

    /** Called when the user taps the color circle button. Opens or closes the palette. */
    fun onTogglePalette() {
        _uiState.update { it.copy(isPaletteVisible = !it.isPaletteVisible) }
    }
}




@Composable
fun DrawingScreen(
    modifier: Modifier = Modifier,
    titleText: String = "Drawing Screen",
    //onBackClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )


    }
}

@Preview(showBackground = true)
@Composable
fun DrawingScreenPreview() {
    InkorporateitTheme {
        DrawingScreen()
    }
}