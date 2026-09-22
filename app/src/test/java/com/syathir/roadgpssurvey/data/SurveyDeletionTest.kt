package com.syathir.roadgpssurvey.data

import android.app.Application
import androidx.room.Room
import com.syathir.roadgpssurvey.model.TrackingStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import com.syathir.roadgpssurvey.location.LocationSample
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SurveyDeletionTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SurveyRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repository = SurveyRepository(database)
    }

    @After fun close() = database.close()

    @Test fun deletingFinishedSurveyKeepsMarkersAndOnlyDeletesItsTrack() = runBlocking {
        val deletedId = seed(TrackingStatus.FINISHED)
        val keptId = seed(TrackingStatus.FINISHED)
        assertTrue(repository.deleteFinishedSession(deletedId))
        assertNull(repository.getSession(deletedId))
        assertTrue(repository.getPoints(deletedId).isEmpty())
        assertTrue(repository.getMarkers(deletedId).isEmpty())
        val retained = database.markerDao().getAll().single { it.sessionId == null }
        assertEquals("Test survey", retained.sourceRuasName)
        assertEquals("TEST_001", retained.pointName)
        assertEquals(0.0, retained.latitude, 0.0)
        assertNotNull(repository.getSession(keptId))
        assertEquals(1, repository.getPoints(keptId).size)
        assertEquals(1, repository.getMarkers(keptId).size)
        assertFalse(repository.deleteFinishedSession(deletedId))
        repository.updateMarker(retained.copy(note = "Masih tersimpan", sessionId = deletedId))
        assertEquals("Masih tersimpan", database.markerDao().getAll().single { it.sessionId == null }.note)
        assertTrue(repository.deleteRetainedMarker(retained.id))
        assertTrue(database.markerDao().getAll().none { it.id == retained.id })
    }

    @Test fun namingAfterFinishPersistsPromptAndRenamesMarkerSource() = runBlocking {
        val id = seed(TrackingStatus.TRACKING)
        val session = repository.getSession(id)!!
        repository.finish(session, TrackingSnapshot(status = TrackingStatus.FINISHED, markerCount = 1), 1234)
        val reopenedRepository = SurveyRepository(database)
        assertEquals("NAME", reopenedRepository.pendingFinish.first()!!.finishStep)
        reopenedRepository.nameFinishedSession(id, "  Jalan Uji  ")
        assertEquals("Jalan Uji", reopenedRepository.getSession(id)!!.name)
        assertEquals("EXPORT", reopenedRepository.pendingFinish.first()!!.finishStep)
        assertEquals("Jalan Uji", reopenedRepository.getMarkers(id).single().sourceRuasName)
        reopenedRepository.dismissFinish(id)
        assertNull(reopenedRepository.pendingFinish.first())
        assertNotNull(reopenedRepository.getSession(id))
    }

    @Test fun markOnlyPersistenceKeepsRunningTimerAndCreatesMarker() = runBlocking {
        val session = repository.createSession("Uji", 1000, 1000, 12000.0)
        val location = LocationSample(0.0, 0.0, null, 3f, 0f, 2000, 2000000000L)
        val snapshot = TrackingSnapshot(status = TrackingStatus.TRACKING, markerCount = 1,
            activeSegmentStartedElapsedMs = 1000, totalDistanceM = 10.0, startChainageM = 12000.0)
        repository.saveMarker(session, location, snapshot)
        val stored = repository.getSession(session.id)!!
        assertEquals(TrackingStatus.TRACKING, stored.status)
        assertEquals(1000L, stored.activeSegmentStartedElapsedRealtimeMs)
        assertEquals(12010.0, repository.getMarkers(session.id).single().chainageM, 0.0)
    }

    @Test fun activeAndPausedSurveysCannotBeDeleted() = runBlocking {
        for (status in listOf(TrackingStatus.TRACKING, TrackingStatus.PAUSED)) {
            val id = seed(status)
            assertFalse(repository.deleteFinishedSession(id))
            assertNotNull(repository.getSession(id))
            assertEquals(1, repository.getPoints(id).size)
            assertEquals(1, repository.getMarkers(id).size)
        }
    }

    @Test fun finishedSurveyWithoutPointsCanBeDeleted() = runBlocking {
        val id = database.sessionDao().insert(SurveySession(name = "Empty test survey", startedAt = 0L, status = TrackingStatus.FINISHED))
        assertTrue(repository.deleteFinishedSession(id))
        assertNull(repository.getSession(id))
    }

    // Synthetic fixtures solely for database integrity checks, not survey observations.
    private suspend fun seed(status: TrackingStatus): Long {
        val id = database.sessionDao().insert(SurveySession(name = "Test survey", startedAt = 0L, status = status))
        database.trackPointDao().insert(TrackPoint(
            sessionId = id, timestamp = 0L, elapsedRealtimeNanos = 0L,
            latitude = 0.0, longitude = 0.0, altitudeM = null, accuracyM = 1f,
            speedMps = 0f, distanceFromStartM = 0.0, chainageM = 0.0,
        ))
        database.markerDao().insert(SurveyMarker(
            sessionId = id, pointName = "TEST_001", timestamp = 0L,
            latitude = 0.0, longitude = 0.0, altitudeM = null, accuracyM = 1f,
            distanceM = 0.0, chainageM = 0.0, speedMps = 0f,
        ))
        return id
    }
}
