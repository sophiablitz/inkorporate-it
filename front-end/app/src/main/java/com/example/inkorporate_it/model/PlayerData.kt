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

// One player. Home only needs a name and an avatar for now.
data class Player(
    val username: String,
    val avatar: Avatar = Avatar()
)

// Blue, like the blob in Jorge's colorful wireframe. The default color is not decided.
val DefaultInkColor = Color(0xFF3B4FE0)

// Longest name allowed, from the Home screen spec.
const val MAX_NAME_LENGTH = 16

// Join codes are exactly this many digits.
const val JOIN_CODE_LENGTH = 6

// Names picked at random on first launch. Add more team names here.
val DefaultNames = listOf("Alejandro", "Jerry", "Jorge", "Ismael")

/*
 * Cleans up a name after the player finishes editing it.
 * - trim() removes spaces at the start and end
 * - take(16) keeps at most 16 characters
 * - if nothing is left, the old name comes back
 */
fun cleanName(newName: String, oldName: String): String {
    val cleaned = newName.trim().take(MAX_NAME_LENGTH)
    return if (cleaned.isEmpty()) oldName else cleaned
}

/*
 * Keeps only digits and stops at 6 of them.
 * Used on every keystroke in the join code field, so letters never appear.
 */
fun filterJoinCode(input: String): String {
    return input.filter { it.isDigit() }.take(JOIN_CODE_LENGTH)
}

// True when the code is exactly 6 digits, so the Join button can be enabled.
fun isJoinCodeComplete(code: String): Boolean {
    return code.length == JOIN_CODE_LENGTH && code.all { it.isDigit() }
}

/*
 * The player you get on first launch: a random team name and the default blob.
 * `random` can be swapped in tests so the result is predictable.
 */
fun randomDefaultPlayer(random: Random = Random): Player {
    return Player(username = DefaultNames.random(random))
}

/*
 * Tells rememberSaveable how to save a Player and get it back.
 * Rotating the phone, switching dark mode or changing the font size rebuilds
 * the screen, and plain `remember` forgets everything (new random name).
 * rememberSaveable keeps values, but only simple ones (text, numbers),
 * so this turns a Player into a list of those and back.
 * If Player or Avatar get new fields, add them here too.
 */
val PlayerSaver = listSaver<Player, Any>(
    // Player → [name, hat, eyes, color as one number]
    save = { player ->
        listOf(
            player.username,
            player.avatar.hatId,
            player.avatar.eyesId,
            player.avatar.inkColor.toArgb() // Color → Int (e.g. 0xFF3B4FE0)
        )
    },
    // [name, hat, eyes, color] → Player. Same order as in save.
    restore = { saved ->
        Player(
            username = saved[0] as String,
            avatar = Avatar(
                hatId = saved[1] as String,
                eyesId = saved[2] as String,
                inkColor = Color(saved[3] as Int) // Int → Color
            )
        )
    }
)
