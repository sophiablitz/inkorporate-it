package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * Faint dotted background (like paper), drawn behind the content.
 * Shared, so other screens can use the same background.
 *
 * - color: dot color; screens pass MaterialTheme.colorScheme.surfaceVariant
 * - spacing: distance between dots
 * - radius: dot size
 */
fun Modifier.dotGrid(color: Color, spacing: Dp = 22.dp, radius: Dp = 1.5.dp): Modifier =
    this.drawBehind {
        val step = spacing.toPx() // dp → pixels for drawing
        val r = radius.toPx()
        // Walk across the area row by row and draw one small circle per spot.
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color = color, radius = r, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }

@Preview(showBackground = true, name = "Dot grid, light")
@Composable
fun DotGridPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.size(200.dp).dotGrid(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dot grid, dark")
@Composable
fun DotGridDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.size(200.dp).dotGrid(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
