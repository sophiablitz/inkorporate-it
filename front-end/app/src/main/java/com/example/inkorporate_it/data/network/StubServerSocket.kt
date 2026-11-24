package com.example.inkorporate_it.data.network

import it.inkorporate.protocol.ClientMessage
import it.inkorporate.protocol.Player
import it.inkorporate.protocol.Room
import it.inkorporate.protocol.RoomPhase
import it.inkorporate.protocol.ServerMessage
import it.inkorporate.protocol.decodeClient
import it.inkorporate.protocol.encodeServer
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
import kotlinx.serialization.SerializationException

/**
 * A pretend server that lives inside the app, so the team can play through the game
 * before the real server answers. InkorporateApp uses it while USE_STUB_SERVER is true.
 *
 * It speaks the real protocol: GameClient sends it the same JSON it would send the
 * server, and gets real server messages back. Nothing above GameSocket knows it's fake.
 *
 * What it does:
 * - create_room: you host a room with one bot ("Stub player")
 * - join_room: you join a room hosted by a bot ("Stub host"), with the code you typed
 * - start_game / next_round: a round starts; you and the bot take turns drawing.
 *   The word is always STUB_WORD. The timer counts down from TURN_SECONDS, then the round ends.
 * - chat: echoed back. Typing STUB_WORD while guessing scores a correct guess.
 * - return_to_lobby, leave_room, close_room: what the server would do
 *
 * Not a real server: one room, no checks for who is host, no other phones.
 */
class StubServerSocket(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : GameSocket {

    private val _status = MutableStateFlow(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val toApp = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incoming: Flow<String> = toApp.asSharedFlow()

    private var room: Room? = null
    private var timer: Job? = null

    override fun connect() {
        _status.value = ConnectionStatus.Connected
    }

    override suspend fun send(text: String): Boolean {
        if (_status.value != ConnectionStatus.Connected) return false
        val message = try {
            decodeClient(text)
        } catch (e: SerializationException) {
            return true // the real server would answer bad_message; the stub just ignores it
        }
        answer(message)
        return true
    }

    override fun close() {
        timer?.cancel()
        room = null
        _status.value = ConnectionStatus.Disconnected
    }

    @Synchronized // the timer ends rounds on another thread
    private fun answer(message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateRoom -> {
                val bot = Player(BOT, "Stub player")
                val room = newRoom("123456", hostId = ME, Player(ME, message.name, message.avatar), bot)
                reply(ServerMessage.RoomJoined(ME, room))
            }

            is ClientMessage.JoinRoom -> {
                val bot = Player(BOT, "Stub host")
                val room = newRoom(message.code, hostId = BOT, bot, Player(ME, message.name, message.avatar))
                reply(ServerMessage.RoomJoined(ME, room))
            }

            ClientMessage.StartGame, ClientMessage.NextRound -> startRound()

            is ClientMessage.Chat -> chat(message.text)

            ClientMessage.ReturnToLobby -> {
                timer?.cancel()
                val current = room ?: return
                val lobby = current.copy(
                    phase = RoomPhase.LOBBY, round = 0, drawerId = null, guessedIds = emptyList(),
                    players = current.players.map { it.copy(score = 0) },
                )
                reply(ServerMessage.RoomUpdate(lobby))
            }

            ClientMessage.LeaveRoom, ClientMessage.CloseRoom -> {
                timer?.cancel()
                if (room != null) reply(ServerMessage.RoomClosed("The room closed."))
                room = null
            }

            // The server never sends the drawer's own lines back, so neither does the stub.
            is ClientMessage.DrawLine, ClientMessage.ClearCanvas -> Unit
        }
    }

    private fun newRoom(code: String, hostId: String, vararg players: Player) = Room(
        code = code,
        phase = RoomPhase.LOBBY,
        hostId = hostId,
        players = players.toList(),
        totalRounds = TOTAL_ROUNDS,
    )

    /** Next round: the next player in join order draws. After the last round, the game ends. */
    private fun startRound() {
        val current = room ?: return
        if (current.round >= current.totalRounds) {
            reply(ServerMessage.GameEnd(current.copy(phase = RoomPhase.GAME_OVER, drawerId = null)))
            return
        }
        val round = current.round + 1
        val drawer = current.players[(round - 1) % current.players.size]
        val next = current.copy(
            phase = RoomPhase.DRAWING, round = round, drawerId = drawer.playerId, guessedIds = emptyList(),
        )
        reply(
            ServerMessage.RoundStart(
                room = next,
                wordLength = STUB_WORD.length,
                turnSeconds = TURN_SECONDS,
                word = if (drawer.playerId == ME) STUB_WORD else null,
            )
        )
        timer?.cancel()
        timer = scope.launch {
            for (seconds in TURN_SECONDS - 1 downTo 0) {
                delay(1_000)
                reply(ServerMessage.TimeLeft(seconds))
            }
            endRound()
        }
    }

    private fun chat(text: String) {
        val current = room ?: return
        val guessing = current.phase == RoomPhase.DRAWING && current.drawerId != ME && ME !in current.guessedIds
        if (guessing && text.trim().equals(STUB_WORD, ignoreCase = true)) {
            // A correct guess is never sent as chat, so the word can't leak.
            reply(ServerMessage.CorrectGuess(ME))
            reply(ServerMessage.RoomUpdate(current.copy(guessedIds = current.guessedIds + ME)))
            endRound() // everyone (just you) has guessed
        } else {
            reply(ServerMessage.Chat(ME, text))
        }
    }

    /** Scores the round: GUESS_POINTS for each guesser who got it, and for the drawer if anyone did. */
    @Synchronized
    private fun endRound() {
        timer?.cancel()
        val current = room ?: return
        if (current.phase != RoomPhase.DRAWING) return
        val scores = buildMap {
            current.guessedIds.forEach { put(it, GUESS_POINTS) }
            if (current.guessedIds.isNotEmpty()) current.drawerId?.let { put(it, GUESS_POINTS / 2) }
        }
        val ended = current.copy(
            phase = RoomPhase.ROUND_OVER,
            players = current.players.map { it.copy(score = it.score + (scores[it.playerId] ?: 0)) },
        )
        reply(ServerMessage.RoundEnd(word = STUB_WORD, roundScores = scores, room = ended))
    }

    /** Sends [message] to the app, and remembers the room it carries. */
    private fun reply(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomJoined -> room = message.room
            is ServerMessage.RoomUpdate -> room = message.room
            is ServerMessage.RoundStart -> room = message.room
            is ServerMessage.RoundEnd -> room = message.room
            is ServerMessage.GameEnd -> room = message.room
            else -> Unit
        }
        toApp.tryEmit(encodeServer(message))
    }

    companion object {
        const val STUB_WORD = "barn"
        const val TURN_SECONDS = 30
        const val TOTAL_ROUNDS = 2
        private const val GUESS_POINTS = 100
        private const val ME = "you"
        private const val BOT = "bot"
    }
}
