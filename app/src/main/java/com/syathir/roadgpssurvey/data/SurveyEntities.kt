package com.syathir.roadgpssurvey.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.syathir.roadgpssurvey.model.TrackingStatus

@Entity(tableName = "survey_sessions")
data class SurveySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val activeElapsedMs: Long = 0L,
    val activeSegmentStartedElapsedRealtimeMs: Long? = null,
    val totalDistanceM: Double = 0.0,
    val startChainageM: Double = 0.0,
    val status: TrackingStatus = TrackingStatus.IDLE,
    val markerCount: Int = 0,
    @ColumnInfo(defaultValue = "''") val finishStep: String = "",
)

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = SurveySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val timestamp: Long,
    val elapsedRealtimeNanos: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val accuracyM: Float,
    val speedMps: Float,
    val distanceFromStartM: Double,
    val chainageM: Double,
)

@Entity(
    tableName = "survey_markers",
    foreignKeys = [
        ForeignKey(
            entity = SurveySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SurveyMarker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long?,
    val pointName: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val accuracyM: Float,
    val distanceM: Double,
    val chainageM: Double,
    val speedMps: Float,
    val note: String = "",
    val markerType: MarkerType = MarkerType.OTHER,
    @ColumnInfo(defaultValue = "''") val sourceRuasName: String = "",
)

enum class MarkerType(val displayName: String) {
    BRIDGE("Jembatan"),
    CULVERT("Gorong-gorong"),
    INTERSECTION("Persimpangan"),
    PAVEMENT_CHANGE("Perubahan Perkerasan"),
    ROAD_DAMAGE("Kerusakan Jalan"),
    OTHER("Lainnya"),
}
