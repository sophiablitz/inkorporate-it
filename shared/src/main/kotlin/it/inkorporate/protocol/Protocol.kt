package it.inkorporate.protocol

import kotlinx.serialization.json.Json

// Every message the phones and the server send each other.

/**
 * The one set of JSON rules for every message. The app and the server both use it,
 * so the same message always turns into the same text on both sides.
 */
val ProtocolJson = Json {
    classDiscriminator = "type"   // puts @SerialName into a "type" field
    ignoreUnknownKeys = true      // an older app won't crash on a newer field
    explicitNulls = false         // null fields are left out of the JSON
    encodeDefaults = true         // fields with default values are still sent
}
