package com.syathir.roadgpssurvey.location

import com.syathir.roadgpssurvey.util.GeoDistance
import kotlin.math.max

data class LocationFilterSettings(
    val maximumAccuracyM: Double = 10.0,
    val minimumMovementM: Double = 2.0,
    // Operational guard for a road-survey app, not a road design-speed assumption.
    val maximumPlausibleSpeedKmh: Double = 250.0,
)

sealed interface LocationFilterDecision {
    data class Accept(val displacementM: Double) : LocationFilterDecision
    data class Reject(val reason: RejectionReason) : LocationFilterDecision
}

enum class RejectionReason {
    INVALID_COORDINATE,
    INVALID_ACCURACY,
    POOR_ACCURACY,
    OUT_OF_ORDER,
    BELOW_MOVEMENT_THRESHOLD,
    IMPLAUSIBLE_JUMP,
}

class LocationFilter(private val settings: LocationFilterSettings) {
    init {
        require(settings.maximumAccuracyM > 0.0)
        require(settings.minimumMovementM >= 0.0)
        require(settings.maximumPlausibleSpeedKmh > 0.0)
    }

    fun evaluate(candidate: LocationSample, previous: LocationSample?): LocationFilterDecision {
        if (!candidate.latitude.isFinite() || !candidate.longitude.isFinite() ||
            candidate.latitude !in -90.0..90.0 || candidate.longitude !in -180.0..180.0
        ) {
            return LocationFilterDecision.Reject(RejectionReason.INVALID_COORDINATE)
        }
        if (!candidate.horizontalAccuracyM.isFinite() || candidate.horizontalAccuracyM <= 0f) {
            return LocationFilterDecision.Reject(RejectionReason.INVALID_ACCURACY)
        }
        if (candidate.horizontalAccuracyM > settings.maximumAccuracyM) {
            return LocationFilterDecision.Reject(RejectionReason.POOR_ACCURACY)
        }
        if (previous == null) return LocationFilterDecision.Accept(0.0)
        if (candidate.elapsedRealtimeNanos <= previous.elapsedRealtimeNanos) {
            return LocationFilterDecision.Reject(RejectionReason.OUT_OF_ORDER)
        }

        val displacementM = GeoDistance.metresBetween(previous, candidate)
        val uncertaintyAwareMinimumM = max(
            settings.minimumMovementM,
            (previous.horizontalAccuracyM + candidate.horizontalAccuracyM) * 0.25,
        )
        if (displacementM < uncertaintyAwareMinimumM) {
            return LocationFilterDecision.Reject(RejectionReason.BELOW_MOVEMENT_THRESHOLD)
        }

        val deltaSeconds = (candidate.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
        val impliedSpeedKmh = displacementM / deltaSeconds * 3.6
        if (!impliedSpeedKmh.isFinite() || impliedSpeedKmh > settings.maximumPlausibleSpeedKmh) {
            return LocationFilterDecision.Reject(RejectionReason.IMPLAUSIBLE_JUMP)
        }
        return LocationFilterDecision.Accept(displacementM)
    }
}

