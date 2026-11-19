package it.inkorporate

import kotlin.math.roundToInt

/**
 * Scoring for guessers and the drawer, from the back-end design doc's scoring table.
 * The Guessing class that uses these arrives with #105.
 */

/**
 * Time bonus T for a guess taken in [secondsTaken] of [turnSeconds]: 0.5..1.0, higher when faster.
 * secondsTaken is clamped into 0..turnSeconds so a late or negative value can't push T out of range.
 */
private fun timeBonus(secondsTaken: Int, turnSeconds: Int): Double {
    val taken = secondsTaken.coerceIn(0, turnSeconds)
    val timeLeft = turnSeconds - taken
    return 0.5 + 0.5 * (timeLeft.toDouble() / turnSeconds)
}

// Round-half-up: Kotlin's roundToInt does exactly this for the 91.5 -> 92 case in the design table.
private fun roundHalfUp(points: Double): Int = points.roundToInt()

/** Points for the guesser who got it [rank]-th (1 = first). Follows the design doc's scoring table. */
fun pointsForGuess(rank: Int, secondsTaken: Int, turnSeconds: Int, playerCount: Int): Int {
    // playerCount is currently unused; kept so callers match the planned signature.
    if (turnSeconds <= 0 || rank < 1) return 0
    val points = (100 + 60.0 / rank) * timeBonus(secondsTaken, turnSeconds)
    return roundHalfUp(points)
}

/**
 * Points for the drawer: 250, scaled by the share of guessers who got it and their mean time bonus.
 * [guessSecondsTaken] has one entry per correct guesser; an empty list means nobody guessed, so 0.
 */
fun pointsForDrawer(guessSecondsTaken: List<Int>, turnSeconds: Int, totalGuessers: Int): Int {
    if (turnSeconds <= 0 || totalGuessers <= 0 || guessSecondsTaken.isEmpty()) return 0
    val meanTimeBonus = guessSecondsTaken.map { timeBonus(it, turnSeconds) }.average()
    val points = 250.0 * (guessSecondsTaken.size.toDouble() / totalGuessers) * meanTimeBonus
    return roundHalfUp(points)
}
