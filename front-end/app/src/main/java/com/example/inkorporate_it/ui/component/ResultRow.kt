package com.example.inkorporate_it.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * One player in the End of Game list (places 4 to 8):
 * place number, small blob, name, a "You" pill if it's this phone's player, and the score.
 *
 * - rowHeight: the screen works it out from the space it has (30-46dp).
 * - isMe: true = adds the "You" pill. The row stays white: yellow means 1st place on this screen.
 * - textScale: makes all text a bit smaller or bigger (0.9 on short phones, 1.05 on tall ones).
 */
@Composable
fun ResultRow(
    rank: Int,
    name: String,
    score: Int,
    avatar: Avatar,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    isMe: Boolean = false,
    textScale: Float = 1f,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(rowHeight * 0.36f) // rounder ends on taller rows
    val fill = colors.surfaceContainerLowest // white (near-black in dark mode)
    val textColor = colors.onSurface
    val textStyle = semiBold(16.sp * textScale)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .semantics(mergeDescendants = true) {} // a screen reader reads the row as one item
            // Shadow first (behind), then fill, then outline on top.
            .hardShadow(shape, colors.onSurface, offset = 3.dp)
            .background(fill, shape)
            .border(3.dp, colors.onSurface, shape)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(20.dp),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
        AvatarBlob(avatar = avatar, size = rowHeight - 12.dp)

        // Name and pill share the leftover width, so the score always stays at the right end.
        // weight(1f, fill = false): the name takes only the space it needs,
        // so the pill sits right after it instead of at the far right.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f, fill = false),
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // long names end in "…"
            )
            if (isMe) YouPill(textScale)
        }

        Text(text = score.toString(), style = textStyle, color = textColor)
    }
}

/* The small purple "You" label after this phone's name. */
@Composable
private fun YouPill(textScale: Float) {
    Box(
        modifier = Modifier
            .height(20.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "You",
            style = semiBold(12.sp * textScale),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/*
 * The theme's Sniglet at a given size, with fake bold (SemiBold).
 * Sniglet has no SemiBold file, so Android draws Regular a bit thicker.
 */
@Composable
private fun semiBold(size: TextUnit): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = size,
        lineHeight = 1.1.em,
        fontWeight = FontWeight.SemiBold,
    )

// dynamicColor = false so the preview shows the team's colors, not wallpaper colors.
// Colors: same values as BlobColors in PR #169. Swap to BlobColors once it merges.
@Preview(showBackground = true, name = "Result rows")
@Composable
fun ResultRowPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .width(342.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultRow(4, "Jerry", 640, Avatar(inkColor = Color(0xFF3B4FE0)), rowHeight = 46.dp, isMe = true)
            ResultRow(5, "Ava", 520, Avatar(inkColor = Color(0xFFF5883A)), rowHeight = 46.dp)
        }
    }
}
