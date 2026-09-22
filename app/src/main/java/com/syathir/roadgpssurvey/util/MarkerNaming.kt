package com.syathir.roadgpssurvey.util

import java.util.Locale

object MarkerNaming {
    fun forNumber(number: Int): String {
        require(number >= 1) { "Marker number must be positive" }
        return String.format(Locale.US, "POINT_%03d", number)
    }
}

