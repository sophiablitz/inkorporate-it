package com.example.inkorporate_it.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/**
 * Speech bubble displayed above a player's avatar to show their latest guess or status.
 *
 * Once a player has guessed the word correctly, their guess is the answer, so it's
 * hidden on this screen: with [hasGuessed] true the bubble renders nothing.
 */
@Composable
fun GuessChatBubble(
    text: String,
    modifier: Modifier = Modifier,
    hasGuessed: Boolean = false,
    bubbleColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    minWidth: Dp = 44.dp,
    maxWidth: Dp = 160.dp,
    borderWidth: Dp = 1.5.dp,
    tailWidth: Dp = 12.dp,
    tailHeight: Dp = 7.dp,
) {
    // Hide the bubble entirely for a correct guesser (don't reveal the answer).
    if (hasGuessed) return

    Column(
        modifier = modifier
            .wrapContentSize()
            .widthIn(min = minWidth, max = maxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(borderWidth, borderColor),
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .offset(y = -borderWidth)
                .size(width = tailWidth, height = tailHeight),
        ) {
            val w = size.width
            val h = size.height
            val strokePx = borderWidth.toPx()

            val fillPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w * 0.5f, h)
                close()
            }
            drawPath(path = fillPath, color = bubbleColor)

            val borderPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.5f, h)
                lineTo(w, 0f)
            }
            drawPath(
                path = borderPath,
                color = borderColor,
                style = DrawStroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Preview(showBackground = true, name = "Guess Chat Bubble")
@Composable
fun GuessChatBubblePreview() {
    InkorporateitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GuessChatBubble(text = "hi")
            GuessChatBubble(text = "box?")
            GuessChatBubble(text = "maybe a skyscraper")
        }
    }
}