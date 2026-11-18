package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * One message, end to end: the server test screen's counter (#139).
 *
 * Copy this pattern for a new feature:
 *   1. Define the messages here in shared/, once. The app and the server both use them.
 *   2. Turn them into JSON and back only through the encode/decode functions below.
 *      Nobody writes JSON by hand: kotlinx.serialization generates it from @Serializable,
 *      using ProtocolJson's rules.
 *   3. The server handles them in back-end/server (Sockets.kt).
 *   4. The app sends them from GameClient's menu, and screens call that menu.
 *
 * These are kept as their own sealed classes, apart from the game's ClientMessage and
 * ServerMessage, so counter_increment never looks like part of the game.
 */

/** Phone to server. */
@Serializable
sealed class CounterClientMessage {

    /** The player tapped the button. JSON: {"type":"counter_increment"} */
    @Serializable @SerialName("counter_increment")
    data object CounterIncrement : CounterClientMessage()

    /** Asks for the current value without changing it. JSON: {"type":"counter_get"} */
    @Serializable @SerialName("counter_get")
    data object CounterGet : CounterClientMessage()
}

/** Server to phone. */
@Serializable
sealed class CounterServerMessage {

    /** The counter's value. JSON: {"type":"counter_value","value":3} */
    @Serializable @SerialName("counter_value")
    data class CounterValue(val value: Int) : CounterServerMessage()
}

// The codec: the only place these messages turn into JSON and back.
// The decode functions throw kotlinx.serialization.SerializationException
// on broken JSON or an unknown "type".

/** App side: a message to send. */
fun encodeCounterClient(message: CounterClientMessage): String =
    ProtocolJson.encodeToString(CounterClientMessage.serializer(), message)

/** Server side: a message that arrived from a phone. */
fun decodeCounterClient(text: String): CounterClientMessage =
    ProtocolJson.decodeFromString(CounterClientMessage.serializer(), text)

/** Server side: a reply to send. */
fun encodeCounterServer(message: CounterServerMessage): String =
    ProtocolJson.encodeToString(CounterServerMessage.serializer(), message)

/** App side: a reply that arrived from the server. */
fun decodeCounterServer(text: String): CounterServerMessage =
    ProtocolJson.decodeFromString(CounterServerMessage.serializer(), text)
