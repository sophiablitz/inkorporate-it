package com.example.inkorporate_it.ui.frontendscreens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.R
import com.example.inkorporate_it.model.Player
import com.example.inkorporate_it.model.cleanName
import com.example.inkorporate_it.model.filterJoinCode
import com.example.inkorporate_it.model.isJoinCodeComplete
import com.example.inkorporate_it.ui.component.AvatarBlob
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * The Home screen. Layout follows Jorge's barebones wireframe, top to bottom:
 * settings, title, avatar + Edit, name, Create game, then join with a code.
 *
 * State hoisting: Home doesn't own the player or decide where buttons go.
 * The caller passes the player in and gets told what happened through the
 * on... functions. Navigation is UNDECIDED, so the caller wires these later.
 *
 * - player / onPlayerChange: the current player, and how to save a new name
 * - joinError: text to show under the code field when joining failed (null = no error)
 * - onSettings, onEditAvatar, onCreateGame: button taps
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
    modifier: Modifier = Modifier,
    joinError: String? = null
) {
    // The code being typed. Only Home needs it, so Home keeps it.
    // rememberSaveable keeps it when the phone rotates. It also stays after a failed join.
    var joinCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // scrolls on small screens / with the keyboard open
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Settings gear, top right. What it opens is UNDECIDED (stub).
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings" // read out by screen readers
                )
            }
        }

        // 2. Title: "inkorporate" plus ".it" in the theme's primary color.
        Text(
            text = buildAnnotatedString {
                append("inkorporate")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(".it")
                }
            },
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. The player's blob with the Edit button (opens the avatar editor stub).
        AvatarBlob(avatar = player.avatar, onEdit = onEditAvatar)

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Name. copy() makes a new Player with only the username changed.
        Text(text = "Name", style = MaterialTheme.typography.titleMedium)
        NameField(
            name = player.username,
            onNameChange = { newName -> onPlayerChange(player.copy(username = newName)) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 5 + 6. Create game on the left, join with a code on the right.
        // IntrinsicSize.Min lets the Create button match the height of the right column.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCreateGame,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)      // half the row
                    .fillMaxHeight() // as tall as the right column
            ) {
                Text(
                    text = "Create game",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(1f), // the other half
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "or with a code", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = joinCode,
                    // filterJoinCode drops letters and stops at 6 digits.
                    onValueChange = { typed -> joinCode = filterJoinCode(typed) },
                    placeholder = { Text("Code") },
                    singleLine = true,
                    isError = joinError != null, // red outline when joining failed
                    supportingText = if (joinError != null) {
                        { Text(joinError) }      // the error message under the field
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, // number keyboard
                        imeAction = ImeAction.Go           // keyboard shows a "Go" key
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { if (isJoinCodeComplete(joinCode)) onJoinGame(joinCode) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Enabled only with exactly 6 digits.
                OutlinedButton(
                    onClick = { onJoinGame(joinCode) },
                    enabled = isJoinCodeComplete(joinCode),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Join game")
                }
            }
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

    OutlinedTextField(
        value = draft,
        onValueChange = { typed -> draft = typed },
        placeholder = { Text("Your name") },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
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
                onJoinGame = {}
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
                onJoinGame = {}
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
                joinError = "Room not found"
            )
        }
    }
}
