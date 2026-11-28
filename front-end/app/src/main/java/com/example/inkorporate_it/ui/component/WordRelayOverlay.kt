package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import kotlin.math.ceil

/** Grey card color; fixed (not themed) so the white text is readable in light and dark mode. */
private val CardColor = Color(0xFF424242).copy(alpha = 0.92f)

// ---------------------------------------------------------------------------
// Public overlays
// ---------------------------------------------------------------------------

/**
 * Picks the right overlay for the start of a turn:
 * the drawer sees their word, everyone else sees that the drawer is getting theirs.
 *
 * Drive it from `GameUiState.isMyTurnToDraw`.
 *
 * @param isMyTurnToDraw true for the drawer, false for guessers.
 * @param word the word to draw. Only used (and only shown) when [isMyTurnToDraw] is true.
 * @param drawerName optional name of the drawer, shown to guessers instead of "The drawer".
 */
@Composable
fun TurnIntroOverlay(
    isMyTurnToDraw: Boolean,
    word: String,
    modifier: Modifier = Modifier,
    drawerName: String? = null,
    durationSeconds: Int = 2,
    onFinished: () -> Unit = {}
) {
    if (isMyTurnToDraw) {
        WordRevealOverlay(
            word = word,
            modifier = modifier,
            durationSeconds = durationSeconds,
            onFinished = onFinished
        )
    } else {
        DrawerGettingWordOverlay(
            modifier = modifier,
            drawerName = drawerName,
            durationSeconds = durationSeconds,
            onFinished = onFinished
        )
    }
}

/**
 * For the drawer: tells them which word to draw, with a countdown.
 * Shows as soon as it is composed and calls [onFinished] once when time is up.
 */
@Composable
fun WordRevealOverlay(
    word: String,
    modifier: Modifier = Modifier,
    durationSeconds: Int = 2,
    onFinished: () -> Unit = {}
) {
    CountdownCard(
        title = "Your word to draw is",
        headline = word,
        modifier = modifier,
        durationSeconds = durationSeconds,
        onFinished = onFinished
    )
}

/**
 * For guessers (isMyTurnToDraw == false): says the drawer is getting their word,
 * with the same countdown. The word itself is never passed in, so it can't leak.
 *
 * @param drawerName shown instead of "The drawer" when you know who is drawing.
 */
@Composable
fun DrawerGettingWordOverlay(
    modifier: Modifier = Modifier,
    drawerName: String? = null,
    durationSeconds: Int = 2,
    onFinished: () -> Unit = {}
) {
    CountdownCard(
        title = "${drawerName ?: "The drawer"} is getting their word",
        headline = null,
        modifier = modifier,
        durationSeconds = durationSeconds,
        onFinished = onFinished
    )
}

// ---------------------------------------------------------------------------
// Shared card
// ---------------------------------------------------------------------------

/**
 * Smaller rounded grey card with a countdown ring. Sits in the middle of a full-size,
 * invisible layer that swallows taps. While it shows, also pass `isDrawing = false` to
 * DrawingCanvas so no ink can be started underneath.
 */
@Composable
private fun CountdownCard(
    title: String,
    headline: String?,
    modifier: Modifier,
    durationSeconds: Int,
    onFinished: () -> Unit
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    // 1f = full time left, 0f = time is up. Drives both the ring and the number.
    val progress = remember { Animatable(1f) }

    LaunchedEffect(title, headline, durationSeconds) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = durationSeconds * 1000, easing = LinearEasing)
        )
        currentOnFinished()
    }

    // 2 -> 1 for a 2 second overlay; never shows 0.
    val secondsLeft = ceil(progress.value * durationSeconds).toInt().coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Invisible full-size layer: swallows taps so nothing underneath reacts.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(28.dp))
                .background(CardColor)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = buildString {
                        append(title)
                        if (headline != null) append(" $headline")
                        append(". $secondsLeft seconds left.")
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            if (headline != null) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Countdown ring with the seconds left in the middle.
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val inset = strokeWidth / 2
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)

                    // Track
                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    // Time left
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = secondsLeft.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Drawer - Light", showBackground = true)
@Preview(name = "Drawer - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WordRevealOverlayPreview() {
    InkorporateitTheme {
        TurnIntroOverlay(isMyTurnToDraw = true, word = "tree", durationSeconds = 60)
    }
}

@Preview(name = "Guesser - Light", showBackground = true)
@Preview(name = "Guesser - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrawerGettingWordOverlayPreview() {
    InkorporateitTheme {
        TurnIntroOverlay(isMyTurnToDraw = false, word = "", durationSeconds = 60)
    }
}