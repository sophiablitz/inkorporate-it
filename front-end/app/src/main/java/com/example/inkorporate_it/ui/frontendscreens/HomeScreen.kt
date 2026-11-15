package com.example.inkorporate_it.ui.frontendscreens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkorporate_it.R
import com.example.inkorporate_it.model.Player
import com.example.inkorporate_it.model.cleanName
import com.example.inkorporate_it.model.filterJoinCode
import com.example.inkorporate_it.model.isJoinCodeComplete
import com.example.inkorporate_it.ui.component.AvatarBlob
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.InkTextField
import com.example.inkorporate_it.ui.component.dotGrid
import com.example.inkorporate_it.ui.component.inkOnYellow
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * The Home screen. Layout follows Jorge's barebones wireframe, top to bottom:
 * settings, title, avatar + Edit, name, Create game, join with a code, Free Draw.
 * The look (outlines, hard shadows, colors) follows the colorful design.
 *
 * State hoisting: Home doesn't own the player or decide where buttons go.
 * The caller passes the player in and gets told what happened through the
 * on... functions. Navigation is UNDECIDED, so the caller wires these later.
 *
 * - player / onPlayerChange: the current player, and how to save a new name
 * - joinError: text to show under the code field when joining failed (null = no error)
 * - onSettings, onEditAvatar, onCreateGame, onFreeDraw: button taps
 * - onJoinGame: Join tapped with a complete 6-digit code
 */
