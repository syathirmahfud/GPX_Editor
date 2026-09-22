package com.syathir.roadgpssurvey.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.syathir.roadgpssurvey.model.TrackingStatus

class DatabaseConverters {
    @TypeConverter fun trackingStatusToString(value: TrackingStatus): String = value.name
    @TypeConverter fun stringToTrackingStatus(value: String): TrackingStatus = TrackingStatus.valueOf(value)
    @TypeConverter fun markerTypeToString(value: MarkerType): String = value.name
    @TypeConverter fun stringToMarkerType(value: String): MarkerType = MarkerType.valueOf(value)
}

@Database(
    entities = [SurveySession::class, TrackPoint::class, SurveyMarker::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SurveySessionDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun markerDao(): SurveyMarkerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE survey_sessions ADD COLUMN finishStep TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE survey_markers_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER, pointName TEXT NOT NULL, timestamp INTEGER NOT NULL,
                        latitude REAL NOT NULL, longitude REAL NOT NULL, altitudeM REAL,
                        accuracyM REAL NOT NULL, distanceM REAL NOT NULL, chainageM REAL NOT NULL,
                        speedMps REAL NOT NULL, note TEXT NOT NULL, markerType TEXT NOT NULL,
                        sourceRuasName TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(sessionId) REFERENCES survey_sessions(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO survey_markers_new
                    SELECT m.id, m.sessionId, m.pointName, m.timestamp, m.latitude, m.longitude,
                        m.altitudeM, m.accuracyM, m.distanceM, m.chainageM, m.speedMps,
                        m.note, m.markerType, COALESCE(s.name, '')
                    FROM survey_markers m LEFT JOIN survey_sessions s ON m.sessionId = s.id
                """.trimIndent())
                db.execSQL("DROP TABLE survey_markers")
                db.execSQL("ALTER TABLE survey_markers_new RENAME TO survey_markers")
                db.execSQL("CREATE INDEX index_survey_markers_sessionId ON survey_markers(sessionId)")
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "road-gps-survey.db",
        ).addMigrations(MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            })
            .build()
    }
}
