package com.syathir.roadgpssurvey.data

import androidx.room.withTransaction
import com.syathir.roadgpssurvey.location.LocationSample
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import com.syathir.roadgpssurvey.util.ChainageFormatter
import com.syathir.roadgpssurvey.util.MarkerNaming
import kotlinx.coroutines.flow.Flow

class SurveyRepository(private val database: AppDatabase) {
    private val sessions = database.sessionDao()
    private val points = database.trackPointDao()
    private val markers = database.markerDao()

    val activeSession: Flow<SurveySession?> = sessions.observeActive()
    val allSessions: Flow<List<SurveySession>> = sessions.observeAll()
    val allMarkers: Flow<List<SurveyMarker>> = markers.observeAll()
    val pendingFinish: Flow<SurveySession?> = sessions.observePendingFinish()

    suspend fun nameFinishedSession(id: Long, name: String) = database.withTransaction {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 120) { "Nama ruas wajib diisi (maksimal 120 karakter)." }
        check(sessions.nameFinished(id, trimmed) == 1) { "Ruas sudah tidak tersedia." }
        markers.renameSource(id, trimmed)
    }

    suspend fun dismissFinish(id: Long) = sessions.dismissFinish(id)

    suspend fun createSession(
        name: String,
        startedAt: Long,
        nowElapsedMs: Long,
        startChainageM: Double,
    ): SurveySession {
        val session = SurveySession(
            name = name,
            startedAt = startedAt,
            activeSegmentStartedElapsedRealtimeMs = nowElapsedMs,
            startChainageM = startChainageM,
            status = TrackingStatus.TRACKING,
        )
        return session.copy(id = sessions.insert(session))
    }

    suspend fun getActive(): SurveySession? = sessions.getActive()
    suspend fun getSession(id: Long): SurveySession? = sessions.get(id)
    fun observeSession(id: Long): Flow<SurveySession?> = sessions.observe(id)
    fun observeMarkers(id: Long): Flow<List<SurveyMarker>> = markers.observeForSession(id)
    suspend fun getPoints(id: Long): List<TrackPoint> = points.getForSession(id)
    suspend fun getMarkers(id: Long): List<SurveyMarker> = markers.getForSession(id)

    suspend fun deleteFinishedSession(id: Long): Boolean = database.withTransaction {
        val session = sessions.get(id) ?: return@withTransaction false
        if (session.status != TrackingStatus.FINISHED) return@withTransaction false
        markers.renameSource(id, session.name)
        markers.detachFromSession(id)
        points.deleteForSession(id)
        sessions.deleteFinished(id) == 1
    }

    suspend fun getLatestLocation(sessionId: Long): LocationSample? =
        points.getLatest(sessionId)?.toLocationSample()

    suspend fun saveAcceptedPoint(
        session: SurveySession,
        location: LocationSample,
        snapshot: TrackingSnapshot,
    ): SurveySession = database.withTransaction {
        val updated = session.copy(totalDistanceM = snapshot.totalDistanceM)
        points.insert(location.toTrackPoint(updated))
        sessions.update(updated)
        updated
    }

    suspend fun saveMarker(
        session: SurveySession,
        location: LocationSample,
        snapshot: TrackingSnapshot,
    ): SurveySession = database.withTransaction {
        val updated = session.copy(
            status = snapshot.status,
            activeElapsedMs = snapshot.activeElapsedMs,
            activeSegmentStartedElapsedRealtimeMs = snapshot.activeSegmentStartedElapsedMs,
            totalDistanceM = snapshot.totalDistanceM,
            markerCount = snapshot.markerCount,
        )
        val markerNumber = updated.markerCount
        markers.insert(
            SurveyMarker(
                sessionId = session.id,
                sourceRuasName = session.name,
                pointName = MarkerNaming.forNumber(markerNumber),
                timestamp = location.timestampMs,
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeM = location.altitudeM,
                accuracyM = location.horizontalAccuracyM,
                distanceM = updated.totalDistanceM,
                chainageM = ChainageFormatter.chainageMetres(updated.startChainageM, updated.totalDistanceM),
                speedMps = location.speedMps,
            ),
        )
        sessions.update(updated)
        updated
    }

    suspend fun resumeWithOrigin(
        session: SurveySession,
        origin: LocationSample,
        snapshot: TrackingSnapshot,
    ): SurveySession = database.withTransaction {
        val updated = session.copy(
            status = TrackingStatus.TRACKING,
            activeSegmentStartedElapsedRealtimeMs = snapshot.activeSegmentStartedElapsedMs,
        )
        points.insert(origin.toTrackPoint(updated))
        sessions.update(updated)
        updated
    }

    suspend fun persistTimer(session: SurveySession, snapshot: TrackingSnapshot): SurveySession {
        val updated = session.copy(
            activeElapsedMs = snapshot.activeElapsedMs,
            activeSegmentStartedElapsedRealtimeMs = snapshot.activeSegmentStartedElapsedMs,
            totalDistanceM = snapshot.totalDistanceM,
            status = snapshot.status,
            markerCount = snapshot.markerCount,
        )
        sessions.update(updated)
        return updated
    }

    suspend fun finish(session: SurveySession, snapshot: TrackingSnapshot, finishedAt: Long): SurveySession {
        val updated = session.copy(
            status = TrackingStatus.FINISHED,
            finishedAt = finishedAt,
            activeElapsedMs = snapshot.activeElapsedMs,
            activeSegmentStartedElapsedRealtimeMs = null,
            totalDistanceM = snapshot.totalDistanceM,
            markerCount = snapshot.markerCount,
            finishStep = "NAME",
        )
        sessions.update(updated)
        return updated
    }

    suspend fun updateMarker(marker: SurveyMarker) =
        markers.updateDetails(marker.id, marker.pointName, marker.note, marker.markerType)

    suspend fun deleteRetainedMarker(id: Long): Boolean = markers.deleteDetached(id) == 1

    private fun LocationSample.toTrackPoint(session: SurveySession): TrackPoint = TrackPoint(
        sessionId = session.id,
        timestamp = timestampMs,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
        latitude = latitude,
        longitude = longitude,
        altitudeM = altitudeM,
        accuracyM = horizontalAccuracyM,
        speedMps = speedMps,
        distanceFromStartM = session.totalDistanceM,
        chainageM = ChainageFormatter.chainageMetres(session.startChainageM, session.totalDistanceM),
    )

    private fun TrackPoint.toLocationSample() = LocationSample(
        latitude = latitude,
        longitude = longitude,
        altitudeM = altitudeM,
        horizontalAccuracyM = accuracyM,
        speedMps = speedMps,
        timestampMs = timestamp,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
    )
}
