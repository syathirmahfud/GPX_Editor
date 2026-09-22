package com.syathir.roadgpssurvey.ui

import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class LocationPreviewState(
    val accuracyM: Float? = null,
    val speedKmh: Double = 0.0,
    val error: String? = null,
)

/** One writer owns the dashboard; preview callbacks never replace survey state. */
fun dashboardStates(
    runtime: Flow<TrackingSnapshot>,
    activeSession: Flow<SurveySession?>,
    preview: Flow<LocationPreviewState>,
    ticker: Flow<Long>,
): Flow<MainUiState> = combine(runtime, activeSession, preview, ticker) { live, stored, fix, now ->
    val snapshot = if (stored != null && stored.id != live.sessionId) {
        TrackingSnapshot(
            status = stored.status,
            totalDistanceM = stored.totalDistanceM,
            activeElapsedMs = stored.activeElapsedMs,
            activeSegmentStartedElapsedMs = stored.activeSegmentStartedElapsedRealtimeMs,
            startChainageM = stored.startChainageM,
            markerCount = stored.markerCount,
            sessionId = stored.id,
        )
    } else live
    val active = snapshot.status == TrackingStatus.TRACKING || snapshot.status == TrackingStatus.PAUSED
    val rawAccuracy = if (active) snapshot.accuracyM ?: fix.accuracyM else fix.accuracyM ?: snapshot.accuracyM
    val roundedAccuracy = rawAccuracy?.let { kotlin.math.round(it * 10f) / 10f }
    val rawSpeed = if (active) snapshot.speedKmh else fix.speedKmh
    val roundedSpeed = kotlin.math.round(rawSpeed * 10.0) / 10.0
    MainUiState(
        status = snapshot.status,
        distanceM = snapshot.totalDistanceM,
        startChainageM = snapshot.startChainageM,
        // The UI displays whole seconds. Keep subsecond precision in the engine/database only.
        activeElapsedMs = snapshot.elapsedAt(now) / 1_000L * 1_000L,
        accuracyM = roundedAccuracy,
        speedKmh = roundedSpeed,
        locationError = if (active) snapshot.errorMessage else fix.error ?: snapshot.errorMessage,
        markerCount = snapshot.markerCount,
    )
}.distinctUntilChanged()
