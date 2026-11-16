package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.model.BlobColors
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * A grid of round color swatches for picking the blob's color (issue #118).
 * Two rows of four, so it fits on small phones too.
 *
 * State hoisting: the selector doesn't remember the pick itself. The caller
 * passes in the current color and gets told about a new pick through onPick,
 * then passes the new color back in. That way the screen owns the player's color.
 *
 * NOT CONNECTED YET: where the selector goes (Edit screen? Home?) is undecided,
 * and the mock service changeBlobColor(playerId, newColor) from #118 doesn't exist.
 * When it does, the caller calls it from onPick.
 *
 * - selected: the color that's currently picked (gets a ring around it)
 * - onPick: called with the tapped color
 * - colors: which colors to show; the eight blob colors unless told otherwise
 */
@Composable
fun BlobColorSelector(
    selected: Color,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = BlobColors.all
) {
    // FlowRow puts items in a row and wraps to the next line; at most 4 per line here.
    // selectableGroup() tells screen readers these swatches are one "pick one" group.
    FlowRow(
        modifier = modifier.selectableGroup(),
        maxItemsInEachRow = 4,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        colors.forEachIndexed { index, color ->
            ColorSwatch(
                color = color,
                isSelected = color == selected,
                // Read out by screen readers, e.g. "Blob color 3 of 8".
                label = "Blob color ${index + 1} of ${colors.size}",
                onClick = { onPick(color) }
            )
        }
    }
}

/*
 * One swatch: a 48dp tap area (Android's minimum touch size) with a 36dp
 * colored circle inside. The picked one gets a dark ring around it.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    // Ring only on the picked swatch. Color.Transparent = invisible ring.
    val ringColor = if (isSelected) ink else Color.Transparent

    Box(
        modifier = Modifier
            .size(48.dp)
            // selectable = tappable, and screen readers say "selected" on the picked one.
            // Role.RadioButton: only one swatch can be picked at a time.
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label }
            .border(3.dp, ringColor, CircleShape)
            .padding(6.dp), // gap between the ring and the colored circle
        contentAlignment = Alignment.Center
    ) {
        // The color itself, with the same dark outline as the buttons.
        // The fill is shrunk by half the outline width (padding before background),
        // so its soft edge ends UNDER the outline. Otherwise a thin colored fringe
        // shows just outside the dark outline.
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(InkOutlineWidth, ink, CircleShape) // drawn on top of the fill below
                .padding(InkOutlineWidth / 2)
                .background(color, CircleShape)
        )
    }
}

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

@Preview(showBackground = true, name = "Light, Indigo picked")
@Composable
fun BlobColorSelectorPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface { // theme background behind the swatches
            BlobColorSelector(
                selected = BlobColors.Indigo,
                onPick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark, Teal picked")
@Composable
fun BlobColorSelectorDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            BlobColorSelector(
                selected = BlobColors.Teal,
                onPick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// Tappable: in Android Studio's Interactive mode, or with "Run Preview" on the emulator,
// tapping a swatch moves the ring. This preview keeps the pick in its own state,
// standing in for the screen that will own it later.
@Preview(showBackground = true, name = "Tappable (Interactive mode / Run Preview)")
@Composable
fun BlobColorSelectorTappablePreview() {
    var picked by remember { mutableStateOf(BlobColors.Indigo) }
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            BlobColorSelector(
                selected = picked,
                onPick = { picked = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
