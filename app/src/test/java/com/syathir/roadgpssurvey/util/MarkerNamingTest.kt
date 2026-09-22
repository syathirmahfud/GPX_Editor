package com.syathir.roadgpssurvey.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MarkerNamingTest {
    @Test
    fun numbersMarkersSequentiallyWithThreeDigitMinimum() {
        assertEquals("POINT_001", MarkerNaming.forNumber(1))
        assertEquals("POINT_002", MarkerNaming.forNumber(2))
        assertEquals("POINT_010", MarkerNaming.forNumber(10))
        assertEquals("POINT_1000", MarkerNaming.forNumber(1_000))
    }

    @Test
    fun rejectsNonPositiveMarkerNumbers() {
        assertThrows(IllegalArgumentException::class.java) { MarkerNaming.forNumber(0) }
    }
}

