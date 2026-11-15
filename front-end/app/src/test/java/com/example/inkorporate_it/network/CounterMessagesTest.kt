package com.example.inkorporate_it.network

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Pins the exact JSON for the counter messages. The server has the same test,
 * so if either copy changes a name or a field, one of the two tests fails.
 */
class CounterMessagesTest {

    @Test
    fun counterIncrement_encodesToTheAgreedJson() {
        val json = CounterJson.encodeToString(
            CounterClientMessage.serializer(),
            CounterClientMessage.CounterIncrement
        )
        assertEquals("""{"type":"counter_increment"}""", json)
    }

    @Test
    fun counterIncrement_decodesFromTheAgreedJson() {
        val msg = CounterJson.decodeFromString(
            CounterClientMessage.serializer(),
            """{"type":"counter_increment"}"""
        )
        assertEquals(CounterClientMessage.CounterIncrement, msg)
    }

    @Test
    fun counterGet_encodesToTheAgreedJson() {
        val json = CounterJson.encodeToString(
            CounterClientMessage.serializer(),
            CounterClientMessage.CounterGet
        )
        assertEquals("""{"type":"counter_get"}""", json)
    }

    @Test
    fun counterGet_decodesFromTheAgreedJson() {
        val msg = CounterJson.decodeFromString(
            CounterClientMessage.serializer(),
            """{"type":"counter_get"}"""
        )
        assertEquals(CounterClientMessage.CounterGet, msg)
    }

    @Test
    fun counterValue_encodesToTheAgreedJson() {
        val json = CounterJson.encodeToString(
            CounterServerMessage.serializer(),
            CounterServerMessage.CounterValue(3)
        )
        assertEquals("""{"type":"counter_value","value":3}""", json)
    }

    @Test
    fun counterValue_decodesFromTheAgreedJson() {
        val msg = CounterJson.decodeFromString(
            CounterServerMessage.serializer(),
            """{"type":"counter_value","value":3}"""
        )
        assertEquals(CounterServerMessage.CounterValue(3), msg)
    }

    @Test
    fun anUnknownExtraField_isIgnored() {
        val msg = CounterJson.decodeFromString(
            CounterServerMessage.serializer(),
            """{"type":"counter_value","value":3,"addedLater":true}"""
        )
        assertEquals(CounterServerMessage.CounterValue(3), msg)
    }
}
