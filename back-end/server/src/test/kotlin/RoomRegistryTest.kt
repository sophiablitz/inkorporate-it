package it.inkorporate

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
            val code = randomRoomCode()
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

    @Test
    fun `create skips a code that is already taken`() {
        val codes = mutableListOf("111111", "111111", "222222")
        val registry = RoomRegistry(newCode = { codes.removeFirst() })

        assertEquals("111111", registry.create().code)
        assertEquals("222222", registry.create().code)
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
        val registry = RoomRegistry(newCode = { "111111" })
        val current = registry.create()
        val stale = GameRoom("111111")   // same code, different room

        registry.remove(stale)

        assertSame(current, registry.find("111111"))
    }
}
