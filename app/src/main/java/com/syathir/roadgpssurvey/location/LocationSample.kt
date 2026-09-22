package com.syathir.roadgpssurvey.location

import android.location.Location

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val horizontalAccuracyM: Float,
    val speedMps: Float,
    val timestampMs: Long,
    val elapsedRealtimeNanos: Long,
) {
    companion object {
        fun from(location: Location): LocationSample = LocationSample(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeM = location.altitude.takeIf { location.hasAltitude() },
            horizontalAccuracyM = location.accuracy,
            speedMps = location.speed.takeIf { location.hasSpeed() } ?: 0f,
            timestampMs = location.time,
            elapsedRealtimeNanos = location.elapsedRealtimeNanos,
        )
    }
}

