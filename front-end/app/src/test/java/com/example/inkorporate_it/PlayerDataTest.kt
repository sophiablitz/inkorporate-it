package com.example.inkorporate_it

import androidx.compose.runtime.saveable.SaverScope
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.model.DefaultNames
import com.example.inkorporate_it.model.Player
import com.example.inkorporate_it.model.PlayerSaver
import com.example.inkorporate_it.model.cleanName
import com.example.inkorporate_it.model.filterJoinCode
import com.example.inkorporate_it.model.isJoinCodeComplete
import com.example.inkorporate_it.model.randomDefaultPlayer
import com.example.inkorporate_it.ui.theme.BlobColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the Home screen rules in model/PlayerData.kt.
class PlayerDataTest {

    // --- cleanName ---

    @Test
    fun cleanName_trimsSpaces() {
        assertEquals("Jerry", cleanName("  Jerry  ", oldName = "Old"))
    }

    @Test
    fun cleanName_cutsTo16Characters() {
        assertEquals("ABCDEFGHIJKLMNOP", cleanName("ABCDEFGHIJKLMNOPQRS", oldName = "Old"))
    }

    @Test
    fun cleanName_emptyGivesOldNameBack() {
        assertEquals("Old", cleanName("", oldName = "Old"))
        assertEquals("Old", cleanName("    ", oldName = "Old"))
    }

    // --- filterJoinCode ---

    @Test
    fun filterJoinCode_removesNonDigits() {
        assertEquals("123", filterJoinCode("1a2-b3"))
    }

    @Test
    fun filterJoinCode_stopsAtSixDigits() {
        assertEquals("123456", filterJoinCode("12345678"))
    }

    // --- isJoinCodeComplete ---

    @Test
    fun isJoinCodeComplete_onlyExactlySixDigits() {
        assertTrue(isJoinCodeComplete("123456"))
        assertFalse(isJoinCodeComplete("12345"))
        assertFalse(isJoinCodeComplete(""))
        assertFalse(isJoinCodeComplete("12345a"))
    }

    // --- randomDefaultPlayer ---

    @Test
    fun randomDefaultPlayer_usesATeamName() {
        val player = randomDefaultPlayer()
        assertTrue(player.username in DefaultNames)
    }

    @Test
    fun randomDefaultPlayer_usesABlobColor() {
        val player = randomDefaultPlayer()
        assertTrue(player.avatar.inkColor in BlobColors.all)
    }

    // --- BlobColors ---

    @Test
    fun blobColors_hasEightDifferentColors() {
        // toSet() drops duplicates, so 8 means no color was listed twice.
        assertEquals(8, BlobColors.all.toSet().size)
    }

    // --- PlayerSaver ---

    @Test
    fun playerSaver_savesAndRestoresTheSamePlayer() {
        val player = Player(
            username = "Jerry",
            avatar = Avatar(hatId = "beanie", eyesId = "wide", inkColor = BlobColors.Teal)
        )
        // SaverScope { true } = "every value can be saved", like Android's real save.
        val saved = with(PlayerSaver) { SaverScope { true }.save(player) }!!
        val restored = PlayerSaver.restore(saved)
        assertEquals(player, restored)
    }
}
