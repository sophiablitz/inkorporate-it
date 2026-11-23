package it.inkorporate

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Plain unit tests: no server, no sockets. RoomRegistry is ordinary Kotlin, so
// we just make one and call it.
class RoomRegistryTest {

    // ---------- randomRoomCode ----------

    @Test
    fun `a room code is six digits`() {
        // With 1000 codes, about 100 start with 0, so this also covers leading zeros.
        repeat(1000) {
            val code = RoomRegistry.randomRoomCode()
            assertEquals(6, code.length, "wrong length: $code")
            assertTrue(code.all { it.isDigit() }, "not all digits: $code")
        }
    }

    // ---------- create ----------

    @Test
    fun `created rooms get different codes`() {
        val registry = RoomRegistry()
        val codes = List(1000) { registry.create().code }
        assertEquals(codes.size, codes.toSet().size)
    }

    // ---------- find ----------

    @Test
    fun `find returns the room that create made`() {
        val registry = RoomRegistry()
        val room = registry.create()
        assertSame(room, registry.find(room.code))
    }

    @Test
    fun `find returns null for an unknown code`() {
        assertNull(RoomRegistry().find("676767"))
    }

    // ---------- remove ----------

    @Test
    fun `find returns null after remove`() {
        val registry = RoomRegistry()
        val room = registry.create()
        registry.remove(room)
        assertNull(registry.find(room.code))
    }

    @Test
    fun `removing an old room does not delete a new room with the same code`() {
        val registry = RoomRegistry()
        val current = registry.create()
        val stale = GameRoom(current.code)   // same code, different room

        registry.remove(stale)

        assertSame(current, registry.find(current.code))
    }

    // ---------- many phones at once ----------

    @Test
    fun `rooms created at the same moment never share a code`() {
        val registry = RoomRegistry()

        // 8 threads, each one a phone tapping Create 1000 times at the same moment.
        // 8000 rooms out of 1,000,000 codes means about 30 collisions, so the
        // "code taken, try another" path really runs while other threads write.
        List(8) { thread { repeat(1000) { registry.create() } } }.forEach { it.join() }

        // If two creates had overwritten each other, there would be fewer than 8000 rooms.
        assertEquals(8000, registry.size)
    }
}
