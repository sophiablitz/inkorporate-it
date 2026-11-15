package it.inkorporate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * ⚠️ TEMPORARY COPY. The same messages are also in the app:
 *    front-end/app/src/main/java/com/example/inkorporate_it/network/CounterMessages.kt
 * Change both, or the phone and the server stop understanding each other.
 * Delete both copies when #16 moves these into shared/.
 *
 * Test-only messages for the server test screen (#139). They are kept apart
 * from protocol v0 (#11) so the real protocol doesn't change.
 */

/** Same settings as ProtocolJson in protocol v0, so the "type" field works the same way. */
val CounterJson = Json {
    classDiscriminator = "type"   // puts @SerialName into a "type" field
    ignoreUnknownKeys = true      // an older app won't crash on a newer field
    explicitNulls = false         // null fields are left out of the JSON
    encodeDefaults = true
}

/** Phone to server. */
@Serializable
sealed class CounterClientMessage {

    /** The player tapped the button. JSON: {"type":"counter_increment"} */
    @Serializable @SerialName("counter_increment")
    data object CounterIncrement : CounterClientMessage()
}

/** Server to phone. */
@Serializable
sealed class CounterServerMessage {

    /** The counter's new value. JSON: {"type":"counter_value","value":3} */
    @Serializable @SerialName("counter_value")
    data class CounterValue(val value: Int) : CounterServerMessage()
}