@Composable
fun HomeScreen(
    player: Player,
    onPlayerChange: (Player) -> Unit,
    onSettings: () -> Unit,
    onEditAvatar: () -> Unit,
    onCreateGame: () -> Unit,
    onJoinGame: (code: String) -> Unit,
    onFreeDraw: () -> Unit,
    modifier: Modifier = Modifier,
    joinError: String? = null
) {
    // The code being typed. Only Home needs it, so Home keeps it.
    // rememberSaveable keeps it when the phone rotates. It also stays after a failed join.
    var joinCode by rememberSaveable { mutableStateOf("") }

    // Lets the buttons take focus away from the name field. NameField saves the
    // name when it loses focus, so clearing focus first means the name is saved
    // before we leave Home, whatever the keyboard or field was doing.
    val focusManager = LocalFocusManager.current

    // Same theme, but every text style a bit bolder (see bolder() below).
    MaterialTheme(typography = MaterialTheme.typography.bolder()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .dotGrid(MaterialTheme.colorScheme.surfaceVariant) // faint dots; before verticalScroll so they stay still
                // fillMaxSize() before verticalScroll makes the column at least screen-tall,
                // so the two weight(1f) spacers below can share out the leftover height.
                .verticalScroll(rememberScrollState()) // scrolls on small screens / with the keyboard open
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Settings gear, top right: a small rounded square. What it opens is UNDECIDED (stub).
            Box(modifier = Modifier.fillMaxWidth()) {
                InkButton(
                    onClick = onSettings,
                    shape = RoundedCornerShape(15.dp),
                    shadow = 3.dp,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(46.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Settings", // read out by screen readers
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Gaps below follow the mockup. On phones taller than the mockup, the leftover
            // height is split evenly here and at the very bottom (both weight(1f)).
            // The bottom also gets a fixed 62dp, the same as the settings row (46dp) plus
            // this 16dp gap, so the empty space above the title matches the space below Free Draw.
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))

            // 2. Title: "inkorporate" plus ".it" in the theme's primary color.
            // One size up (displayMedium). ExtraBold is FAKED: Type.kt only loads Sniglet Regular,
            // so Android draws it thicker. (Real Sniglet ExtraBold was tried Nov 9: too heavy and wide.)
            Text(
                text = buildAnnotatedString {
                    append("inkorporate")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(".it")
                    }
                },
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3. The player's blob with the Edit button (opens the avatar editor stub).
            AvatarBlob(avatar = player.avatar, onEdit = onEditAvatar, size = 220.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Name. copy() makes a new Player with only the username changed.
            Text(
                text = "Name",
                style = MaterialTheme.typography.titleMedium, // small heading above a field (16sp)
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            NameField(
                name = player.username,
                onNameChange = { newName -> onPlayerChange(player.copy(username = newName)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 5 + 6. Create game on the left, join with a code on the right.
            // MatchRightHeightRow (bottom of this file) makes Create game exactly as tall
            // as the right column, in one measuring step.
            MatchRightHeightRow(
                gap = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                left = {
                    InkButton(
                        onClick = {
                            focusManager.clearFocus() // saves a name still being typed
                            onCreateGame()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp),
                        shadow = 6.dp
                    ) {
                        Text(
                            text = "Create game", // wraps to two lines at this width
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                right = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "or with a code",
                            style = MaterialTheme.typography.titleMedium, // small heading above a field (16sp)
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        InkTextField(
                            value = joinCode,
                            // filterJoinCode drops letters and stops at 6 digits.
                            onValueChange = { typed -> joinCode = filterJoinCode(typed) },
                            placeholder = "Code",
                            errorText = joinError, // red outline + message when joining failed
                            // Spaced-out digits, like the design.
                            textStyle = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, // number keyboard
                                imeAction = ImeAction.Go           // keyboard shows a "Go" key
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { if (isJoinCodeComplete(joinCode)) onJoinGame(joinCode) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Enabled only with exactly 6 digits. Yellow when enabled, grey when not.
                        InkButton(
                            onClick = {
                                focusManager.clearFocus() // saves a name still being typed
                                // This runs ~0.18 s after the tap (button bounce). Check the code
                                // again: a digit deleted in that time must not send 5 digits.
                                if (isJoinCodeComplete(joinCode)) onJoinGame(joinCode)
                            },
                            enabled = isJoinCodeComplete(joinCode),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = inkOnYellow(), // near-black, in light and dark mode
                            shadow = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join game")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Free Draw: full-width white button with a pencil icon.
            InkButton(
                onClick = onFreeDraw,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp) // at least 58dp; taller if big phone text needs room
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null // the "Free Draw" text already says what it does
                )
                Text("Free Draw", style = MaterialTheme.typography.titleLarge)
            }

            // The other half of the leftover height, plus the settings row's height (see above the title).
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(62.dp))
        }
    }
}

/*
 * The theme's text styles, all one step bolder (SemiBold), for Home only.
 * Sniglet has no SemiBold file, so Android draws Regular a bit thicker ("fake bold").
 * This belongs in Type.kt so every screen gets it; ask Ismael, then delete this.
 */
private fun Typography.bolder(): Typography {
    // copy() keeps size and font, and only changes the weight.
    fun TextStyle.semiBold() = copy(fontWeight = FontWeight.SemiBold)
    return Typography(
        displayLarge = displayLarge.semiBold(),
        displayMedium = displayMedium.semiBold(),
        displaySmall = displaySmall.semiBold(),
        headlineLarge = headlineLarge.semiBold(),
        headlineMedium = headlineMedium.semiBold(),
        headlineSmall = headlineSmall.semiBold(),
        titleLarge = titleLarge.semiBold(),
        titleMedium = titleMedium.semiBold(),
        titleSmall = titleSmall.semiBold(),
        bodyLarge = bodyLarge.semiBold(),
        bodyMedium = bodyMedium.semiBold(),
        bodySmall = bodySmall.semiBold(),
        labelLarge = labelLarge.semiBold(),
        labelMedium = labelMedium.semiBold(),
        labelSmall = labelSmall.semiBold()
    )
}

/*
 * Two halves side by side, with the LEFT one made exactly as tall as the RIGHT one.
 * Used for Create game (left) next to the code field + Join button (right).
 *
 * Why not Row + IntrinsicSize.Min? That kept the button text in the default font
 * instead of Sniglet (tested Nov 9, with Button and with InkButton).
 * Why not measure the right side and copy its height into state? That takes two
 * frames, so Create game showed at the wrong size for a moment when coming back
 * to Home. Layout does it in one step:
 *   1. measure the right half (half the width, any height)
 *   2. measure the left half at the same width and exactly that height
 *   3. place them next to each other
 */
@Composable
private fun MatchRightHeightRow(
    gap: Dp,
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    // contents = two slots; measurables[0] is everything in `left`, measurables[1] in `right`.
    Layout(contents = listOf(left, right), modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()                            // dp → pixels
        val halfWidth = (constraints.maxWidth - gapPx) / 2

        // 1. Right half first: fixed width, as tall as it wants.
        val rightPlaceables = measurables[1].map { it.measure(Constraints.fixedWidth(halfWidth)) }
        val height = rightPlaceables.maxOfOrNull { it.height } ?: 0

        // 2. Left half: same width, exactly the right half's height.
        val leftPlaceables = measurables[0].map { it.measure(Constraints.fixed(halfWidth, height)) }

        // 3. Place them: left at x = 0, right after the gap.
        layout(width = constraints.maxWidth, height = height) {
            leftPlaceables.forEach { it.place(x = 0, y = 0) }
            rightPlaceables.forEach { it.place(x = halfWidth + gapPx, y = 0) }
        }
    }
}

/*
 * The name box. Only Home uses it, so it's private to this file.
 *
 * While typing, the text lives in `draft`. Editing counts as "done" when the
 * keyboard's Done key is pressed or the field loses focus. Then cleanName()
 * trims it, cuts it to 16 characters, and puts the old name back if it's empty.
 */
@Composable
private fun NameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(name): start from the saved name, and start over if it changes.
    var draft by remember(name) { mutableStateOf(name) }
    val focusManager = LocalFocusManager.current

    InkTextField(
        value = draft,
        onValueChange = { typed -> draft = typed },
        placeholder = "Your name",
        shape = CircleShape, // pill shape
        textStyle = MaterialTheme.typography.titleLarge,
        // Small grey pencil on the right, a hint that the name can be changed.
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done // keyboard shows a "Done" key
        ),
        // Done just drops focus. Losing focus (below) is what saves the name,
        // so Done and tapping away both go through the same code.
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier.onFocusChanged { state ->
            if (!state.isFocused) {
                val cleaned = cleanName(draft, oldName = name)
                draft = cleaned
                if (cleaned != name) onNameChange(cleaned)
            }
        }
    )
}

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

@Preview(showBackground = true, name = "Home, light")
@Composable
fun HomeScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface { // Surface paints the theme's background color behind the screen
            HomeScreen(
                player = Player(username = "Alejandro"),
                onPlayerChange = {},
                onSettings = {},
                onEditAvatar = {},
                onCreateGame = {},
                onJoinGame = {},
                onFreeDraw = {}
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Home, dark")
@Composable
fun HomeScreenDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            HomeScreen(
                player = Player(username = "Jerry"),
                onPlayerChange = {},
                onSettings = {},
                onEditAvatar = {},
                onCreateGame = {},
                onJoinGame = {},
                onFreeDraw = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Home, join error")
@Composable
fun HomeScreenJoinErrorPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface {
            HomeScreen(
                player = Player(username = "Jorge"),
                onPlayerChange = {},
                onSettings = {},
                onEditAvatar = {},
                onCreateGame = {},
                onJoinGame = {},
                onFreeDraw = {},
                joinError = "Room not found"
            )
        }
    }
}
