package it.inkorporate

import java.util.concurrent.atomic.AtomicInteger

/**
 * The server test screen's counter (#139). One per server, shared by every phone.
 *
 * AtomicInteger makes increment() safe when several phones tap at the same moment:
 * each tap gets its own number, and none are lost.
 */
class Counter {
    private val value = AtomicInteger(0)

    /** The current value, without changing it. */
    fun current(): Int = value.get()

    /** Adds 1 and returns the new value. */
    fun increment(): Int = value.incrementAndGet()
}
