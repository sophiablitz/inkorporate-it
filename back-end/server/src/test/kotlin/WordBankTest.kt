package it.inkorporate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordBankTest {

    private val sampleJson = """{"words":["apple","banana","cherry"]}"""

    @Test
    fun `getCurrentWord is null before any word is chosen`() {
        val bank = WordBank()
        assertNull(bank.getCurrentWord())
    }

    @Test
    fun `loadJson populates the available words`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        // every word in the bank should be one of the loaded words
        val words = setOf("apple", "banana", "cherry")
        repeat(10) {
            assertTrue(bank.chooseWord() in words)
        }
    }

    @Test
    fun `loadJson with empty words list adds nothing`() {
        val bank = WordBank()
        bank.loadJson("""{"words":[]}""")

        // choosing from an empty set throws
        assertFailsWith<NoSuchElementException> { bank.chooseWord() }
    }

    @Test
    fun `loadJson accumulates words across multiple calls`() {
        val bank = WordBank()
        bank.loadJson("""{"words":["one","two"]}""")
        bank.loadJson("""{"words":["three"]}""")

        bank.chooseWord()
        // all three words should still be present until removed
        val remaining = bank.updateList()
        // one word was chosen and removed, leaving two of the original three
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it in setOf("one", "two", "three") })
    }

    @Test
    fun `loadJson ignores duplicate words`() {
        val bank = WordBank()
        bank.loadJson("""{"words":["dup","dup","unique"]}""")

        // updateList with no chosen word returns the full de-duplicated set
        val words = bank.updateList()
        assertEquals(setOf("dup", "unique"), words)
    }

    @Test
    fun `chooseWord returns a loaded word and sets the current word`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        val chosen = bank.chooseWord()
        assertTrue(chosen in setOf("apple", "banana", "cherry"))
        assertEquals(chosen, bank.getCurrentWord())
    }

    @Test
    fun `chooseWord throws when no words are available`() {
        val bank = WordBank()
        assertFailsWith<NoSuchElementException> { bank.chooseWord() }
    }

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

        val remaining = bank.updateList()
        assertEquals(setOf("apple", "banana", "cherry"), remaining)
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

        // all words have been removed; the next choose should throw
        assertFailsWith<NoSuchElementException> { bank.chooseWord() }
    }

    @Test
    fun `resetList clears all available words`() {
        val bank = WordBank()
        bank.loadJson(sampleJson)

        bank.resetList()

        // set is now empty, so updateList returns an empty set
        assertTrue(bank.updateList().isEmpty())
        // and choosing a word throws because nothing is available
        assertFailsWith<NoSuchElementException> { bank.chooseWord() }
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
