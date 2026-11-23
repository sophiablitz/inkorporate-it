package com.example.inkorporate_it.model

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.random.Random

/*
 * Player and Avatar data for the Home screen.
 *
 * NOT FINAL: the team has three different versions of these objects
 * (Jorge's Home doc, Jerry's example code, Alejandro's old Lobby notes).
 * This file uses Jorge's Home doc version until the team picks one.
 * Everything is in this one file so it's easy to change later.
 */

// The look of a player's ink blob.
data class Avatar(
    val hatId: String = "none",        // which hat the blob wears ("none", "beanie", ...)
    val eyesId: String = "round",      // which eyes the blob has ("round", "wide", ...)
    val inkColor: Color = DefaultInkColor // the blob's color
)
/*
 * Tells rememberSaveable how to save an Avatar and get it back.
 * Rotating the phone, switching dark mode or changing the font size rebuilds
 * the screen, and plain `remember` forgets everything.
 * rememberSaveable keeps values, but only simple ones (text, numbers),
 * so this turns an Avatar into a list of those and back.
 * If Avatar gets new fields, add them here too.
 */
val AvatarSaver = listSaver<Avatar, Any>(
    // Avatar → [hat, eyes, color as one number]
    save = { avatar ->
        listOf(
            avatar.hatId,
            avatar.eyesId,
            avatar.inkColor.toArgb() // Color → Int (e.g. 0xFF3B4FE0)
        )
    },
    // [hat, eyes, color] → Avatar. Same order as in save.
    restore = { saved ->
        Avatar(
            hatId = saved[0] as String,
            eyesId = saved[1] as String,
            inkColor = Color(saved[2] as Int) // Int → Color
        )
    }
)
// Indigo, the first blob color (BlobColors.kt), like the blob in Jorge's colorful wireframe.
// The default color is not decided.
val DefaultInkColor = BlobColors.Indigo
