package com.syathir.roadgpssurvey.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.syathir.roadgpssurvey.model.TrackingStatus
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationTest {
    @Test fun upgradeFromVersionOnePreservesSurveyTrackAndMarkers() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val schema = JSONObject(File("schemas/com.syathir.roadgpssurvey.data.AppDatabase/1.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            for (i in 0 until schema.length()) {
                val entity = schema.getJSONObject(i)
                val tableName = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName))
                }
            }
            // Synthetic old-install data. No real survey observations are fabricated.
            old.execSQL("INSERT INTO survey_sessions VALUES (1, 'Ruas Lama', 1000, 2000, 1000, NULL, 12.0, 100.0, 'FINISHED', 1)")
            old.execSQL("INSERT INTO track_points VALUES (1, 1, 1000, 1000000000, 0.0, 0.0, NULL, 3.0, 0.0, 12.0, 112.0)")
            old.execSQL("INSERT INTO survey_markers VALUES (1, 1, 'POINT_001', 1000, 0.0, 0.0, NULL, 3.0, 12.0, 112.0, 0.0, 'Catatan lama', 'BRIDGE')")
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val repository = SurveyRepository(upgraded)
            assertEquals(TrackingStatus.FINISHED, repository.getSession(1)!!.status)
            assertEquals(1, repository.getPoints(1).size)
            val marker = repository.getMarkers(1).single()
            assertEquals("Catatan lama", marker.note)
            assertEquals("Ruas Lama", marker.sourceRuasName)
            assertEquals(112.0, marker.chainageM, 0.0)
            assertNull(repository.pendingFinish.first())
            assertTrue(repository.deleteFinishedSession(1))
            assertTrue(repository.getPoints(1).isEmpty())
            val retained = upgraded.markerDao().getAll().single()
            assertNull(retained.sessionId)
            assertEquals(marker.copy(sessionId = null), retained)
        } finally { upgraded.close() }
    }
}
