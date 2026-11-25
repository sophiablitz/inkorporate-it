package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.R
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * A player's ink blob inside a round frame. Shared: Home shows it big,
 * and the Lobby can show it small.
 *
 * PLACEHOLDER: there is no blob art yet (hat, eyes), so the blob is a
 * plain circle in the avatar's ink color.
 *
 * - size: how big the whole frame is
 * No Edit button here: Home uses EditableAvatarBlob (below) for that.
 */
@Composable
fun AvatarBlob(
    avatar: Avatar,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    // Box stacks its children on top of each other.
    Box(modifier = modifier.size(size)) {
        // The round frame: white fill, thick dark outline, hard shadow below.
        // The shadow grows with the frame (6dp on Home's 200dp blob, ~2dp in the Lobby).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hardShadow(CircleShape, MaterialTheme.colorScheme.onBackground, offset = size * 0.03f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // The blob itself: 60% of the frame, in the player's ink color.
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .background(avatar.inkColor, CircleShape)
            )
        }
    }
}

/*
 * A panel that slides up from the bottom with the blob color selector.
 * Only EditableAvatarBlob (Home) uses it, so it's private to this file.
 * It closes when the player swipes it down, taps the dimmed area above it,
 * or presses Back; all of those call onDismiss.
 * Picking a color doesn't close it, so the player can try a few colors.
 */
@OptIn(ExperimentalMaterial3Api::class) // ModalBottomSheet is marked experimental in Material 3
@Composable
private fun BlobColorSheet(
    selected: Color,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // skipPartiallyExpanded: open all the way at once (the sheet is short anyway).
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The sheet's handle (the small bar at the top) can be tapped to close the sheet,
    // and its press effect (ripple) is a square by default. ripple(bounded = false)
    // makes press effects in the sheet round instead. CompositionLocalProvider
    // swaps the press effect for everything inside it.
    // radius = 20.dp: the handle's center is 24dp below the sheet's top edge, so a
    // 20dp circle stays inside the sheet (a bigger one spilled over the top).
    // Anything added to this sheet later gets this small effect too, unless it sets
    // its own indication (BlobColorSelector does: indication = ripple()).
    CompositionLocalProvider(LocalIndication provides ripple(bounded = false, radius = 20.dp)) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Blob color",
                    style = MaterialTheme.typography.titleLarge, // sheet heading (22sp)
                    // heading(): screen readers (TalkBack) can jump straight to this title.
                    modifier = Modifier.semantics { heading() }
                )
                BlobColorSelector(selected = selected, onPick = onPick)
            }
        }
    }
}

/*
 * Home's blob: AvatarBlob plus a yellow "Edit" button at the bottom right.
 * Edit opens the blob color sheet; picking a color calls onAvatarChange.
 * Other screens (Lobby, End of Game) use plain AvatarBlob, with no Edit button.
 *
 * - size: how big the whole frame is (Home can pass a size that fits the screen)
 * - onEditClick: runs first when Edit is tapped. Home uses it to clear focus,
 *   so a name still being typed gets saved before the sheet opens.
 * - onAvatarChange: gets the new avatar when a color is picked
 */
@Composable
fun EditableAvatarBlob(
    avatar: Avatar,
    onAvatarChange: (Avatar) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onEditClick: () -> Unit = {}
) {
    // Whether the blob color sheet is open. rememberSaveable keeps it open when the phone rotates.
    var showColorSheet by rememberSaveable { mutableStateOf(false) }

    // Box stacks its children on top of each other: the blob, then the Edit button over it.
    // The caller's modifier goes on this outer Box only (not on AvatarBlob too).
    Box(modifier = modifier.size(size)) {
        AvatarBlob(avatar = avatar, size = size)

        // A small pill in the theme's secondaryContainer color (yellow in our theme).
        InkButton(
            onClick = {
                onEditClick()          // Home's listener (clears focus, saves the name)
                showColorSheet = true  // then open the sheet
            },
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

        // The color sheet, on top of Home while it's open.
        if (showColorSheet) {
            BlobColorSheet(
                selected = avatar.inkColor,
                // copy() makes a new Avatar with only the color changed.
                // The blob uses avatar, so it changes color right away (#119).
                onPick = { color -> onAvatarChange(avatar.copy(inkColor = color)) },
                onDismiss = { showColorSheet = false }
            )
        }
    }
}

// dynamicColor = false so the preview shows the team's colors, not wallpaper colors.
@Preview(showBackground = true, name = "Light, no Edit")
@Composable
fun AvatarBlobPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), size = 100.dp)
    }
}

@Preview(showBackground = true, name = "Light, with Edit")
@Composable
fun EditableAvatarBlobPreview() {
    InkorporateitTheme(dynamicColor = false) {
        EditableAvatarBlob(avatar = Avatar(), onAvatarChange = {})
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark, small, no Edit"
)
@Composable
fun AvatarBlobSmallDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), size = 64.dp)
    }
}
