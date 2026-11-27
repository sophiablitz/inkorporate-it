package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.StrokePart

/**
 * One game, as this phone knows it. Built only from what the server said (plus the
 * drawer's own strokes, which the server doesn't send back to the drawer).
 * Every field has a default, so GameSession() is "not in a room".
 *
 * Adapted from Sophia's GameSession on front-end/game-client.
 */
data class GameSession(
    val myPlayerId: String? = null,                // which player in room.players is this phone
    val room: Room? = null,                        // null until room_joined, and after leaving
    val word: String? = null,                      // the secret word: the drawer's phone only, until turn_end
    val wordLength: Int = 0,                       // for the guessers' blanks
    val revealedWord: String? = null,              // the word, shown to everyone at turn_end
    val secondsToStart: Int = 0,                   // how long the drawer has to look at their word
    val secondsLeft: Int = 0,
    val strokes: List<StrokePart> = emptyList(),   // this turn's canvas, oldest first, one entry per stroke
    val chat: List<ChatEntry> = emptyList(),       // this turn's chat, oldest first
    val turnScores: Map<String, Int> = emptyMap(), // playerId to points from the last turn
    val closedReason: String? = null,              // set when the room closes
    val error: String? = null,                     // the last thing that went wrong, to show once
) {
    val phase: RoomPhase? get() = room?.phase
    val isMyTurnToDraw: Boolean get() = myPlayerId != null && room?.drawerId == myPlayerId

    /** The new session after [message]. Pure: no sending, so it's easy to test. */
    fun apply(message: ServerMessage): GameSession = when (message) {
        is ServerMessage.RoomJoined -> GameSession(myPlayerId = message.playerId, room = message.room)

        is ServerMessage.RoomUpdate -> when {
            message.room.phase == RoomPhase.LOBBY -> copy(
                // Back in the lobby: no turn, so nothing on the canvas.
                room = message.room, word = null, wordLength = 0, revealedWord = null,
                strokes = emptyList(), chat = emptyList(), turnScores = emptyMap(),
            )
            startsNewTurn(message.room) -> copy(
                // A new drawer is getting ready: forget the last turn. your_word comes next, for the drawer.
                room = message.room, word = null, wordLength = 0, revealedWord = null,
                secondsLeft = 0, strokes = emptyList(), chat = emptyList(), turnScores = emptyMap(),
            )
            else -> copy(room = message.room)
        }

        // secondsLeft starts at the drawer's viewing time; time_left counts it down.
        is ServerMessage.YourWord -> copy(word = message.word, secondsToStart = message.secondsToStart, secondsLeft = message.secondsToStart)

        is ServerMessage.TurnStart -> copy(
            room = message.room,
            wordLength = message.wordLength,
            secondsLeft = message.turnSeconds,
        )

        is ServerMessage.TimeLeft -> copy(secondsLeft = message.seconds)
        is ServerMessage.Stroke -> withStroke(message.part)
        ServerMessage.Undo -> copy(strokes = strokes.dropLast(1))
        ServerMessage.ClearCanvas -> copy(strokes = emptyList())
        is ServerMessage.Chat -> copy(chat = chat + ChatEntry(message.playerId, message.text))

        is ServerMessage.CorrectGuess ->
            copy(chat = chat + ChatEntry(playerId = null, text = "${nameOf(message.playerId)} guessed it!"))

        is ServerMessage.TurnEnd -> copy(
            room = message.room,
            word = null,
            revealedWord = message.word,
            secondsLeft = 0,
            turnScores = message.turnScores,
        )

        is ServerMessage.GameEnd -> copy(room = message.room, word = null, secondsLeft = 0)
        is ServerMessage.RoomClosed -> GameSession(closedReason = message.reason)
        is ServerMessage.ErrorMessage -> copy(error = message.message)
    }

    /** Adds [part] to the canvas: a new stroke, or the next part of one being drawn. */
    fun withStroke(part: StrokePart): GameSession {
        val index = strokes.indexOfFirst { it.strokeId == part.strokeId }
        if (index < 0) return copy(strokes = strokes + part)
        val joined = strokes[index].let { it.copy(points = it.points + part.points, done = part.done) }
        return copy(strokes = strokes.toMutableList().also { it[index] = joined })
    }

    /** A player's name, from their id. */
    fun nameOf(playerId: String?): String =
        room?.players?.firstOrNull { it.playerId == playerId }?.name ?: "Someone"

    /** Getting ready, with a different drawer from before (or after something else): a new turn. */
    private fun startsNewTurn(next: Room): Boolean =
        next.phase == RoomPhase.GETTING_READY &&
            (room?.phase != RoomPhase.GETTING_READY || room.drawerId != next.drawerId)
}

/** One line in the chat. playerId is null for the server's own notes ("Jerry guessed it!"). */
data class ChatEntry(
    val playerId: String?,
    val text: String,
)
