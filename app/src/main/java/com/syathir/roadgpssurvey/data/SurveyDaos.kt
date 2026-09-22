package com.syathir.roadgpssurvey.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveySessionDao {
    @Insert
    suspend fun insert(session: SurveySession): Long

    @Update
    suspend fun update(session: SurveySession)

    @Query("SELECT * FROM survey_sessions WHERE id = :id")
    suspend fun get(id: Long): SurveySession?

    @Query("SELECT * FROM survey_sessions WHERE id = :id")
    fun observe(id: Long): Flow<SurveySession?>

    @Query("SELECT * FROM survey_sessions WHERE status IN ('TRACKING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<SurveySession?>

    @Query("SELECT * FROM survey_sessions WHERE status IN ('TRACKING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): SurveySession?

    @Query("SELECT * FROM survey_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SurveySession>>

    @Query("SELECT * FROM survey_sessions WHERE status = 'FINISHED' AND finishStep != '' ORDER BY finishedAt, id LIMIT 1")
    fun observePendingFinish(): Flow<SurveySession?>

    @Query("UPDATE survey_sessions SET name = :name, finishStep = 'EXPORT' WHERE id = :id AND status = 'FINISHED'")
    suspend fun nameFinished(id: Long, name: String): Int

    @Query("UPDATE survey_sessions SET finishStep = '' WHERE id = :id AND status = 'FINISHED'")
    suspend fun dismissFinish(id: Long)

    // Track samples cascade; marked points are detached through ON DELETE SET NULL.
    @Query("DELETE FROM survey_sessions WHERE id = :id AND status = 'FINISHED'")
    suspend fun deleteFinished(id: Long): Int
}

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(point: TrackPoint): Long

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp, id")
    suspend fun getForSession(sessionId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(sessionId: Long): TrackPoint?

    @Query("DELETE FROM track_points WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long): Int
}

@Dao
interface SurveyMarkerDao {
    @Insert
    suspend fun insert(marker: SurveyMarker): Long

    @Query("DELETE FROM survey_markers WHERE id = :id AND sessionId IS NULL")
    suspend fun deleteDetached(id: Long): Int

    @Query("UPDATE survey_markers SET sessionId = NULL WHERE sessionId = :sessionId")
    suspend fun detachFromSession(sessionId: Long): Int

    @Query("UPDATE survey_markers SET pointName = :name, note = :note, markerType = :type WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, note: String, type: MarkerType)

    @Query("UPDATE survey_markers SET sourceRuasName = :name WHERE sessionId = :sessionId")
    suspend fun renameSource(sessionId: Long, name: String)

    @Query("SELECT * FROM survey_markers ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<SurveyMarker>>

    @Query("SELECT * FROM survey_markers ORDER BY timestamp DESC, id DESC")
    suspend fun getAll(): List<SurveyMarker>

    @Query("SELECT * FROM survey_markers WHERE sessionId = :sessionId ORDER BY timestamp, id")
    fun observeForSession(sessionId: Long): Flow<List<SurveyMarker>>

    @Query("SELECT * FROM survey_markers WHERE sessionId = :sessionId ORDER BY timestamp, id")
    suspend fun getForSession(sessionId: Long): List<SurveyMarker>
}
