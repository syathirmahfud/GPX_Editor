package com.syathir.roadgpssurvey.util

import com.syathir.roadgpssurvey.location.LocationSample
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    private const val EARTH_MEAN_RADIUS_M = 6_371_008.8

    fun metresBetween(a: LocationSample, b: LocationSample): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(deltaLat / 2.0)
        val sinLon = sin(deltaLon / 2.0)
        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon
        return 2.0 * EARTH_MEAN_RADIUS_M * atan2(sqrt(h), sqrt((1.0 - h).coerceAtLeast(0.0)))
    }
}

