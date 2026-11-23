package com.example.inkorporate_it.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inkorporate_it.ui.component.DrawerGettingWordOverlay
import com.example.inkorporate_it.ui.component.DrawingCanvas
import com.example.inkorporate_it.ui.component.DrawingState
import com.example.inkorporate_it.ui.component.TurnIntroOverlay
import com.example.inkorporate_it.ui.component.WordRevealOverlay

@Composable
fun DrawingScreen(
    state: GameUiState,
    onSendStroke: (Stroke) -> Unit,
    onClearCanvas: () -> Unit,
    modifier: Modifier = Modifier,
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {

    // TEMPORARY for testing: lost on rotation. Move this into GameViewModel later.
    val drawingState = remember { DrawingState() }
    var showWord by rememberSaveable { mutableStateOf(false) }
    var showIntro by rememberSaveable { mutableStateOf(true) }
    var Draw by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // your screen content first
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            DrawingCanvas(
                state = drawingState,
                isDrawing = Draw && !showIntro,
                modifier = Modifier.weight(1f)
            )
        }

        // overlay last, so it sits on top
        if (showIntro) {
            if (Draw) {
                WordRevealOverlay(
                    word = state.wordHint,
                    onFinished = { showIntro = false }
                )
            } else {
                DrawerGettingWordOverlay(
                    drawerName = state.players.firstOrNull { it.isDrawing }?.name,
                    onFinished = { showIntro = false }
                )
            }
        }
    }

    /*GamePlaceholderScreen(
        title = "Drawing",
        note = "Your turn to draw. Not built yet.",
        modifier = modifier,
        options = listOf(
            PlaceholderOption("End the round") { onStubPhase(GamePhase.RoundOver, false) },
        ),
    ) */
}

@Preview(showBackground = true)
@Composable
fun DrawingScreenPreview() {
    // uncomment the line below when stub has been removed
//    InkorporateitTheme { DrawingScreen (previewGuesserState, onSendStroke = {}, onClearCanvas = {} ) }
}