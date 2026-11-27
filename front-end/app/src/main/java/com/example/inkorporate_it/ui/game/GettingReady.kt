package com.example.inkorporate_it.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.inkOnYellow
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/**
 * The start of a turn, before the timer runs (#160). The drawer sees their word and a
 * Start button; everyone else sees who's getting ready. The turn also starts by itself
 * after state.secondsToStart, so nobody has to wait on a drawer who walked away.
 *
 * A placeholder layout: the design team can restyle it freely.
 */
@Composable
fun GettingReadyScreen(
    state: GameUiState,
    onStartDrawing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Round ${state.round} of ${state.totalRounds}",
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.isMyTurnToDraw) {
            Text("Your word is", style = MaterialTheme.typography.titleMedium)
            Text(state.wordHint, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
            InkButton(
                onClick = onStartDrawing,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = inkOnYellow(),
            ) { Text("Start", style = MaterialTheme.typography.headlineSmall) }
            Text(
                text = "Starts by itself in ${state.secondsLeft} s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "${state.drawerName} is getting ready to draw",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, name = "Getting ready, drawer")
@Composable
fun GettingReadyDrawerPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            GettingReadyScreen(
                state = GameUiState(
                    phase = GamePhase.Drawing, isGettingReady = true, isMyTurnToDraw = true,
                    wordHint = "barn", secondsToStart = 10, secondsLeft = 7, round = 1, totalRounds = 3,
                ),
                onStartDrawing = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Getting ready, guesser, dark")
@Composable
fun GettingReadyGuesserPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            GettingReadyScreen(
                state = GameUiState(
                    phase = GamePhase.Drawing, isGettingReady = true, drawerName = "Jerry", round = 2, totalRounds = 3,
                ),
                onStartDrawing = {},
            )
        }
    }
}
