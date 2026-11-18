package it.inkorporate.protocol

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins the exact JSON for the counter example. The app and the server both use
 * this one file, so these tests cover both sides at once.
 */
class CounterExampleTest {

    @Test
    fun `counter_increment encodes to the agreed JSON`() {
        assertEquals("""{"type":"counter_increment"}""", encodeCounterClient(CounterClientMessage.CounterIncrement))
    }

    @Test
    fun `counter_increment decodes from the agreed JSON`() {
        assertEquals(CounterClientMessage.CounterIncrement, decodeCounterClient("""{"type":"counter_increment"}"""))
    }

    @Test
    fun `counter_get encodes to the agreed JSON`() {
        assertEquals("""{"type":"counter_get"}""", encodeCounterClient(CounterClientMessage.CounterGet))
    }

    @Test
    fun `counter_get decodes from the agreed JSON`() {
        assertEquals(CounterClientMessage.CounterGet, decodeCounterClient("""{"type":"counter_get"}"""))
    }

    @Test
    fun `counter_value encodes to the agreed JSON`() {
        assertEquals("""{"type":"counter_value","value":3}""", encodeCounterServer(CounterServerMessage.CounterValue(3)))
    }

    @Test
    fun `counter_value decodes from the agreed JSON`() {
        assertEquals(CounterServerMessage.CounterValue(3), decodeCounterServer("""{"type":"counter_value","value":3}"""))
    }

    @Test
    fun `an unknown extra field is ignored`() {
        assertEquals(
            CounterServerMessage.CounterValue(3),
            decodeCounterServer("""{"type":"counter_value","value":3,"addedLater":true}"""),
        )
    }

    @Test
    fun `broken JSON or an unknown type throws SerializationException`() {
        assertFailsWith<SerializationException> { decodeCounterClient("not json at all") }
        assertFailsWith<SerializationException> { decodeCounterClient("""{"type":"something_else"}""") }
    }
}
