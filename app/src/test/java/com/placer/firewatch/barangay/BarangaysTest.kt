package com.placer.firewatch.barangay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarangaysTest {

    @Test
    fun `contains exactly 35 barangays`() {
        assertEquals(35, Barangays.ALL.size)
    }

    @Test
    fun `has no duplicates`() {
        assertEquals(Barangays.ALL.size, Barangays.ALL.toSet().size)
    }

    @Test
    fun `has no blank entries`() {
        assertTrue(Barangays.ALL.all { it.isNotBlank() })
    }

    @Test
    fun `includes Poblacion, the default barangay`() {
        assertTrue(Barangays.ALL.contains("Poblacion"))
    }
}
