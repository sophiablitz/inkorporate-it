package com.example.inkorporate_it.model

import androidx.compose.ui.graphics.Color

/*
 * The colors a player's ink blob can be (from the blob color sheet).
 * These are NOT app theme colors: they don't change in dark mode, and buttons
 * or text shouldn't use them. Only the blob (Avatar.inkColor) does.
 *
 * Use BlobColors.all wherever a blob color is chosen, for example the
 * color selector (issue #118) or the default color in PlayerData.kt.
 *
 * `object` = one shared set of values, used like BlobColors.Indigo.
 */
object BlobColors {
    val Indigo = Color(0xFF3B4FE0)
    val Violet = Color(0xFF8A4FE0)
    val Pink = Color(0xFFF06BA8)
    val Red = Color(0xFFE5484D)
    val Orange = Color(0xFFF5883A)
    val Yellow = Color(0xFFFCD365)
    val Green = Color(0xFF3DBE7A)
    val Teal = Color(0xFF2BB5C9)

    // All eight, in the order of the color sheet.
    val all = listOf(Indigo, Violet, Pink, Red, Orange, Yellow, Green, Teal)
}
