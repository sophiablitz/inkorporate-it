package it.inkorporate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins what each ProtocolJson setting does, with a made-up message. */
class ProtocolJsonTest {

    @Serializable
    sealed class Sample {
        @Serializable @SerialName("sample")
        data class Hello(val name: String, val note: String? = null, val count: Int = 1) : Sample()
    }

    @Test
    fun `the message name goes in a type field`() {
        val json = ProtocolJson.encodeToString(Sample.serializer(), Sample.Hello("Ana"))
        assertEquals("""{"type":"sample","name":"Ana","count":1}""", json)
    }

    @Test
    fun `null fields are left out and defaults are still sent`() {
        val json = ProtocolJson.encodeToString(Sample.serializer(), Sample.Hello("Ana", note = null, count = 1))
        assertEquals("""{"type":"sample","name":"Ana","count":1}""", json)
    }

    @Test
    fun `fields this side doesn't know about are ignored`() {
        val message = ProtocolJson.decodeFromString(
            Sample.serializer(),
            """{"type":"sample","name":"Ana","count":1,"addedLater":true}""",
        )
        assertEquals(Sample.Hello("Ana"), message)
    }
}
