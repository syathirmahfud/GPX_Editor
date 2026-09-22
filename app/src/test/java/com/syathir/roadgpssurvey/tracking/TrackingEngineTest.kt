package com.syathir.roadgpssurvey.tracking

import com.syathir.roadgpssurvey.location.LocationSample
import com.syathir.roadgpssurvey.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingEngineTest {
    @Test
    fun accumulatesOnlyAcceptedDistance() {
        val engine = TrackingEngine()
        engine.start(nowElapsedMs = 0L, startChainageM = 0.0)
        engine.onLocation(sample(0.0, 1))
        engine.onLocation(sample(0.000005, 2)) // Less than the 2 m movement threshold.
        engine.onLocation(sample(0.00009, 3))
        engine.onLocation(sample(0.00018, 4))

        assertTrue(engine.snapshot.totalDistanceM in 19.0..21.0)
    }

    @Test
    fun pauseResumeDoesNotBridgeThePausedGap() {
        val engine = TrackingEngine()
        engine.start(nowElapsedMs = 0L, startChainageM = 12_300.0)
        val beforePause = sample(0.00009, 2)
        engine.onLocation(sample(0.0, 1))
        engine.onLocation(beforePause)
        engine.pauseAndMark(beforePause, nowElapsedMs = 2_000L)

        val postPauseOrigin = sample(0.00450, 5) // Approximately 500 m away.
        engine.resume(postPauseOrigin, nowElapsedMs = 5_000L)
        engine.onLocation(sample(0.00459, 6))

        assertEquals(TrackingStatus.TRACKING, engine.snapshot.status)
        assertTrue(engine.snapshot.totalDistanceM in 19.0..21.0)
    }

    @Test
    fun elapsedTimeCountsOnlyActiveSegments() {
        val engine = TrackingEngine()
        val location = sample(0.0, 1)
        engine.start(nowElapsedMs = 1_000L, startChainageM = 0.0)
        engine.pauseAndMark(location, nowElapsedMs = 6_000L)
        assertEquals(5_000L, engine.snapshot.activeElapsedMs)

        engine.resume(sample(0.01, 10), nowElapsedMs = 10_000L)
        engine.finish(nowElapsedMs = 12_000L)
        assertEquals(7_000L, engine.snapshot.activeElapsedMs)
    }

    @Test
    fun checkpointRebasesMonotonicTimerWithoutChangingElapsedTime() {
        val engine = TrackingEngine()
        engine.start(nowElapsedMs = 1_000L, startChainageM = 0.0)
        engine.checkpoint(nowElapsedMs = 4_000L)
        assertEquals(3_000L, engine.snapshot.activeElapsedMs)
        assertEquals(5_000L, engine.snapshot.elapsedAt(6_000L))
    }

    @Test fun markOnlyDoesNotPauseTimerOrResetDistanceOrigin() {
        val engine = TrackingEngine()
        engine.start(0L, 1000.0)
        engine.onLocation(sample(0.0, 1))
        val marked = sample(0.00009, 2)
        engine.onLocation(marked)
        val event = engine.markOnly(marked, 2000)
        assertEquals(1, event.number)
        assertEquals(TrackingStatus.TRACKING, engine.snapshot.status)
        assertEquals(0L, engine.snapshot.activeSegmentStartedElapsedMs)
        engine.onLocation(sample(0.00018, 3))
        assertTrue(engine.snapshot.totalDistanceM in 19.0..21.0)
        assertEquals(3000L, engine.snapshot.elapsedAt(3000))
        engine.pauseAndMark(sample(0.00018, 3), 3000)
        assertEquals(2, engine.snapshot.markerCount)
        assertEquals(TrackingStatus.PAUSED, engine.snapshot.status)
    }

    private fun sample(latitude: Double, elapsedSeconds: Long) = LocationSample(
        latitude = latitude,
        longitude = 0.0,
        altitudeM = 5.0,
        horizontalAccuracyM = 3f,
        speedMps = 10f,
        timestampMs = elapsedSeconds * 1_000L,
        elapsedRealtimeNanos = elapsedSeconds * 1_000_000_000L,
    )
}
