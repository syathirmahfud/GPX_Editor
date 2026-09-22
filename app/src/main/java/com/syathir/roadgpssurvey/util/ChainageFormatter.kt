package com.syathir.roadgpssurvey.util

import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

object ChainageFormatter {
    fun format(chainageM: Double): String {
        require(chainageM.isFinite()) { "Chainage must be finite" }
        val roundedM = chainageM.coerceAtLeast(0.0).roundToLong()
        val km = roundedM / 1_000L
        val remainderM = roundedM % 1_000L
        return String.format(Locale.US, "%d+%03d", km, remainderM)
    }

    fun parse(value: String): Double? {
        val match = Regex("^\\s*(?:STA\\s*)?(\\d+)\\s*\\+\\s*(\\d{1,3})\\s*$", RegexOption.IGNORE_CASE)
            .matchEntire(value) ?: return null
        val km = match.groupValues[1].toLongOrNull() ?: return null
        val metres = match.groupValues[2].toIntOrNull() ?: return null
        if (metres !in 0..999) return null
        return km * 1_000.0 + metres
    }

    fun chainageMetres(startChainageM: Double, distanceM: Double): Double {
        require(startChainageM.isFinite() && startChainageM >= 0.0)
        require(distanceM.isFinite() && distanceM >= 0.0)
        return floor((startChainageM + distanceM) * 1_000.0 + 0.5) / 1_000.0
    }
}

