package com.example.inkorporate_it.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.dotGrid
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/** One button on a placeholder screen: where it goes, and what happens when it's tapped. */
data class PlaceholderOption(val label: String, val onClick: () -> Unit)

/*
 * A stand-in for a game screen that isn't built yet: the Leave button, a title,
 * and one InkButton per place the real screen will be able to go.
 * The buttons let the team walk through the whole game flow before the real screens exist.
 * Replace each use with the real screen when it's made.
 */
@Composable
fun GamePlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    note: String = "Not built yet",
    options: List<PlaceholderOption> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .dotGrid(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            options.forEach { option ->
                InkButton(onClick = option.onClick, modifier = Modifier.widthIn(min = 220.dp)) {
                    Text(option.label)
                }
            }
        }
    }
}

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

@Preview(showBackground = true, name = "Placeholder, light")
@Composable
fun GamePlaceholderScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            GamePlaceholderScreen(
                title = "End of round",
                options = listOf(
                    PlaceholderOption("Next round: I'm drawing") {},
                    PlaceholderOption("Next round: I'm guessing") {},
                    PlaceholderOption("End the game") {},
                ),
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Placeholder, dark")
@Composable
fun GamePlaceholderScreenDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            GamePlaceholderScreen(
                title = "Guessing",
                options = listOf(PlaceholderOption("End the round") {}),
            )
        }
    }
}
