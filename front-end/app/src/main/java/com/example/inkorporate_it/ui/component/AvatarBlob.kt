package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.R
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import com.example.inkorporate_it.ui.theme.onSurfaceLight
import com.example.inkorporate_it.ui.theme.surfaceContainerLowestLight

/*
 * A player's ink blob. Shared: Home shows it big, and the Lobby can show it small.
 *
 * The blob is drawn on a Canvas from the design's 200 x 200 drawing. Every number
 * below is in those design units; scale() stretches them to the real size, so the
 * blob looks the same at 220dp on Home and at 64dp in the Lobby.
 * The face is the same for everyone; only avatar.inkColor changes.
 * Hats and eye styles (hatId, eyesId) are still placeholders and aren't drawn.
 *
 * - size: how big the blob is (width and height)
 * - onEdit: if given, an "Edit" button appears at the bottom right.
 *   Leave it out (null) where editing isn't allowed, like the Lobby.
 */
@Composable
fun AvatarBlob(
    avatar: Avatar,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onEdit: (() -> Unit)? = null
) {
    // Read theme colors here: the Canvas drawing code below can't call MaterialTheme.
    val outline = MaterialTheme.colorScheme.onBackground

    // Box stacks its children on top of each other: the blob, then the Edit button.
    Box(modifier = modifier.size(size)) {
        // semantics: a Canvas is silent to screen readers, so give it a label to read out.
        Canvas(modifier = Modifier.fillMaxSize().semantics { contentDescription = "Player avatar" }) {
            // How many pixels one design unit is. this.size is the Canvas size in pixels.
            val unit = this.size.width / DESIGN_SIZE
            // Draw in design units from the top-left corner (pivot = Offset.Zero).
            scale(scale = unit, pivot = Offset.Zero) {
                drawBlob(inkColor = avatar.inkColor, outline = outline)
            }
        }

        // Only show the Edit button when someone passed an onEdit function.
        if (onEdit != null) {
            // A small pill in the theme's secondaryContainer color (yellow in our theme).
            InkButton(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = inkOnYellow(), // near-black on yellow, in light and dark mode
                shape = CircleShape,
                shadow = 3.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null, // the "Edit" text already says what it does
                    modifier = Modifier.size(18.dp)
                )
                Text("Edit")
            }
        }
    }
}

// The design is drawn in a 200 x 200 box.
private const val DESIGN_SIZE = 200f

// The blob's outline as SVG path text, parsed once into a Path that Canvas can draw.
// M = move to, C = curve to, Z = close the shape.
private val BlobPath: Path = PathParser()
    .parsePathString("M100 20 C150 10 190 50 185 100 C180 150 150 185 100 185 C50 185 15 150 18 100 C20 50 50 28 100 20Z")
    .toPath()

// The smile. Q = a curve with one bend point (100, 146).
private val MouthPath: Path = PathParser()
    .parsePathString("M78 128 Q100 146 122 128")
    .toPath()

// The face uses the LIGHT theme's white and ink on purpose: eyes stay white with
// dark pupils in dark mode too, like a drawing. (The theme's onBackground turns
// light in dark mode, which would make light pupils on white eyes.)
private val EyeWhite = surfaceContainerLowestLight
private val FaceInk = onSurfaceLight

/*
 * Draws the blob in design units (0 to 200). Called inside scale(), so
 * all positions and line widths grow and shrink with the blob's size.
 */
private fun DrawScope.drawBlob(inkColor: Color, outline: Color) {
    // 1. Shadow: the same shape, 7 units lower, filled with the outline color.
    translate(top = 7f) {
        drawPath(BlobPath, color = outline)
    }

    // 2. The blob: filled with the player's ink color, then a 4-unit outline on top.
    drawPath(BlobPath, color = inkColor)
    drawPath(BlobPath, color = outline, style = Stroke(width = 4f))

    // 3. Eyes: white circles (radius 16) with a 3-unit outline.
    for (eyeCenter in listOf(Offset(72f, 92f), Offset(128f, 92f))) {
        drawCircle(EyeWhite, radius = 16f, center = eyeCenter)
        drawCircle(FaceInk, radius = 16f, center = eyeCenter, style = Stroke(width = 3f))
    }

    // 4. Pupils: dark circles (radius 7), a little right of and below each eye's center.
    drawCircle(FaceInk, radius = 7f, center = Offset(76f, 95f))
    drawCircle(FaceInk, radius = 7f, center = Offset(132f, 95f))

    // 5. Mouth: a 4-unit line with round ends, not filled in.
    drawPath(MouthPath, color = FaceInk, style = Stroke(width = 4f, cap = StrokeCap.Round))
}

// dynamicColor = false so the preview shows the team's colors, not wallpaper colors.
@Preview(showBackground = true, name = "Light, with Edit")
@Composable
fun AvatarBlobPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), onEdit = {})
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark, with Edit"
)
@Composable
fun AvatarBlobDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), onEdit = {})
    }
}

// Lobby size: the blob still looks right when it's small.
@Preview(showBackground = true, name = "Small (Lobby), no Edit")
@Composable
fun AvatarBlobSmallPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), size = 64.dp)
    }
}
