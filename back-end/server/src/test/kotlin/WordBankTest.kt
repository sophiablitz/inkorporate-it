package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordBankTest {

    private val sampleJson = """{"words":["apple","banana","cherry"]}"""
    private val sampleWords = setOf("apple", "banana", "cherry")

    // --- constructor ---

    @Test
    fun `constructor seeds the available words`() {
        val bank = WordBank(listOf("apple", "banana", "cherry"))
        assertEquals(sampleWords, bank.updateList())
    }

    @Test
    fun `constructor defaults to an empty word bank`() {
        val bank = WordBank()
        assertTrue(bank.updateList().isEmpty())
    }

    @Test
    fun `constructor de-duplicates seed words`() {
        val bank = WordBank(listOf("dup", "dup", "unique"))
        assertEquals(setOf("dup", "unique"), bank.updateList())
    }

    // --- getCurrentWord ---

    @Test
    fun `getCurrentWord is null before any word is chosen`() {
        val bank = WordBank()
        assertNull(bank.getCurrentWord())
    }

    @Test
    fun `getCurrentWord returns the most recently chosen word`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        val chosen = bank.chooseWord()
        assertEquals(chosen, bank.getCurrentWord())
    }

    // --- loadJson ---

    @Test
    fun `loadJson loads all words into the available set`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        // every loaded word must be present in the available set
        assertEquals(sampleWords, bank.updateList())
    }

    @Test
    fun `loadJson with empty words list adds nothing`() {
        val bank = WordBank()
        bank.loadJson("""{"words":[]}""")

        assertTrue(bank.updateList().isEmpty())
    }

    @Test
    fun `loadJson accumulates words across multiple calls`() {
        val bank = WordBank()
        bank.loadJson("""{"words":["one","two"]}""")
        bank.loadJson("""{"words":["three"]}""")

        assertEquals(setOf("one", "two", "three"), bank.updateList())
    }

    @Test
    fun `loadJson ignores duplicate words`() {
        val bank = WordBank()
        bank.loadJson("""{"words":["dup","dup","unique"]}""")

        assertEquals(setOf("dup", "unique"), bank.updateList())
    }

    // Real Json Test
    @Test
    fun `the real words json loads`() {
        val json = WordBank::class.java.getResource("/words.json")!!.readText()
        val bank = WordBank()
        bank.loadJson(json)
        assertTrue(bank.updateList().isNotEmpty())
    }

    // --- chooseWord ---

    @Test
    fun `chooseWord returns a loaded word and sets the current word`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        val chosen = bank.chooseWord()
        assertTrue(chosen in sampleWords)
        assertEquals(chosen, bank.getCurrentWord())
    }

    @Test
    fun `chooseWord returns null when no words are available`() {
        val bank = WordBank()
        assertNull(bank.chooseWord())
    }

    // --- updateList ---

    @Test
    fun `updateList removes the current word from the available set`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        val chosen = bank.chooseWord()
        val remaining = bank.updateList()

        assertEquals(2, remaining.size)
        assertTrue(chosen !in remaining)
    }

    @Test
    fun `updateList does nothing when no word has been chosen`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        assertEquals(sampleWords, bank.updateList())
    }

    @Test
    fun `updateList is idempotent for an already removed word`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        val chosen = bank.chooseWord()
        bank.updateList()
        // calling again with the same current word should not remove anything extra
        val remaining = bank.updateList()

        assertEquals(2, remaining.size)
        assertTrue(chosen !in remaining)
    }

    @Test
    fun `choosing and updating repeatedly drains the word bank`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        repeat(3) {
            bank.chooseWord()
            bank.updateList()
        }

        // all words have been removed; the next choose returns null
        assertTrue(bank.updateList().isEmpty())
        assertNull(bank.chooseWord())
    }

    // --- resetList ---

    @Test
    fun `resetList clears all available words`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        bank.resetList()

        assertTrue(bank.updateList().isEmpty())
        assertNull(bank.chooseWord())
    }

    @Test
    fun `words can be reloaded after a reset`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)
        bank.resetList()

        bank.loadJson("""{"words":["fresh"]}""")
        assertEquals("fresh", bank.chooseWord())
    }
}
