package com.syathir.roadgpssurvey.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFilterTest {
    private val filter = LocationFilter(LocationFilterSettings())

    @Test
    fun rejectsPoorAccuracyAndStationaryJitter() {
        val origin = sample(latitude = 0.0, accuracyM = 3f, elapsedSeconds = 1)
        val poor = sample(latitude = 0.0001, accuracyM = 12f, elapsedSeconds = 2)
        val jitter = sample(latitude = 0.000005, accuracyM = 3f, elapsedSeconds = 2)

        assertEquals(
            RejectionReason.POOR_ACCURACY,
            (filter.evaluate(poor, origin) as LocationFilterDecision.Reject).reason,
        )
        assertEquals(
            RejectionReason.BELOW_MOVEMENT_THRESHOLD,
            (filter.evaluate(jitter, origin) as LocationFilterDecision.Reject).reason,
        )
    }

    @Test
    fun rejectsOutOfOrderAndImpossibleJump() {
        val origin = sample(latitude = 0.0, elapsedSeconds = 2)
        val old = sample(latitude = 0.0001, elapsedSeconds = 1)
        val jump = sample(latitude = 0.01, elapsedSeconds = 3)

        assertEquals(
            RejectionReason.OUT_OF_ORDER,
            (filter.evaluate(old, origin) as LocationFilterDecision.Reject).reason,
        )
        assertEquals(
            RejectionReason.IMPLAUSIBLE_JUMP,
            (filter.evaluate(jump, origin) as LocationFilterDecision.Reject).reason,
        )
    }

    @Test
    fun acceptsPlausibleMovement() {
        val decision = filter.evaluate(
            sample(latitude = 0.00009, elapsedSeconds = 2),
            sample(latitude = 0.0, elapsedSeconds = 1),
        )
        assertTrue(decision is LocationFilterDecision.Accept)
        assertTrue((decision as LocationFilterDecision.Accept).displacementM in 9.0..11.0)
    }

    private fun sample(
        latitude: Double,
        accuracyM: Float = 3f,
        elapsedSeconds: Long,
    ) = LocationSample(
        latitude = latitude,
        longitude = 0.0,
        altitudeM = null,
        horizontalAccuracyM = accuracyM,
        speedMps = 0f,
        timestampMs = elapsedSeconds * 1_000L,
        elapsedRealtimeNanos = elapsedSeconds * 1_000_000_000L,
    )
}

