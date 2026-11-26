package com.example.inkorporate_it.ui.game

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for the size rules in EndOfGame.kt.
class EndOfGameTest {

    // --- endOfGameTextScale ---

    @Test
    fun textScale_shortPhoneIsSmaller() {
        assertEquals(0.9f, endOfGameTextScale(640.dp))
    }

    @Test
    fun textScale_tallPhoneIsBigger() {
        assertEquals(1.05f, endOfGameTextScale(915.dp))
    }

    @Test
    fun textScale_inBetweenIsNormal() {
        assertEquals(1f, endOfGameTextScale(844.dp))
        assertEquals(1f, endOfGameTextScale(641.dp))
    }

    // --- resultRowHeight ---

    @Test
    fun rowHeight_isAFifthOfWhatsLeft() {
        // (247 - 12 - 3 - 32) / 5 = 40
        assertEquals(40.dp, resultRowHeight(247.dp))
    }

    @Test
    fun rowHeight_staysBetween30And46() {
        assertEquals(30.dp, resultRowHeight(100.dp))
        assertEquals(46.dp, resultRowHeight(500.dp))
    }
}
