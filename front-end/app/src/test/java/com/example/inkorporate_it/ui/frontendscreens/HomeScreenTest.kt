package com.example.inkorporate_it.ui.frontendscreens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the name and join-code rules in HomeScreen.kt.
class HomeScreenTest {

    // --- cleanName ---

    @Test
    fun cleanName_trimsSpaces() {
        assertEquals("Jerry", cleanName("  Jerry  ", oldName = "Old"))
    }

    @Test
    fun cleanName_cutsTo16Characters() {
        assertEquals("ABCDEFGHIJKLMNOP", cleanName("ABCDEFGHIJKLMNOPQRS", oldName = "Old"))
    }

    @Test
    fun cleanName_emptyGivesOldNameBack() {
        assertEquals("Old", cleanName("", oldName = "Old"))
        assertEquals("Old", cleanName("    ", oldName = "Old"))
    }

    // --- filterJoinCode ---

    @Test
    fun filterJoinCode_removesNonDigits() {
        assertEquals("123", filterJoinCode("1a2-b3"))
    }

    @Test
    fun filterJoinCode_stopsAtSixDigits() {
        assertEquals("123456", filterJoinCode("12345678"))
    }

    // --- isJoinCodeComplete ---

    @Test
    fun isJoinCodeComplete_onlyExactlySixDigits() {
        assertTrue(isJoinCodeComplete("123456"))
        assertFalse(isJoinCodeComplete("12345"))
        assertFalse(isJoinCodeComplete(""))
        assertFalse(isJoinCodeComplete("12345a"))
    }
}
