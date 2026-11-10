package it.inkorporate.server

/**
 * Turns for one room: word choice, the turn timer, drawer rotation, rounds and game end.
 * Owner: Section 2 (Timer and drawer rotation).
 * Sends: drawer_choosing, word_options, turn_start, turn_end, game_end.
 * See the back-end plan, "How the files connect".
 */
class TurnManager(val room: GameRoom) {
    /** Null in the lobby and between turns. */
    val currentDrawer: PlayerConnection? get() = TODO("Section 2")

    /** Never sent to guessers; only the drawer's turn_start carries it. */
    val currentWord: String? get() = TODO("Section 2")

    /** When the current turn began, in ms. Guessing uses it to score by time. */
    val turnStartedAt: Long get() = TODO("Section 2")

    /** CanvasState uses this for the NOT_DRAWER check. */
    fun isDrawer(player: PlayerConnection): Boolean = TODO("Section 2")

    /** Called by GameRoom.onStartGame after the host and player-count checks pass. */
    suspend fun startGame(): Unit = TODO("Section 2")

    /** choose_word from the drawer; auto-pick after 10s if it never comes. */
    suspend fun onChooseWord(player: PlayerConnection, index: Int): Unit = TODO("Section 2")

    /** Called by Guessing when every non-drawer has guessed: end the turn early. */
    suspend fun onEveryoneGuessed(): Unit = TODO("Section 2")

    /** If [player] is the drawer mid-turn, end the turn and move on. */
    suspend fun onPlayerLeft(player: PlayerConnection): Unit = TODO("Section 2")
}
