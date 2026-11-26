package com.example.inkorporate_it.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.example.inkorporate_it.ui.component.AvatarBlob
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.ResultRow
import com.example.inkorporate_it.ui.component.dotGrid
import com.example.inkorporate_it.ui.component.hardShadow
import com.example.inkorporate_it.ui.component.inkOnYellow
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * The last screen of a game: the final leaderboard (issue #132).
 *
 * Top to bottom, the height is split by weight (no fixed sizes, no scrolling):
 *   12%  title: "Game over!" and "{winner} wins!"
 *   34%  podium: places 2, 1, 3
 *   32%  list: places 4 to 8
 *   22%  buttons (Play again / Leave come in issue #133; for now Sophia's stub button)
 *
 * NOT DESIGNED YET (ask the team, don't add rules here without them):
 * - Ties: players with the same score keep their list order and still get different places.
 * - Fewer than 8 players: missing podium places stay empty, the list just has fewer rows.
 * - This phone's player on the podium gets no "You" pill (only rows 4 to 8 have it).
 *
 * - currentPlayerName: this phone's name, to highlight its row. Matched by name because
 *   players have no id yet, so two players with the same name both get highlighted.
 */
@Composable
fun EndOfGameScreen(
    state: GameUiState,
    onCloseRoom: () -> Unit,
    onGoToLobby: () -> Unit,
    modifier: Modifier = Modifier,
    currentPlayerName: String = "",
    /* STUB: placeholder -- remove once connected*/
    onStubPhase: (phase: GamePhase, isMyTurnToDraw: Boolean) -> Unit
) {
    // Highest score first. sortedByDescending makes a new sorted list; state.players is unchanged.
    val ranked = state.players.sortedByDescending { it.score }
    val podium = ranked.take(3)               // places 1 to 3
    val others = ranked.drop(3).take(5)       // places 4 to 8 (max 8 players)

    // The phone's whole window size, in dp. Text is a bit smaller on short phones
    // and a bit bigger on tall ones; the content is narrower on the unfolded Fold.
    val windowSize = LocalWindowInfo.current.containerSize
    val (windowWidth, windowHeight) = with(LocalDensity.current) {
        windowSize.width.toDp() to windowSize.height.toDp()
    }
    val textScale = endOfGameTextScale(windowHeight)
    val maxContentWidth = if (windowWidth >= 600.dp) 440.dp else 480.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .dotGrid(MaterialTheme.colorScheme.surfaceVariant) // same dots as Home
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxContentWidth) // not wider than this on big screens...
                .fillMaxWidth(),                // ...but as wide as it can be up to that
        ) {
            // weight(12f) etc.: each region gets its share of the height (12 of 100 parts).
            Title(
                winnerName = podium.firstOrNull()?.name,
                textScale = textScale,
                modifier = Modifier
                    .weight(12f)
                    .fillMaxWidth(),
            )
            Podium(
                players = podium,
                textScale = textScale,
                modifier = Modifier
                    .weight(34f)
                    .fillMaxWidth(),
            )
            OtherPlayers(
                players = others,
                currentPlayerName = currentPlayerName,
                textScale = textScale,
                modifier = Modifier
                    .weight(32f)
                    .fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .weight(22f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // STUB (Sophia's): lets the team walk back to the lobby until the real
                // Play again / Leave buttons (issue #133) and the server exist.
                InkButton(onClick = { onStubPhase(GamePhase.Waiting, false) }) {
                    Text("Back to the lobby")
                }
            }
        }
    }
}

/** Text size multiplier for the End of Game screen, from the window's height. */
internal fun endOfGameTextScale(windowHeight: Dp): Float = when {
    windowHeight <= 640.dp -> 0.9f
    windowHeight >= 900.dp -> 1.05f
    else -> 1f
}

/**
 * Height of one row in places 4 to 8, from the height of their region:
 * 5 rows, 4 gaps of 8dp (32dp), 3dp for the last row's shadow and 12dp of air.
 * Kept between 30dp (still tappable-looking) and 46dp (not too chunky).
 */
internal fun resultRowHeight(regionHeight: Dp): Dp =
    ((regionHeight - 12.dp - 3.dp - 32.dp) / 5).coerceIn(30.dp, 46.dp)

/* "Game over!" and, under it, who won. */
@Composable
private fun Title(winnerName: String?, textScale: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Game over!",
            style = semiBold(38.sp * textScale).copy(letterSpacing = (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        // ?.let runs the block only if there is a winner (the list could be empty).
        winnerName?.let { name ->
            Text(
                text = "$name wins!",
                style = semiBold(22.sp * textScale),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/*
 * Places 2, 1, 3 side by side, sitting on the bottom of their region.
 * BoxWithConstraints tells us how much space we got (maxWidth, maxHeight),
 * so the blobs and blocks can grow or shrink with the screen.
 */
@Composable
private fun Podium(players: List<GamePlayer>, textScale: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val h = maxHeight - 16.dp                    // usable height
        val columnWidth = (maxWidth - 20.dp) / 3     // 3 columns, two 10dp gaps

        // Block heights: 1st is tallest.
        val firstBlock = h * 0.32f
        val secondBlock = h * 0.23f
        val thirdBlock = h * 0.17f

        // 1st place's blob gets whatever height is left after the crown, the gaps,
        // the name and score, and its block. Never under 40dp, never wider than its column.
        val firstBlob = (h - 26.dp * textScale - 4.dp - 6.dp - 40.dp * textScale - 6.dp - firstBlock - 4.dp)
            .coerceAtLeast(40.dp)
            .coerceAtMost(columnWidth * 0.86f)
        val otherBlob = firstBlob * 0.8f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp), // room for the blocks' shadows
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Left to right: 2nd, 1st, 3rd. getOrNull gives null if there's no such player.
            listOf(
                Triple(2, otherBlob, secondBlock),
                Triple(1, firstBlob, firstBlock),
                Triple(3, otherBlob, thirdBlock),
            ).forEach { (place, blobSize, blockHeight) ->
                val player = players.getOrNull(place - 1)
                if (player != null) {
                    PodiumPlace(
                        name = player.name,
                        score = player.score,
                        avatar = player.avatar,
                        place = place,
                        blobSize = blobSize,
                        blockHeight = blockHeight,
                        textScale = textScale,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f)) // keeps the other places where they are
                }
            }
        }
    }
}

/* Places 4 to 8, centered in their region. */
@Composable
private fun OtherPlayers(
    players: List<GamePlayer>,
    currentPlayerName: String,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val rowHeight = resultRowHeight(maxHeight)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // forEachIndexed also gives the position: index 0 is 4th place.
            players.forEachIndexed { index, player ->
                ResultRow(
                    rank = index + 4,
                    name = player.name,
                    score = player.score,
                    avatar = player.avatar,
                    rowHeight = rowHeight,
                    isMe = player.name == currentPlayerName,
                    textScale = textScale,
                )
            }
        }
    }
}

