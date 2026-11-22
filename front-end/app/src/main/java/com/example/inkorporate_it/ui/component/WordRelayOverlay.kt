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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Grey scrim; fixed (not themed) so the white text is readable in light and dark mode. */
private val ScrimColor = Color(0xFF424242).copy(alpha = 0.92f)

/**
 * Full-size grey overlay that tells the drawer which word to draw, with a countdown.
 *
 * Put it on top of the screen content (inside a Box). It appears as soon as it is composed,
 * counts down from [durationSeconds], then calls [onFinished] so the caller can remove it.
 * While it is showing it swallows taps, but the drawing canvas should also be switched off
 * with `isDrawing = false` so no ink can be started underneath.
 *
 * @param word the word the user must draw.
 * @param durationSeconds how long the overlay stays (default 2).
 * @param onFinished called once when the countdown reaches zero.
 */
@Composable
fun WordRevealOverlay(
    word: String,
    modifier: Modifier = Modifier,
    durationSeconds: Int = 2,
    onFinished: () -> Unit = {}
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    // 1f = full time left, 0f = time is up. Drives both the ring and the number.
    val progress = remember { Animatable(1f) }

    LaunchedEffect(word, durationSeconds) {
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
            .background(ScrimColor)
            // Swallow taps so nothing underneath reacts while the overlay is up.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Your word to draw is $word. $secondsLeft seconds left."
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your word to draw is",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Text(
                text = word,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

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

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WordRevealOverlayPreview() {
    InkorporateitTheme {
        WordRevealOverlay(word = "tree", durationSeconds = 60)
    }
}