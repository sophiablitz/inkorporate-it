package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Point
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.RoomSettings
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.StrokePart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A pretend server that lives inside the app, so every screen can be built and tried
 * without running the real one. InkorporateApp uses it while USE_STUB_SERVER is true.
 * Adapted from Sophia's StubServerSocket on front-end/stub-server-socket.
 *
 * It answers with the real protocol's messages, in the same order the real server sends
 * them (see the turn walkthrough at the top of shared/'s Protocol.kt), so nothing above
 * GameSocket knows it's fake. Its timers are shorter than the real game's (a whole game takes
 * about two minutes), and hurryTimer() cuts any wait short.
 *
 * The room always has you and two bots, "Stub player 1" and "Stub player 2":
 * - create_room: you host. join_room: "Stub player 1" hosts, with the code you typed.
 * - start_game: everyone draws once per round, in join order. The word is always STUB_WORD.
 *   Your turn: your_word, then Start (or SECONDS_TO_START). A bot's turn: it starts after
 *   a second and draws a little house.
 * - The bots chat like players: they say hi in the lobby, and in every turn they aren't
 *   drawing they send a wrong guess or two (plain chat) before guessing right a few
 *   seconds in, so every turn has chat and points to show. Typing STUB_WORD while
 *   guessing scores you too.
 * - Points are fixed and easy to read: 150, 110, then 90 for the 1st, 2nd and 3rd guesser,
 *   and 60 to the drawer for each player who guessed.
 * - next_turn, return_to_lobby, leave_room, close_room: what the real server does.
 * - time_left: once a second for every countdown (looking at the word, drawing, the
 *   results), so every screen can show how long is left.
 * - hurryTimer(): brings whatever is counting down (looking at the word, drawing, the
 *   results) to its last second. The placeholder screens' buttons use it
 *   (GameViewModel.stubSetPhase), and a real "skip" button could call it the same way.
 *
 * Not a real server: one room, no checks for who's allowed, no other phones. The real
 * game rules are in back-end/server's TurnManager.
 */
class StubServerSocket(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val settings: RoomSettings = RoomSettings(rounds = 2, turnSeconds = TURN_SECONDS),
    private val millisPerSecond: Long = 1_000,
) : GameSocket {

    private val _status = MutableStateFlow(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val toApp = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val incoming: Flow<ServerMessage> = toApp.asSharedFlow()

    private var room: Room? = null

    /** Everything timed that belongs to the current step (timer, bot guesses), so a new step can cancel it all. */
    private val timers = mutableListOf<Job>()

    /** How many seconds the current countdown has left, or 0 when nothing is counting down. */
    private var secondsLeftInStep = 0

    override fun connect() {
        _status.value = ConnectionStatus.Connected
    }

    override suspend fun send(message: ClientMessage): Boolean {
        if (_status.value != ConnectionStatus.Connected) return false
        answer(message)
        return true
    }

    override fun close() {
        cancelTimers()
        room = null
        _status.value = ConnectionStatus.Disconnected
    }

    /**
     * Brings whatever is counting down to its last second: the drawer looking at their
     * word, the drawing timer, or the results. A second later, that step ends the normal
     * way. Does nothing when nothing is counting down (the lobby, the end of the game).
     */
    @Synchronized
    fun hurryTimer() {
        if (secondsLeftInStep <= 1) return
        secondsLeftInStep = 1
        reply(ServerMessage.TimeLeft(1))
    }

    @Synchronized // the timers run on another thread
    private fun answer(message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateRoom -> {
                reply(ServerMessage.RoomJoined(ME, newRoom("123456", ME, Player(ME, message.name, message.avatar), BOT_PLAYER, GUESSER_PLAYER)))
                greetInLobby()
            }

            is ClientMessage.JoinRoom -> {
                reply(ServerMessage.RoomJoined(ME, newRoom(message.code, BOT, BOT_PLAYER, Player(ME, message.name, message.avatar), GUESSER_PLAYER)))
                greetInLobby()
            }

            ClientMessage.StartGame -> room?.let {
                room = it.copy(round = 1)
                nextTurn() // the first turn's room_update tells the app the game started
            }
            ClientMessage.StartDrawing -> if (room?.phase == RoomPhase.GETTING_READY && room?.drawerId == ME) beginDrawing()
            ClientMessage.NextTurn -> if (room?.phase == RoomPhase.TURN_OVER) nextTurn()
            is ClientMessage.Chat -> chat(message.text.trim())
            ClientMessage.ReturnToLobby -> backToLobby()

            ClientMessage.LeaveRoom, ClientMessage.CloseRoom -> {
                cancelTimers()
                if (room != null) reply(ServerMessage.RoomClosed("The room closed."))
                room = null
            }

            // The server never sends the drawer's own strokes back, so neither does the stub.
            is ClientMessage.Stroke, ClientMessage.Undo, ClientMessage.ClearCanvas -> Unit
        }
    }

    private fun newRoom(code: String, hostId: String, vararg players: Player) = Room(
        code = code,
        phase = RoomPhase.LOBBY,
        hostId = hostId,
        players = players.toList(),
        totalRounds = settings.rounds,
        settings = settings,
    )

    /** The next drawer in join order. After everyone has drawn, the next round, or the end. */
    private fun nextTurn() {
        cancelTimers()
        val current = room ?: return
        val drawerIndex = current.players.indexOfFirst { it.playerId == current.drawerId }
        val nextIndex = drawerIndex + 1 // -1 + 1 = the first player, for the first turn
        val round = if (nextIndex >= current.players.size) current.round + 1 else current.round
        if (round > settings.rounds) return endGame()

        val drawer = current.players[nextIndex % current.players.size]
        reply(ServerMessage.RoomUpdate(current.copy(
            phase = RoomPhase.GETTING_READY, round = round, drawerId = drawer.playerId, guessedIds = emptyList(),
        )))
        if (drawer.playerId == ME) reply(ServerMessage.YourWord(STUB_WORD, SECONDS_TO_START))
        val secondsToLook = if (drawer.playerId == ME) SECONDS_TO_START else BOT_SECONDS_TO_START
        countDown(secondsToLook) { if (room?.phase == RoomPhase.GETTING_READY) beginDrawing() }
    }

    private fun beginDrawing() {
        cancelTimers()
        val current = room ?: return
        reply(ServerMessage.TurnStart(current.copy(phase = RoomPhase.DRAWING), STUB_WORD.length, settings.turnSeconds))
        val botDraws = current.drawerId != ME

        countDown(
            settings.turnSeconds,
            eachSecond = { secondsLeft ->
                val stroke = HOUSE.getOrNull(settings.turnSeconds - 1 - secondsLeft)
                if (botDraws && stroke != null) reply(ServerMessage.Stroke(stroke))
            },
        ) { endTurn() }
        // Each bot that isn't drawing sends a wrong guess or two, then guesses right a
        // little later than the bot before it.
        current.players.filter { it.playerId != ME && it.playerId != current.drawerId }
            .forEachIndexed { i, bot ->
                WRONG_GUESSES[i % WRONG_GUESSES.size].forEachIndexed { n, guess ->
                    after(1 + i + n * 2) { botSays(bot.playerId, guess, onlyIn = RoomPhase.DRAWING) }
                }
                after(BOT_GUESS_SECONDS + i * 2) { correctGuess(bot.playerId) }
            }
    }

    /** The bots say hi in the lobby, a second apart. */
    private fun greetInLobby() {
        after(1) { botSays(BOT, "hi!", onlyIn = RoomPhase.LOBBY) }
        after(2) { botSays(GUESSER_PLAYER.playerId, "ready when you are", onlyIn = RoomPhase.LOBBY) }
    }

    /** A bot's chat message, as the server would relay it, if the room is still in [onlyIn]. */
    private fun botSays(botId: String, text: String, onlyIn: RoomPhase) {
        if (room?.phase == onlyIn) reply(ServerMessage.Chat(botId, text))
    }

    private fun chat(text: String) {
        val current = room ?: return
        if (text.isEmpty()) return
        val guessing = current.phase == RoomPhase.DRAWING && current.drawerId != ME && ME !in current.guessedIds
        when {
            // A correct guess is never sent as chat, so the word can't leak.
            guessing && text.equals(STUB_WORD, ignoreCase = true) -> correctGuess(ME)
            current.phase == RoomPhase.DRAWING && current.drawerId == ME -> Unit // the drawer can't chat
            else -> reply(ServerMessage.Chat(ME, text))
        }
    }

    private fun correctGuess(playerId: String) {
        val current = room ?: return
        if (current.phase != RoomPhase.DRAWING || playerId in current.guessedIds) return
        reply(ServerMessage.CorrectGuess(playerId))
        val updated = current.copy(guessedIds = current.guessedIds + playerId)
        reply(ServerMessage.RoomUpdate(updated))
        val everyoneGuessed = updated.players.all { it.playerId == updated.drawerId || it.playerId in updated.guessedIds }
        if (everyoneGuessed) endTurn()
    }

    /** The fixed points (see the class comment), then the results, then the next turn by itself. */
    private fun endTurn() {
        cancelTimers()
        val current = room ?: return
        if (current.phase != RoomPhase.DRAWING && current.phase != RoomPhase.GETTING_READY) return
        val scores = buildMap {
            current.guessedIds.forEachIndexed { rank, id -> put(id, GUESS_POINTS.getOrElse(rank) { GUESS_POINTS.last() }) }
            current.drawerId?.let { put(it, DRAWER_POINTS_PER_GUESS * current.guessedIds.size) }
        }
        val ended = current.copy(
            phase = RoomPhase.TURN_OVER,
            players = current.players.map { it.copy(score = it.score + (scores[it.playerId] ?: 0)) },
        )
        reply(ServerMessage.TurnEnd(word = STUB_WORD, turnScores = scores, room = ended))
        countDown(RESULTS_SECONDS) { if (room?.phase == RoomPhase.TURN_OVER) nextTurn() }
    }

    private fun endGame() {
        cancelTimers()
        val current = room ?: return
        reply(ServerMessage.GameEnd(current.copy(phase = RoomPhase.GAME_OVER, drawerId = null, round = minOf(current.round, settings.rounds))))
    }

    private fun backToLobby() {
        cancelTimers()
        val current = room ?: return
        reply(ServerMessage.RoomUpdate(current.copy(
            phase = RoomPhase.LOBBY, round = 0, drawerId = null, guessedIds = emptyList(),
            players = current.players.map { it.copy(score = 0) },
        )))
    }

    /**
     * Counts [seconds] down, one tick per second, then runs [done]. [eachSecond] gets the
     * seconds left after each tick. hurryTimer() can cut it short by lowering
     * secondsLeftInStep, which this loop reads every tick.
     */
    private fun countDown(seconds: Int, eachSecond: (Int) -> Unit = {}, done: () -> Unit) {
        secondsLeftInStep = seconds
        reply(ServerMessage.TimeLeft(seconds)) // every countdown is shown, from its first second
        timers += scope.launch {
            while (true) {
                delay(millisPerSecond)
                synchronized(this@StubServerSocket) {
                    secondsLeftInStep--
                    reply(ServerMessage.TimeLeft(secondsLeftInStep))
                    eachSecond(secondsLeftInStep)
                    if (secondsLeftInStep <= 0) {
                        done()
                        return@launch
                    }
                }
            }
        }
    }

    private fun after(seconds: Int, block: () -> Unit) {
        timers += scope.launch {
            delay(seconds * millisPerSecond)
            synchronized(this@StubServerSocket) { block() }
        }
    }

    private fun cancelTimers() {
        timers.forEach { it.cancel() }
        timers.clear()
        secondsLeftInStep = 0
    }

    /** Sends [message] to the app, and remembers the room it carries. */
    private fun reply(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomJoined -> room = message.room
            is ServerMessage.RoomUpdate -> room = message.room
            is ServerMessage.TurnStart -> room = message.room
            is ServerMessage.TurnEnd -> room = message.room
            is ServerMessage.GameEnd -> room = message.room
            else -> Unit
        }
        toApp.tryEmit(message)
    }

    companion object {
        const val STUB_WORD = "barn"

        // Shorter than the real game's, so every screen comes round quickly, and hurryTimer()
        // skips the rest of any wait. The real game uses
        // SECONDS_TO_START (10), an 80-second turn and TURN_RESULTS_SECONDS (8).
        const val SECONDS_TO_START = 5
        const val TURN_SECONDS = 20
        const val RESULTS_SECONDS = 6
        private const val BOT_SECONDS_TO_START = 2
        private const val BOT_GUESS_SECONDS = 3

        /** Each bot's wrong guesses, sent as plain chat before it gets the word. */
        private val WRONG_GUESSES = listOf(listOf("house?"), listOf("a box", "garage?"))

        /** Points for the 1st, 2nd and 3rd guesser, and the drawer's points per player who guessed. */
        val GUESS_POINTS = listOf(150, 110, 90)
        const val DRAWER_POINTS_PER_GUESS = 60

        private const val ME = "you"
        private const val BOT = "bot"
        private val BOT_PLAYER = Player(BOT, "Stub player 1")
        private val GUESSER_PLAYER = Player("guesser", "Stub player 2")

        /** The bot's drawing, one stroke a second: walls, then a roof, then a door. */
        private val HOUSE = listOf(
            listOf(0.25f to 0.75f, 0.25f to 0.45f, 0.75f to 0.45f, 0.75f to 0.75f, 0.25f to 0.75f),
            listOf(0.20f to 0.47f, 0.50f to 0.20f, 0.80f to 0.47f),
            listOf(0.45f to 0.75f, 0.45f to 0.60f, 0.55f to 0.60f, 0.55f to 0.75f),
        ).mapIndexed { i, points ->
            StrokePart(strokeId = i + 1, points = points.map { (x, y) -> Point(x, y) }, color = "#3B4FE0")
        }
    }
}
