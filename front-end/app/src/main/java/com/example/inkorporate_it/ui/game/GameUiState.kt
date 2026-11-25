package com.example.inkorporate_it.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.inkorporate_it.model.Avatar

/*
 * Everything GameScreen draws, in one read-only snapshot.
 *
 * GameViewModel builds a new copy whenever something changes; GameScreen only reads it.
 * Every field has a default, so GameUiState() is a valid "nothing yet" state and
 * previews only set the fields they care about.
 *
 * The fields are already worked out for the screen (isMyTurnToDraw, wordHint),
 * so composables never have to compare player ids or hide the word themselves.
 *
 * NOT FINAL: GamePlayer, Stroke and ChatMessage are UI-side placeholders.
 * The shared protocol models (shared/.../protocol/Models.kt) are still empty;
 * once they exist, GameViewModel converts them into these types.
 */
data class GameUiState(
    val phase: GamePhase = GamePhase.Waiting,
    val roomCode: String = "",
    val isHost: Boolean = false,        // true if this phone created the room
    val players: List<GamePlayer> = emptyList(),
    val isMyTurnToDraw: Boolean = true,
    val wordHint: String = "",          // "_ _ _ _ _" for guessers, the whole word for the drawer
    val secondsLeft: Int = 0,
    val round: Int = 0,
    val totalRounds: Int = 0,
    val strokes: List<Stroke> = emptyList(),
    val chat: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
)

/** Where the game is. Most of the screen stays the same in every phase. */
enum class GamePhase { Waiting, Drawing, RoundOver, GameOver }

/** One player as the game screen shows them. */
data class GamePlayer(
    val name: String,
    val avatar: Avatar = Avatar(),
    val score: Int = 0,
    val isHost: Boolean = false,
    val isDrawing: Boolean = false,
    val hasGuessed: Boolean = false,
)

/** One continuous line on the canvas: the points a finger passed through. */
data class Stroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float = 8f,
)

/** One line in the chat. */
data class ChatMessage(
    val sender: String,
    val text: String,
)

val previewPlayers = listOf(
    GamePlayer(name = "Alejandro", score = 120, isDrawing = true),
    GamePlayer(name = "Jerry", score = 90, hasGuessed = true),
    GamePlayer(name = "Jorge", score = 40, isHost = true),
    GamePlayer(name = "Ismael", score = 0),
)

val previewStrokes = listOf(
    // A rough house: walls, then a roof.
    Stroke(
        points = listOf(
            Offset(200f, 500f),
            Offset(200f, 300f),
            Offset(500f, 300f),
            Offset(500f, 500f),
            Offset(200f, 500f)
        ),
        color = Color(0xFF3B4FE0),
    ),
    Stroke(
        points = listOf(Offset(170f, 310f), Offset(350f, 160f), Offset(530f, 310f)),
        color = Color(0xFFA626A2),
        width = 12f,
    ),
)

val previewChat = listOf(
    ChatMessage(sender = "Jorge", text = "box?"),
    ChatMessage(sender = "Ismael", text = "barn"),
    ChatMessage(sender = "Jerry", text = "Guessed it correctly"),
)

val previewGuesserState = GameUiState(
    phase = GamePhase.Drawing,
    roomCode = "123456",
    isHost = true,
    players = previewPlayers,
    isMyTurnToDraw = false,
    wordHint = "_ _ _ _ _",
    secondsLeft = 42,
    round = 2,
    totalRounds = 4,
    strokes = previewStrokes,
    chat = previewChat,
)
