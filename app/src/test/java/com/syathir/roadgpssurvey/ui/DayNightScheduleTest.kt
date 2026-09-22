package com.syathir.roadgpssurvey.ui

import com.syathir.roadgpssurvey.ui.theme.DayNightSchedule
import com.syathir.roadgpssurvey.export.ExportFileName
import com.syathir.roadgpssurvey.export.ExportFormat
import org.junit.Assert.*
import org.junit.Test

class DayNightScheduleTest {
    @Test fun localTimeBoundariesSwitchAtSixAndEighteen() {
        assertTrue(DayNightSchedule.isNight(0))
        assertTrue(DayNightSchedule.isNight(5))
        assertFalse(DayNightSchedule.isNight(6))
        assertFalse(DayNightSchedule.isNight(17))
        assertTrue(DayNightSchedule.isNight(18))
        assertTrue(DayNightSchedule.isNight(23))
    }

    @Test fun exportUsesRuasNameWithoutPathCharacters() {
        assertEquals("Jalan A_B_42.csv", ExportFileName.forSession("Jalan A/B", 42, ExportFormat.CSV))
        assertEquals("Ruas_42.gpx", ExportFileName.forSession("...", 42, ExportFormat.GPX))
        assertEquals("Jalan KMZ_7.kmz", ExportFileName.forSession("Jalan KMZ", 7, ExportFormat.KMZ))
    }
}
