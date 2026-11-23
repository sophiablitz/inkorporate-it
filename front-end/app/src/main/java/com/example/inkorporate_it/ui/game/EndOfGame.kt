package com.example.inkorporate_it.ui.game

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.PodiumPlace
import com.example.inkorporate_it.ui.component.ResultRow
import com.example.inkorporate_it.ui.component.dotGrid
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

// --- Previews at the 5 mockup sizes (dynamicColor = false shows the team's colors) ---

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
