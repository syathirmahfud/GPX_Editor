package com.syathir.roadgpssurvey.tracking

import com.syathir.roadgpssurvey.location.LocationFilter
import com.syathir.roadgpssurvey.location.LocationFilterDecision
import com.syathir.roadgpssurvey.location.LocationFilterSettings
import com.syathir.roadgpssurvey.location.LocationSample
import com.syathir.roadgpssurvey.model.TrackingStatus

class TrackingEngine(
    filterSettings: LocationFilterSettings = LocationFilterSettings(),
    initialSnapshot: TrackingSnapshot = TrackingSnapshot(),
) {
    private val filter = LocationFilter(filterSettings)
    private var lastAccepted: LocationSample? = null

    var snapshot: TrackingSnapshot = initialSnapshot
        private set

    fun start(nowElapsedMs: Long, startChainageM: Double): TrackingSnapshot {
        check(snapshot.status == TrackingStatus.IDLE || snapshot.status == TrackingStatus.FINISHED)
        require(startChainageM >= 0.0 && startChainageM.isFinite())
        lastAccepted = null
        snapshot = TrackingSnapshot(
            status = TrackingStatus.TRACKING,
            activeSegmentStartedElapsedMs = nowElapsedMs,
            startChainageM = startChainageM,
        )
        return snapshot
    }

    fun onLocation(location: LocationSample): TrackingEvent {
        snapshot = snapshot.copy(
            accuracyM = location.horizontalAccuracyM.takeIf { it.isFinite() && it > 0f },
            speedKmh = (location.speedMps * 3.6).coerceAtLeast(0.0),
            errorMessage = null,
        )
        if (snapshot.status != TrackingStatus.TRACKING) return TrackingEvent.NoChange

        return when (val decision = filter.evaluate(location, lastAccepted)) {
            is LocationFilterDecision.Accept -> {
                snapshot = snapshot.copy(totalDistanceM = snapshot.totalDistanceM + decision.displacementM)
                lastAccepted = location
                TrackingEvent.PointAccepted(location, snapshot.totalDistanceM)
            }
            is LocationFilterDecision.Reject -> TrackingEvent.NoChange
        }
    }

    fun pauseAndMark(location: LocationSample, nowElapsedMs: Long): TrackingEvent.MarkerCaptured {
        check(snapshot.status == TrackingStatus.TRACKING)
        val elapsed = snapshot.elapsedAt(nowElapsedMs)
        val nextMarker = snapshot.markerCount + 1
        snapshot = snapshot.copy(
            status = TrackingStatus.PAUSED,
            activeElapsedMs = elapsed,
            activeSegmentStartedElapsedMs = null,
            accuracyM = location.horizontalAccuracyM,
            speedKmh = location.speedMps * 3.6,
            markerCount = nextMarker,
        )
        lastAccepted = null
        return TrackingEvent.MarkerCaptured(location, nextMarker, snapshot.totalDistanceM, elapsed)
    }

    fun markOnly(location: LocationSample, nowElapsedMs: Long): TrackingEvent.MarkerCaptured {
        check(snapshot.status == TrackingStatus.TRACKING)
        snapshot = snapshot.copy(
            accuracyM = location.horizontalAccuracyM,
            speedKmh = location.speedMps * 3.6,
            markerCount = snapshot.markerCount + 1,
            errorMessage = null,
        )
        // Keep both the distance origin and running time segment unchanged.
        return TrackingEvent.MarkerCaptured(location, snapshot.markerCount, snapshot.totalDistanceM, snapshot.elapsedAt(nowElapsedMs))
    }

    fun resume(origin: LocationSample, nowElapsedMs: Long): TrackingEvent.PointAccepted {
        check(snapshot.status == TrackingStatus.PAUSED)
        lastAccepted = origin
        snapshot = snapshot.copy(
            status = TrackingStatus.TRACKING,
            activeSegmentStartedElapsedMs = nowElapsedMs,
            accuracyM = origin.horizontalAccuracyM,
            speedKmh = origin.speedMps * 3.6,
        )
        return TrackingEvent.PointAccepted(origin, snapshot.totalDistanceM)
    }

    fun finish(nowElapsedMs: Long): TrackingSnapshot {
        check(snapshot.status == TrackingStatus.TRACKING || snapshot.status == TrackingStatus.PAUSED)
        val elapsed = snapshot.elapsedAt(nowElapsedMs)
        lastAccepted = null
        snapshot = snapshot.copy(
            status = TrackingStatus.FINISHED,
            activeElapsedMs = elapsed,
            activeSegmentStartedElapsedMs = null,
            speedKmh = 0.0,
        )
        return snapshot
    }

    fun checkpoint(nowElapsedMs: Long): TrackingSnapshot {
        if (snapshot.status == TrackingStatus.TRACKING) {
            snapshot = snapshot.copy(
                activeElapsedMs = snapshot.elapsedAt(nowElapsedMs),
                activeSegmentStartedElapsedMs = nowElapsedMs,
            )
        }
        return snapshot
    }

    fun restoreDistanceOrigin(origin: LocationSample?) {
        lastAccepted = origin
    }

    fun setError(message: String) {
        snapshot = snapshot.copy(errorMessage = message)
    }
}
