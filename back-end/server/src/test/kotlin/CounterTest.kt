package it.inkorporate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CounterTest {

    @Test
    fun `starts at 0`() {
        assertEquals(0, Counter().current())
    }

    @Test
    fun `increment adds 1 and returns the new value`() {
        val counter = Counter()
        assertEquals(1, counter.increment())
        assertEquals(2, counter.increment())
        assertEquals(2, counter.current())
    }

    @Test
    fun `no taps are lost when many arrive at once`() {
        val counter = Counter()
        // 100 "phones" tapping 100 times each, on several threads at once.
        runBlocking(Dispatchers.Default) {
            repeat(100) {
                launch { repeat(100) { counter.increment() } }
            }
        }
        assertEquals(10_000, counter.current())
    }
}
