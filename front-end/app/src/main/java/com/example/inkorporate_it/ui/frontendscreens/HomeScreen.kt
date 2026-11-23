package com.example.inkorporate_it.ui.frontendscreens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.inkorporate_it.R
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.ui.component.AvatarBlob
import com.example.inkorporate_it.ui.component.InkButton
import com.example.inkorporate_it.ui.component.InkTextField
import com.example.inkorporate_it.ui.component.dotGrid
import com.example.inkorporate_it.ui.component.inkOnYellow
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import com.example.inkorporate_it.ui.theme.extraBoldFontFamily

/*
 * The Home screen. Three sections, top to bottom (#166):
 * 15% title + settings, 35% avatar + Edit, 50% name, Create game, join with a code, Free Draw.
 * The look (outlines, hard shadows, colors) follows the colorful design.
 *
 * State hoisting: Home doesn't own the player or decide where buttons go.
 * The caller passes the player in and gets told what happened through the
 * on... functions. Navigation is UNDECIDED, so the caller wires these later.
 *
 * - username / onUsernameChange: the player's name, and how to save a new one
 * - avatar / onAvatarChange: the player's blob, and how to save a new one
 * - joinError: text to show under the code field when joining failed (null = no error)
 * - onSettings, onCreateGame, onFreeDraw: button taps
 * - onJoinGame: Join tapped with a complete 6-digit code
 */
@Composable
fun HomeScreen(
    username: String,
    avatar: Avatar,
    onUsernameChange: (String) -> Unit,
    onAvatarChange: (Avatar) -> Unit,
    onSettings: () -> Unit,
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
        // BoxWithConstraints tells us how much room Home has (maxWidth, maxHeight),
        // so the sections below can be sized as a share of the screen.
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .dotGrid(MaterialTheme.colorScheme.surfaceVariant) // faint dots behind everything
                // The keyboard pushes Home up instead of covering the fields. This works with
                // windowSoftInputMode="adjustResize" in AndroidManifest.xml, which stops Android
                // from also sliding the whole window up (that made room for the keyboard twice).
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // The top row gets at least 15% of Home's height, the bottom at least half.
            val topMinHeight = maxHeight * 0.15f
            val bottomMinHeight = maxHeight * 0.5f

            /*
             * Three sections, top to bottom: 15% title row, 35% avatar, 50% buttons.
             * No verticalScroll: everything fits on one screen.
             *
             * How the split works: the top row and the bottom section have no weight, so
             * Column measures them first: each takes its share (15% / 50%), or more if its
             * content needs more. The avatar (weight 1f) gets whatever is left, 35% normally.
             * On a short screen, or with the keyboard open, the title and buttons keep their
             * size and only the avatar shrinks.
             */
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    // On wide screens (the unfolded Fold) stay phone-width and centered.
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
            ) {
                // 1. Top row (at least 15%): title on the left, settings on the right.
                Row(
                    modifier = Modifier
                        .heightIn(min = topMinHeight)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title: "inkorporate" in Sniglet Regular (with Home's usual fake bold),
                    // then ".it" in the real Sniglet ExtraBold font and the theme's primary color.
                    // buildAnnotatedString = one piece of text where parts can have their own style.
                    // SpanStyle = the style for the part inside withStyle { }.
                    // weight(1f): the title takes all the width the settings button doesn't.
                    // autoSize: starts at displayMedium (45sp) and steps down until it fits on one
                    // line, so it doesn't wrap on narrow phones.
                    Text(
                        text = buildAnnotatedString {
                            append("inkorporate")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = extraBoldFontFamily, // the bundled ExtraBold file (Type.kt)
                                    fontWeight = FontWeight.ExtraBold // matches that file, so nothing is faked
                                )
                            ) {
                                append(".it")
                            }
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 24.sp,
                            maxFontSize = MaterialTheme.typography.displayMedium.fontSize
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Settings gear: a small rounded square. What it opens is UNDECIDED (stub).
                    InkButton(
                        onClick = onSettings,
                        shape = RoundedCornerShape(15.dp),
                        shadow = 4.dp, // 4-5dp shadows on Home's buttons (#166)
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings", // read out by screen readers
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Avatar (the rest, 35% normally): the biggest square that fits, so the blob
                // grows and shrinks with the screen. Edit is Sophia's placeholder for now (see AvatarBlob).
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // min(): the smaller of this section's width and height.
                    // Minus 16dp so the blob's shadow and the Edit pill stay inside the section.
                    val blobSize = (min(maxWidth, maxHeight) - 16.dp).coerceAtLeast(0.dp)
                    // A tiny blob (keyboard open) has no room for the Edit pill, so leave it out
                    // (null = no Edit button). It comes back when the keyboard closes.
                    AvatarBlob(
                        avatar = avatar,
                        onEdit = if (blobSize >= MinBlobSizeForEdit) onAvatarChange else null,
                        size = blobSize
                    )
                }

                // 3. Bottom (at least 50%): name, Create / Join, Free Draw.
                // spacedBy(..., CenterVertically): 24dp between the parts, and any spare
                // height split above and below them.
                // padding(top) after heightIn: the 16dp is part of the 50%, and keeps "Name"
                // off the avatar on short screens where there's no spare height.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = bottomMinHeight)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 4. Name. Label and name both on the left, lined up with "or with a code" below.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.titleMedium, // small heading above a field (16sp)
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NameField(
                            name = username,
                            onNameChange = onUsernameChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                                shadow = 5.dp,
                                outlineWidth = BottomOutlineWidth
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
                                    outlineWidth = BottomOutlineWidth,
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
                                    // Same as Create game, so the bottoms line up: pressed in (disabled)
                                    // Join ends where Create game's shadow ends.
                                    shadow = 5.dp,
                                    outlineWidth = BottomOutlineWidth,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Join game")
                                }
                            }
                        }
                    )

                    // 7. Free Draw: full-width white button with a pencil icon.
                    InkButton(
                        onClick = onFreeDraw,
                        outlineWidth = BottomOutlineWidth,
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
                }
            }
        }
    }
}

