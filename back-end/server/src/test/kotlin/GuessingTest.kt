package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Tests for the scoring functions in Guessing.kt, against the design doc's worked example (turnSeconds = 80).
class GuessingTest {
    // --- pointsForGuess ---
    @Test
    fun `first guesser at 9s scores 151`() {
        assertEquals(151, pointsForGuess(rank = 1, secondsTaken = 9, turnSeconds = 80, playerCount = 5))
    }
    @Test
    fun `second guesser at 22s scores 112`() {
        assertEquals(112, pointsForGuess(rank = 2, secondsTaken = 22, turnSeconds = 80, playerCount = 5))
    }
    @Test
    fun `third guesser at 38s rounds 91 point 5 up to 92`() {
        assertEquals(92, pointsForGuess(rank = 3, secondsTaken = 38, turnSeconds = 80, playerCount = 5))
    }
    @Test
    fun `fourth guesser at 61s scores 71`() {
        assertEquals(71, pointsForGuess(rank = 4, secondsTaken = 61, turnSeconds = 80, playerCount = 5))
    }
    @Test
    fun `non-positive turnSeconds scores 0`() {
        assertEquals(0, pointsForGuess(rank = 1, secondsTaken = 9, turnSeconds = 0, playerCount = 5))
        assertEquals(0, pointsForGuess(rank = 1, secondsTaken = 9, turnSeconds = -80, playerCount = 5))
    }
    @Test
    fun `rank below 1 scores 0`() {
        assertEquals(0, pointsForGuess(rank = 0, secondsTaken = 9, turnSeconds = 80, playerCount = 5))
    }
    @Test
    fun `secondsTaken is clamped into the turn`() {
        // Beyond the turn clamps to turnSeconds (T = 0.5); below 0 clamps to 0 (T = 1.0).
        assertEquals(80, pointsForGuess(rank = 1, secondsTaken = 999, turnSeconds = 80, playerCount = 5))
        assertEquals(160, pointsForGuess(rank = 1, secondsTaken = -5, turnSeconds = 80, playerCount = 5))
    }
    // --- pointsForDrawer ---
    @Test
    fun `drawer with four of five guessers scores 159`() {
        assertEquals(159, pointsForDrawer(guessSecondsTaken = listOf(9, 22, 38, 61), turnSeconds = 80, totalGuessers = 5))
    }
    @Test
    fun `drawer with no correct guessers scores 0`() {
        assertEquals(0, pointsForDrawer(guessSecondsTaken = emptyList(), turnSeconds = 80, totalGuessers = 5))
    }
    @Test
    fun `non-positive turnSeconds scores the drawer 0`() {
        assertEquals(0, pointsForDrawer(guessSecondsTaken = listOf(9, 22), turnSeconds = 0, totalGuessers = 5))
    }
    @Test
    fun `non-positive totalGuessers scores the drawer 0`() {
        assertEquals(0, pointsForDrawer(guessSecondsTaken = listOf(9, 22), turnSeconds = 80, totalGuessers = 0))
    }
    @Test
    fun `more correct guessers than total guessers scores 0`() {
        assertEquals(0, pointsForDrawer(guessSecondsTaken = listOf(9, 22, 38), turnSeconds = 80, totalGuessers = 2))
    }
    // --- guess matching ---
    @Test
    fun `exact guess matches`() {
        assertTrue(guessMatches("apple", "apple"))
    }
    @Test
    fun `guess matches ignoring case`() {
        assertTrue(guessMatches("ApPlE", "apple"))
    }
    @Test
    fun `guess matches with leading and trailing spaces`() {
        assertTrue(guessMatches("  apple  ", "apple"))
    }
    @Test
    fun `guess matches with doubled interior spaces`() {
        assertTrue(guessMatches("red  apple", "red apple"))
    }
    @Test
    fun `near miss does not match`() {
        assertFalse(guessMatches("apples", "apple"))
    }
    @Test
    fun `empty guess does not match`() {
        assertFalse(guessMatches("", "apple"))
    }
    @Test
    fun `whitespace-only guess does not match`() {
        assertFalse(guessMatches("   ", "apple"))
    }
}
