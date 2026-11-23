package com.example.inkorporate_it.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the blob colors in model/BlobColors.kt.
class BlobColorsTest {

    @Test
    fun blobColors_hasNoDuplicates() {
        // toSet() drops duplicates, so the sizes only match if no color is listed twice.
        // Doesn't check the count, so adding a ninth color won't break this test.
        assertEquals(BlobColors.all.size, BlobColors.all.toSet().size)
    }

    @Test
    fun defaultAvatar_usesABlobColor() {
        // A new player's blob (first launch) must be one of the colors the selector shows.
        assertTrue(Avatar().inkColor in BlobColors.all)
    }
}
