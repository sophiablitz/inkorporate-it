package com.example.inkorporate_it.ui.component


import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

// ---------------------------------------------------------------------------
// Data (self-contained: no dependency on GameUiState)
// ---------------------------------------------------------------------------

/**
 * One player as this row needs them. Deliberately separate from the game's own player class,
 * so the component works on any screen. Later, convert game players into these:
 * `players.map { AvatarPlayer(it.name, it.isDrawing) }`.
 *
 * @param color the circle color; null uses the next color from the default palette.
 */
data class AvatarPlayer(
    val name: String,
    val isDrawing: Boolean = false,
    val color: Color? = null,
)

/** Fake players for trying the component without any game state. */
val fakeAvatarPlayers = listOf(
    AvatarPlayer(name = "Alejandro", isDrawing = true),
    AvatarPlayer(name = "Jerry"),
    AvatarPlayer(name = "Jorge"),
    AvatarPlayer(name = "Ismael"),
    AvatarPlayer(name = "Garrick"),
)

/**
 * Circle colors that tell players apart. Fixed (not themed) so each player keeps the same
 * color in light and dark mode. Player #1 gets the first color, #2 the second, and so on.
 * All are dark enough for white text.
 */
private val PlayerColors = listOf(
    Color(0xFF3B4FE0), // blue
    Color(0xFFD9482F), // orange-red
    Color(0xFF1E9E6A), // green
    Color(0xFFA626A2), // purple
    Color(0xFFC98200), // gold
    Color(0xFF0E8CA8), // teal
    Color(0xFFD6336C), // pink
    Color(0xFF5B6270), // grey
)

/** Default color for a player: stable per list position, repeating after 8 players. */
fun defaultPlayerColor(index: Int): Color = PlayerColors[index % PlayerColors.size]

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

/**
 * A row of circular player avatars, each filled with its own color.
 *
 * - The color tells players apart.
 * - The player with `isDrawing = true` gets a pencil badge.
 * - The row scrolls sideways if there are more players than fit.
 *
 * @param avatarSize diameter of each circle.
 * @param showInitial show the player's first letter in the circle.
 */
@Composable
fun PlayerAvatarRow(
    players: List<AvatarPlayer>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    showInitial: Boolean = true
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsIndexed(players, key = { index, player -> "$index-${player.name}" }) { index, player ->
            PlayerAvatarItem(
                player = player,
                color = player.color ?: defaultPlayerColor(index),
                avatarSize = avatarSize,
                showInitial = showInitial
            )
        }
    }
}

@Composable
private fun PlayerAvatarItem(
    player: AvatarPlayer,
    color: Color,
    avatarSize: Dp,
    showInitial: Boolean
) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = if (player.isDrawing) "${player.name}, drawing" else player.name
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(avatarSize)) {

            // The colored circle
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (showInitial) {
                    Text(
                        text = player.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Pencil badge on whoever is drawing
            if (player.isDrawing) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null, // described by the item above
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = player.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (player.isDrawing) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = avatarSize + 12.dp)
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlayerAvatarRowPreview() {
    InkorporateitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlayerAvatarRow(
                players = fakeAvatarPlayers,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}