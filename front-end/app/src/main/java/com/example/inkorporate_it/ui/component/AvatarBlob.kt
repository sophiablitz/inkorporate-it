package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
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
    name = "Dark, small, no Edit"
)
@Composable
fun AvatarBlobSmallDarkPreview() {
    InkorporateitTheme(dynamicColor = false) {
        AvatarBlob(avatar = Avatar(), size = 64.dp)
    }
}

