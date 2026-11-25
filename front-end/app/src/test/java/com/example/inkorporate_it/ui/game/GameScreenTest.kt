package com.example.inkorporate_it.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for formatSeconds in GameScreen.kt, the timer text in the top bar.
class GameScreenTest {

    @Test
    fun formatSeconds_underAMinutePadsSeconds() {
        assertEquals("0:42", formatSeconds(42))
        assertEquals("0:05", formatSeconds(5))
    }

    @Test
    fun formatSeconds_overAMinuteShowsMinutes() {
        assertEquals("1:15", formatSeconds(75))
        assertEquals("2:00", formatSeconds(120))
    }

    @Test
    fun formatSeconds_zeroAndNegativeShowZero() {
        assertEquals("0:00", formatSeconds(0))
        assertEquals("0:00", formatSeconds(-3))
    }
}
