package com.example.inkorporate_it.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * One place on the End of Game podium, top to bottom:
 * crown (1st place only), blob, name, score, and a block with the place number.
 *
 * The screen works out blobSize and blockHeight from the space it has,
 * so this only draws what it's told.
 *
 * - place: 1, 2 or 3. Picks the block color, the text sizes and the crown.
 * - textScale: makes all text a bit smaller or bigger (0.9 on short phones, 1.05 on tall ones).
 */
@Composable
fun PodiumPlace(
    name: String,
    score: Int,
    avatar: Avatar,
    place: Int,
    blobSize: Dp,
    blockHeight: Dp,
    modifier: Modifier = Modifier,
    textScale: Float = 1f,
) {
    val colors = MaterialTheme.colorScheme
    val isWinner = place == 1

    // `when` picks one value for each case, like a switch statement.
    val blockColor = when (place) {
        1 -> colors.secondaryContainer      // yellow
        2 -> colors.surfaceContainerLowest  // white
        else -> colors.surfaceContainerHigh // light lilac
    }
    // Text on the yellow block stays near-black, also in dark mode.
    val numberColor = if (place == 1) inkOnYellow() else colors.onSurface
    val numberSize = when (place) {
        1 -> 40.sp
        2 -> 32.sp
        else -> 28.sp
    } * textScale
    val nameSize = (if (isWinner) 19.sp else 17.sp) * textScale
    val scoreSize = (if (isWinner) 16.sp else 15.sp) * textScale
    val blockShape = RoundedCornerShape(18.dp)

    // mergeDescendants: a screen reader reads the whole place as one item ("Mia, 1240, 1").
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isWinner) {
            Crown(Modifier.size(26.dp * textScale))
            Spacer(Modifier.height(4.dp))
        }
        AvatarBlob(avatar = avatar, size = blobSize) // no onEdit, so no Edit button
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = semiBold(nameSize),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis, // long names end in "…"
        )
        Text(text = score.toString(), style = semiBold(scoreSize), color = colors.onSurface)
        Spacer(Modifier.height(6.dp))

        // The block. Order matters: shadow first (behind), then fill, then outline on top.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blockHeight)
                .hardShadow(blockShape, colors.onSurface, offset = 4.dp)
                .background(blockColor, blockShape)
                .border(3.5.dp, colors.onSurface, blockShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = place.toString(), style = semiBold(numberSize), color = numberColor)
        }
    }
}

/*
 * The theme's Sniglet at a given size, with fake bold (SemiBold).
 * Sniglet has no SemiBold file, so Android draws Regular a bit thicker.
 * lineHeight = 1.1.em: each line is 1.1 x the font size tall, so the sizes add up as designed.
 */
@Composable
private fun semiBold(size: TextUnit): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = size,
        lineHeight = 1.1.em,
        fontWeight = FontWeight.SemiBold,
    )

/*
 * The crown over 1st place, drawn from two SVG paths (no icon library).
 * Paths from the Lucide icon set ("crown"), ISC license: assets/licenses/Lucide-ISC.txt.
 * The paths are drawn in a 24 x 24 space, then scaled to the crown's real size.
 */
private val crownShape = PathParser().parsePathString(
    "M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 " +
        ".798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 " +
        ".798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z"
).toPath()
private val crownBase = PathParser().parsePathString("M5 21h14").toPath()

@Composable
private fun Crown(modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.secondaryContainer
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        // 2 units wide in the 24-unit space, with round ends and corners.
        val line = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // scale() stretches everything inside it (lines too) from 24 units to the real size.
        scale(size.width / 24f, pivot = Offset.Zero) {
            drawPath(crownShape, fill)                  // yellow inside
            drawPath(crownShape, outline, style = line) // dark outline
            drawPath(crownBase, outline, style = line)  // the line under the crown
        }
    }
}

// dynamicColor = false so the preview shows the team's colors, not wallpaper colors.
// Colors: same values as BlobColors in PR #169. Swap to BlobColors once it merges.
@Preview(showBackground = true, name = "Podium places 2, 1, 3")
@Composable
fun PodiumPlacePreview() {
    InkorporateitTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            PodiumPlace("Sam", 980, Avatar(inkColor = Color(0xFFE5484D)), place = 2,
                blobSize = 64.dp, blockHeight = 50.dp, modifier = Modifier.width(96.dp))
            PodiumPlace("Mia", 1240, Avatar(inkColor = Color(0xFFF06BA8)), place = 1,
                blobSize = 80.dp, blockHeight = 70.dp, modifier = Modifier.width(96.dp))
            PodiumPlace("Leo", 870, Avatar(inkColor = Color(0xFF3DBE7A)), place = 3,
                blobSize = 64.dp, blockHeight = 38.dp, modifier = Modifier.width(96.dp))
        }
    }
}
