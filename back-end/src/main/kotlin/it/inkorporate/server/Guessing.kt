package it.inkorporate.server

/**
 * Guesses for one room: checking chat against the word, who guessed, and points.
 * Owner: Section 3 (Guessing and scoring).
 * Sends: chat, correct_guess (never the word).
 * See the back-end plan, "How the files connect".
 */
class Guessing(val room: GameRoom) {
    /** Forget last turn's guesses. TurnManager calls this at each turn start. */
    fun startTurn(): Unit = TODO("Section 3")

    /** Ignore the drawer and anyone who already guessed. Correct → correct_guess; wrong → chat. */
    suspend fun onChat(player: PlayerConnection, text: String): Unit = TODO("Section 3")

    /** playerId → points earned this turn, for TurnManager's turn_end. */
    fun turnScores(): Map<String, Int> = TODO("Section 3")
}

/** Points for the guesser who got it [rank]-th (1 = first). Follows the design doc's scoring table. */
fun pointsForGuess(rank: Int, secondsTaken: Int, turnSeconds: Int, playerCount: Int): Int = TODO("Section 3")

/** Points for the drawer, based on how many players guessed. */
fun pointsForDrawer(correctGuessers: Int, playerCount: Int): Int = TODO("Section 3")
