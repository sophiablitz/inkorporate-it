package com.example.inkorporate_it.model

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for AvatarSaver in model/PlayerData.kt.
class AvatarSaverTest {

    @Test
    fun avatarSaver_savesAndRestoresTheSameAvatar() {
        val avatar = Avatar(hatId = "beanie", eyesId = "wide", inkColor = Color(0xFFE04B3B))
        // SaverScope { true } = "every value can be saved", like Android's real save.
        val saved = with(AvatarSaver) { SaverScope { true }.save(avatar) }!!
        val restored = AvatarSaver.restore(saved)
        assertEquals(avatar, restored)
    }
}
