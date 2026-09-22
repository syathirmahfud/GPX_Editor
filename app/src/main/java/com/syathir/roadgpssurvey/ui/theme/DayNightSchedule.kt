package com.syathir.roadgpssurvey.ui.theme

import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

object DayNightSchedule {
    fun isNight(hourOfDay: Int): Boolean = hourOfDay < 6 || hourOfDay >= 18

    fun isNightNow(): Boolean = isNight(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    // Re-read the local timezone/clock, including manual time changes and returning from sleep.
    fun observeNight() = flow {
        while (true) {
            emit(isNightNow())
            delay(1_000L)
        }
    }.distinctUntilChanged()
}
