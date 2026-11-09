package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The app's "ink" look: thick dark outline + a hard shadow (no blur) under it.
 * Shared, so the Lobby and other screens can use the same buttons.
 */

// Thickness of the dark outline on buttons and fields.
val InkOutlineWidth = 2.5.dp

/*
 * Text color for the yellow (secondaryContainer) buttons: near-black in light AND dark mode.
 * Our yellow is the same in both modes, so its text should be too. The theme's
 * onSecondaryContainer is brown, and onBackground turns light in dark mode, so
 * this picks whichever of background / onBackground is darker.
 * (Better long-term: set onSecondaryContainer in Theme.kt; ask Ismael.)
 */
@Composable
fun inkOnYellow(): Color {
    val colors = MaterialTheme.colorScheme
    // luminance(): 0 = black, 1 = white
    return if (colors.background.luminance() < colors.onBackground.luminance()) {
        colors.background
    } else {
        colors.onBackground
    }
}

/*
 * Hard shadow: paints a copy of the shape, moved down by `offset`, behind
 * whatever this modifier is on. drawBehind = "draw this first, under the content".
 */
fun Modifier.hardShadow(shape: Shape, color: Color, offset: Dp): Modifier =
    this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this) // shape at this size
        translate(top = offset.toPx()) { drawOutline(outline, color) }   // shifted down
    }

// Press animation timing: the slide takes 80 ms, and each half of a tap's bounce
// waits 90 ms, so a tap runs its action about 0.18 s after the finger lifts.
private const val PressAnimationMillis = 80
private const val PressHoldMillis = 90L

/*
 * A button in the ink style.
 * - containerColor / contentColor: fill and text color when enabled
 * - enabled = false: grey fill, light outline, no shadow (theme colors)
 * - shadow: how far down the hard shadow sits (0.dp = none)
 * - onClick runs about 0.18 s after the tap, once the press bounce has played
 *   (instantly if shadow = 0.dp). Extra taps during the bounce are ignored.
 */
@Composable
fun InkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    shape: Shape = RoundedCornerShape(18.dp),
    shadow: Dp = 5.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Pick enabled or disabled colors from the theme.
    val fill = if (enabled) containerColor else colors.surfaceContainerHigh
    val text = if (enabled) contentColor else colors.outline
    val border = if (enabled) colors.onBackground else colors.outlineVariant
    val hasShadow = enabled && shadow > 0.dp

    // Press animation: while a finger is down, the button slides down onto its shadow.
    // interactionSource tells us about touches; collectIsPressedAsState() turns that
    // into a true/false that updates the screen when it changes.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // A quick tap is too short to see (and the next screen opens right away), so every
    // tap also plays a full bounce: down, wait, up, and only THEN runs onClick.
    val scope = rememberCoroutineScope()                       // runs code with waits in it
    var bouncing by remember { mutableStateOf(false) }         // true = hold the button down
    var clickPending by remember { mutableStateOf(false) }     // true = ignore extra taps

    // How far the button has moved down: 0 normally, the full shadow when pressed.
    // animateDpAsState slides between the two instead of jumping.
    val pressOffset by animateDpAsState(
        targetValue = if ((isPressed || bouncing) && hasShadow) shadow else 0.dp,
        animationSpec = tween(durationMillis = PressAnimationMillis),
        label = "press"
    )

    Surface(
        onClick = {
            if (!hasShadow) {
                onClick() // nothing to animate, so no wait
            } else if (!clickPending) {
                clickPending = true
                scope.launch {
                    bouncing = true
                    delay(PressHoldMillis)   // button goes (or stays) down
                    bouncing = false
                    delay(PressHoldMillis)   // button pops back up
                    clickPending = false
                    onClick()                // now open the next screen, etc.
                }
            }
        },
        enabled = enabled,
        shape = shape,
        color = fill,
        contentColor = text,
        border = BorderStroke(InkOutlineWidth, border),
        interactionSource = interactionSource, // so we hear about presses
        // The shadow goes on the outside, so it's added before Surface draws.
        // The button moves down by pressOffset and the shadow shrinks by the same
        // amount, so the shadow stays put and the button "sinks" into it.
        modifier = if (hasShadow) {
            modifier
                // Lambda form: the animation only re-places the button each frame,
                // instead of rebuilding it (lint's advice). roundToPx(): dp → whole pixels.
                .offset { IntOffset(x = 0, y = pressOffset.roundToPx()) }
                .hardShadow(shape, colors.onBackground, shadow - pressOffset)
        } else {
            modifier
        }
    ) {
        // Button text uses the theme's titleMedium (Sniglet) unless the caller sets a style.
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp) // big enough to tap
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

// --- Previews: default (white), yellow, disabled ---

@Preview(showBackground = true, name = "InkButton, light")
@Composable
fun InkButtonPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InkButton(onClick = {}) { Text("Free Draw") }
            InkButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = inkOnYellow()
            ) { Text("Join game") }
            InkButton(onClick = {}, enabled = false) { Text("Join game") }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "InkButton, dark")
@Composable
fun InkButtonDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            InkButton(onClick = {}, modifier = Modifier.padding(16.dp)) { Text("Free Draw") }
        }
    }
}
