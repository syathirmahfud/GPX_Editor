package com.syathir.roadgpssurvey.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChainageFormatterTest {
    @Test
    fun formatsRequiredStationValues() {
        val cases = mapOf(
            0.0 to "0+000",
            99.0 to "0+099",
            100.0 to "0+100",
            999.0 to "0+999",
            1_000.0 to "1+000",
            3_420.0 to "3+420",
            12_470.0 to "12+470",
        )
        cases.forEach { (metres, expected) -> assertEquals(expected, ChainageFormatter.format(metres)) }
    }

    @Test
    fun appliesStartingStationIndependentlyOfUi() {
        assertEquals("12+470", ChainageFormatter.format(ChainageFormatter.chainageMetres(12_300.0, 170.0)))
        assertEquals(12_300.0, ChainageFormatter.parse("STA 12+300")!!, 0.0)
    }
}

