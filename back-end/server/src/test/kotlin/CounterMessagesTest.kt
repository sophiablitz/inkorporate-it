package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Pins the exact JSON for the counter messages. The app has the same test,
 * so if either copy changes a name or a field, one of the two tests fails.
 */
class CounterMessagesTest {

    @Test
    fun `counter_increment encodes to the agreed JSON`() {
        val json = CounterJson.encodeToString(
            CounterClientMessage.serializer(),
            CounterClientMessage.CounterIncrement
        )
        assertEquals("""{"type":"counter_increment"}""", json)
    }

    @Test
    fun `counter_increment decodes from the agreed JSON`() {
        val msg = CounterJson.decodeFromString(
            CounterClientMessage.serializer(),
            """{"type":"counter_increment"}"""
        )
        assertEquals(CounterClientMessage.CounterIncrement, msg)
    }

    @Test
    fun `counter_get encodes to the agreed JSON`() {
        val json = CounterJson.encodeToString(
            CounterClientMessage.serializer(),
            CounterClientMessage.CounterGet
        )
        assertEquals("""{"type":"counter_get"}""", json)
    }

    @Test
    fun `counter_get decodes from the agreed JSON`() {
        val msg = CounterJson.decodeFromString(
            CounterClientMessage.serializer(),
            """{"type":"counter_get"}"""
        )
        assertEquals(CounterClientMessage.CounterGet, msg)
    }

    @Test
    fun `counter_value encodes to the agreed JSON`() {
        val json = CounterJson.encodeToString(
            CounterServerMessage.serializer(),
            CounterServerMessage.CounterValue(3)
        )
        assertEquals("""{"type":"counter_value","value":3}""", json)
    }

    @Test
    fun `counter_value decodes from the agreed JSON`() {
        val msg = CounterJson.decodeFromString(
            CounterServerMessage.serializer(),
            """{"type":"counter_value","value":3}"""
        )
        assertEquals(CounterServerMessage.CounterValue(3), msg)
    }

    @Test
    fun `an unknown extra field is ignored`() {
        val msg = CounterJson.decodeFromString(
            CounterServerMessage.serializer(),
            """{"type":"counter_value","value":3,"addedLater":true}"""
        )
        assertEquals(CounterServerMessage.CounterValue(3), msg)
    }
}
