package com.syathir.roadgpssurvey.tracking

import com.syathir.roadgpssurvey.location.LocationSample
import com.syathir.roadgpssurvey.model.TrackingStatus

data class TrackingSnapshot(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val totalDistanceM: Double = 0.0,
    val activeElapsedMs: Long = 0L,
    val activeSegmentStartedElapsedMs: Long? = null,
    val startChainageM: Double = 0.0,
    val accuracyM: Float? = null,
    val speedKmh: Double = 0.0,
    val markerCount: Int = 0,
    val errorMessage: String? = null,
    val sessionId: Long? = null,
) {
    fun elapsedAt(nowElapsedMs: Long): Long = activeElapsedMs +
        if (status == TrackingStatus.TRACKING && activeSegmentStartedElapsedMs != null) {
            (nowElapsedMs - activeSegmentStartedElapsedMs).coerceAtLeast(0L)
        } else {
            0L
        }
}

sealed interface TrackingEvent {
    data class PointAccepted(
        val location: LocationSample,
        val distanceFromStartM: Double,
    ) : TrackingEvent

    data class MarkerCaptured(
        val location: LocationSample,
        val number: Int,
        val distanceM: Double,
        val activeElapsedMs: Long,
    ) : TrackingEvent

    data object NoChange : TrackingEvent
}