/*
 * One place on the podium, top to bottom:
 * crown (1st place only), blob, name, score, and a block with the place number.
 * Private: only this screen uses it. (ResultRow is in ui/component/ because
 * End of Round may reuse it.)
 *
 * Podium() works out blobSize and blockHeight from the space it has,
 * so this only draws what it's told.
 *
 * - place: 1, 2 or 3. Picks the block color, the text sizes and the crown.
 * - textScale: makes all text a bit smaller or bigger (0.9 on short phones, 1.05 on tall ones).
 */
@Composable
private fun PodiumPlace(
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
        1 -> colors.secondaryContainer // gold: the theme's yellow
        2 -> PodiumSilver
        else -> PodiumBronze
    }
    // All three blocks are light in both modes, so the number stays near-black, also in dark mode.
    val numberColor = inkOnYellow()
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
 * Medal colors for the 2nd and 3rd place blocks (1st uses the theme's yellow as gold).
 * The theme has no silver or bronze, so they're fixed here, like the blob colors:
 * the same in light and dark mode, just like the yellow.
 */
private val PodiumSilver = Color(0xFFCDD3DC)
private val PodiumBronze = Color(0xFFE0A86E)

/*
 * The crown over 1st place, drawn from two SVG paths (no icon library).
 * Paths from the Lucide icon set ("crown"), ISC license: assets/licenses/Lucide-ISC.txt.
 * The paths are drawn in a 24 x 24 space, then scaled to the crown's real size.
 */
private const val CrownShapePath =
    "M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 " +
        ".798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 " +
        ".798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z"
private const val CrownBasePath = "M5 21h14"

@Composable
private fun Crown(modifier: Modifier = Modifier) {
    // Turn the path text into drawable shapes once, the first time the crown is drawn.
    // remember keeps them for later redraws. (Building them when the file loads would
    // break the unit tests: they run without Android, and a Path needs Android.)
    val crownShape = remember { PathParser().parsePathString(CrownShapePath).toPath() }
    val crownBase = remember { PathParser().parsePathString(CrownBasePath).toPath() }
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

/* The theme's Sniglet at a given size, with fake bold (SemiBold, no ExtraBold on this screen). */
@Composable
private fun semiBold(size: TextUnit): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = size,
        lineHeight = 1.1.em,
        fontWeight = FontWeight.SemiBold,
    )

/*
 * The 8 players from the mockup, for previews and for the GameViewModel stub.
 * Jerry (4th place) is "you" in the previews.
 * Colors: same values as BlobColors in PR #169. Swap to BlobColors once it merges.
 */
val previewFinalPlayers = listOf(
    GamePlayer(name = "Mia", score = 1240, avatar = Avatar(inkColor = Color(0xFFF06BA8))),   // pink
    GamePlayer(name = "Sam", score = 980, avatar = Avatar(inkColor = Color(0xFFE5484D))),    // red
    GamePlayer(name = "Leo", score = 870, avatar = Avatar(inkColor = Color(0xFF3DBE7A))),    // green
    GamePlayer(name = "Jerry", score = 640, avatar = Avatar(inkColor = Color(0xFF3B4FE0))),  // indigo
    GamePlayer(name = "Ava", score = 520, avatar = Avatar(inkColor = Color(0xFFF5883A))),    // orange
    GamePlayer(name = "Noah", score = 410, avatar = Avatar(inkColor = Color(0xFF2BB5C9))),   // teal
    GamePlayer(name = "Zoe", score = 300, avatar = Avatar(inkColor = Color(0xFF8A4FE0))),    // violet
    GamePlayer(name = "Eli", score = 150, avatar = Avatar(inkColor = Color(0xFFFCD365))),    // yellow
)

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

// Just the podium places, side by side: Sam (2nd), Mia (1st), Leo (3rd).
@Preview(showBackground = true, name = "Podium places 2, 1, 3")
@Composable
private fun PodiumPlacePreview() {
    InkorporateitTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val (mia, sam, leo) = previewFinalPlayers // the first three, highest score first
            PodiumPlace(sam.name, sam.score, sam.avatar, place = 2,
                blobSize = 64.dp, blockHeight = 50.dp, modifier = Modifier.width(96.dp))
            PodiumPlace(mia.name, mia.score, mia.avatar, place = 1,
                blobSize = 80.dp, blockHeight = 70.dp, modifier = Modifier.width(96.dp))
            PodiumPlace(leo.name, leo.score, leo.avatar, place = 3,
                blobSize = 64.dp, blockHeight = 38.dp, modifier = Modifier.width(96.dp))
        }
    }
}

// The whole screen at the 5 mockup sizes, plus dark mode.

@Composable
private fun EndOfGamePreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            EndOfGameScreen(
                state = GameUiState(phase = GamePhase.GameOver, players = previewFinalPlayers),
                onCloseRoom = {},
                onGoToLobby = {},
                currentPlayerName = "Jerry",
                onStubPhase = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "End of game, 390x844")
@Composable
fun EndOfGameScreenPreview() = EndOfGamePreview()

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "End of game, small 360x640")
@Composable
fun EndOfGameScreenSmallPreview() = EndOfGamePreview()

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "End of game, Pixel 8 412x915")
@Composable
fun EndOfGameScreenPixel8Preview() = EndOfGamePreview()

@Preview(showBackground = true, widthDp = 411, heightDp = 797, name = "End of game, Fold folded 411x797")
@Composable
fun EndOfGameScreenFoldFoldedPreview() = EndOfGamePreview()

@Preview(showBackground = true, widthDp = 841, heightDp = 701, name = "End of game, Fold unfolded 841x701")
@Composable
fun EndOfGameScreenFoldUnfoldedPreview() = EndOfGamePreview()

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "End of game, dark 390x844",
)
@Composable
fun EndOfGameScreenDarkPreview() = EndOfGamePreview()