// Widest Home's content gets. Phones are narrower, so this only matters on the unfolded Fold and tablets.
private val MaxContentWidth = 420.dp

// Outlines in the bottom half (name, code, Create game, Join, Free Draw) are a bit thicker
// than the top half's (InkOutlineWidth, 2.5dp), from #166.
private val BottomOutlineWidth = 3.5.dp

// Below this size the blob is smaller than its Edit pill, so Home hides Edit.
private val MinBlobSizeForEdit = 120.dp

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
        // Same corners as the code field (InkTextField's default). A full pill's round
        // end cut into the left-aligned name.
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Start, // name on the left, pencil on the right (#166)
        outlineWidth = BottomOutlineWidth,
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


// Longest name allowed, from the Home screen spec.
private const val MAX_NAME_LENGTH = 16

// Join codes are exactly this many digits.
private const val JOIN_CODE_LENGTH = 6

/*
 * Cleans up a name after the player finishes editing it.
 * - trim() removes spaces at the start and end
 * - take(16) keeps at most 16 characters
 * - if nothing is left, the old name comes back
 */
internal fun cleanName(newName: String, oldName: String): String {
    val cleaned = newName.trim().take(MAX_NAME_LENGTH)
    return if (cleaned.isEmpty()) oldName else cleaned
}

/*
 * Keeps only digits and stops at 6 of them.
 * Used on every keystroke in the join code field, so letters never appear.
 */
internal fun filterJoinCode(input: String): String {
    return input.filter { it.isDigit() }.take(JOIN_CODE_LENGTH)
}

// True when the code is exactly 6 digits, so the Join button can be enabled.
internal fun isJoinCodeComplete(code: String): Boolean {
    return code.length == JOIN_CODE_LENGTH && code.all { it.isDigit() }
}

// --- Previews (dynamicColor = false shows the team's colors, not wallpaper colors) ---

@Preview(showBackground = true, name = "Home, light")
@Composable
fun HomeScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Surface { // Surface paints the theme's background color behind the screen
            HomeScreen(
                username = "Alejandro",
                avatar = Avatar(),
                onUsernameChange = {},
                onSettings = {},
                onAvatarChange = {},
                onCreateGame = {},
                onJoinGame = {},
                onFreeDraw = {},
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
                username = "Jerry",
                avatar = Avatar(),
                onUsernameChange = {},
                onSettings = {},
                onAvatarChange = {},
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
                username = "Jerry",
                avatar = Avatar(),
                onUsernameChange = {},
                onSettings = {},
                onAvatarChange = {},
                onCreateGame = {},
                onJoinGame = {},
                onFreeDraw = {},
                joinError = "Room not found",
            )
        }
    }
}
