package com.syathir.roadgpssurvey.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.ui.history.HistoryScreen
import com.syathir.roadgpssurvey.ui.theme.RoadGpsTheme
import com.syathir.roadgpssurvey.ui.history.SessionDetailScreen
import com.syathir.roadgpssurvey.ui.history.SessionDetailState
import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.export.ExportFormat
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardHistoryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cardsStayEqualAndStableWhenValuesAndTrackingStateChange() {
        val state = mutableStateOf(MainUiState())
        compose.setContent {
            RoadGpsTheme(darkTheme = false) { MainScreen(state.value, {}, {}, {}, {}, {}, {}) }
        }
        val before = bounds("GPS ACCURACY")
        assertEquals(before.height, bounds("SPEED").height, 0.1f)
        // Equal weights can distribute an odd physical pixel between the two rows.
        assertEquals(before.height, bounds("TIME").height, 1f)
        compose.onNodeWithText("MULAI").assertIsDisplayed()
        compose.runOnIdle {
            state.value = MainUiState(status = TrackingStatus.TRACKING, accuracyM = 7.9f,
                distanceM = 123456.0, activeElapsedMs = 360000000L, speedKmh = 123.4)
        }
        assertEquals(before, bounds("GPS ACCURACY"))
        compose.onNodeWithText("CUKUP").assertIsDisplayed()
        compose.onNodeWithText("JEDA + TITIK").assertIsDisplayed()
        compose.onNodeWithText("SELESAI").assertIsDisplayed()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val finish = compose.onNodeWithText("SELESAI").fetchSemanticsNode().boundsInRoot
        assertTrue(finish.bottom <= root.bottom)
        capture("dashboard-portrait")
    }

    @Test @Config(qualifiers = "w800dp-h360dp-mdpi")
    fun landscapeKeepsControlsAndAllMetricsVisible() {
        compose.setContent {
            RoadGpsTheme(darkTheme = false) { MainScreen(MainUiState(status = TrackingStatus.PAUSED, accuracyM = 7.9f), {}, {}, {}, {}, {}, {}) }
        }
        listOf("DISTANCE", "STA", "TIME", "GPS ACCURACY", "SPEED").forEach {
            compose.onNodeWithTag("metric-$it").assertIsDisplayed()
            val node = compose.onNodeWithTag("metric-$it").fetchSemanticsNode()
            assertEquals(node.layoutInfo.height.toFloat(), node.boundsInRoot.height, 0.1f)
        }
        compose.onNodeWithText("LANJUT").assertIsDisplayed()
        compose.onNodeWithText("SELESAI").assertIsDisplayed()
        capture("dashboard-landscape")
    }

    @Test @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun smallScreenKeepsActionsVisibleAndMetricsScrollable() {
        compose.setContent {
            MaterialTheme { MainScreen(MainUiState(status = TrackingStatus.TRACKING), {}, {}, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("SELESAI").assertIsDisplayed()
        compose.onNodeWithTag("metric-SPEED").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("JEDA + TITIK").assertIsDisplayed()
    }

    @Test fun deletionRequiresConfirmationAndCancelDoesNothing() {
        var deletedId: Long? = null
        val session = SurveySession(id = 42, name = "Test survey", startedAt = 0, status = TrackingStatus.FINISHED)
        compose.setContent {
            MaterialTheme { HistoryScreen(listOf(session), {}, {}, { _, _ -> }, { deletedId = it }) }
        }
        compose.onNodeWithText("HAPUS").performClick()
        assertNull(deletedId)
        compose.onNodeWithText("BATAL").performClick()
        assertNull(deletedId)
        compose.onNodeWithText("HAPUS").performClick()
        compose.onAllNodesWithText("HAPUS").onLast().performClick()
        compose.runOnIdle { assertEquals(42L, deletedId) }
    }

    private fun bounds(label: String) = compose.onNodeWithTag("metric-$label").fetchSemanticsNode().boundsInRoot

    @Test fun markOnlyButtonDoesNotInvokePause() {
        val state = mutableStateOf(MainUiState(status = TrackingStatus.TRACKING))
        var pauseCalls = 0
        compose.setContent {
            RoadGpsTheme(darkTheme = false) {
                MainScreen(state.value, {}, { pauseCalls++ }, {}, {}, {}, {},
                    onMarkOnly = { state.value = state.value.copy(markerCount = state.value.markerCount + 1) })
            }
        }
        compose.onNodeWithText("TANDAI").performClick()
        compose.onNodeWithText("SURVEI GPS JALAN · 1 TITIK").assertIsDisplayed()
        compose.onNodeWithText("JEDA + TITIK").assertIsDisplayed()
        assertEquals(0, pauseCalls)
    }

    @Test fun emptyDetailHasReadableDayAndNightBackgrounds() {
        val night = mutableStateOf(false)
        val session = SurveySession(id = 1, name = "Ruas dengan nama panjang untuk pengujian layar detail", startedAt = 0)
        compose.setContent {
            RoadGpsTheme(darkTheme = night.value) {
                SessionDetailScreen(SessionDetailState(session, loading = false), {}, {})
            }
        }
        compose.onNodeWithText("Belum ada titik penanda").assertIsDisplayed()
        compose.onNodeWithText("KEMBALI").assertIsDisplayed()
        val dayImage = compose.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(android.graphics.Color.WHITE, dayImage.getPixel(5, dayImage.height - 5))
        capture("empty-detail-day")
        compose.runOnIdle { night.value = true }
        compose.onNodeWithText("Belum ada titik penanda").assertIsDisplayed()
        val nightImage = compose.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(android.graphics.Color.BLACK, nightImage.getPixel(5, nightImage.height - 5))
        capture("empty-detail-night")
    }

    @Test fun finishAsksNameThenOffersBothFormats() {
        val session = mutableStateOf(SurveySession(id = 1, name = "RUAS_TEST", startedAt = 0,
            status = TrackingStatus.FINISHED, finishStep = "NAME"))
        var selected: List<ExportFormat>? = null
        compose.setContent {
            RoadGpsTheme(darkTheme = false) {
                FinishSurveyDialog(session.value, false, null,
                    { session.value = session.value.copy(name = it, finishStep = "EXPORT") }, { selected = it })
            }
        }
        compose.onNodeWithText("SIMPAN").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("Jalan Uji")
        compose.onNodeWithText("SIMPAN").performClick()
        compose.onNodeWithText("Ekspor ruas").assertIsDisplayed()
        compose.onNodeWithText("CSV DAN GPX").performClick()
        compose.runOnIdle {
            assertEquals("Jalan Uji", session.value.name)
            assertEquals(listOf(ExportFormat.CSV, ExportFormat.GPX), selected)
        }
    }

    @Test fun preservedMarkerRemainsVisibleAfterRuasDeletion() {
        val marker = SurveyMarker(id = 1, sessionId = null, sourceRuasName = "Ruas Lama", pointName = "POINT_001",
            timestamp = 0, latitude = 0.0, longitude = 0.0, altitudeM = null,
            accuracyM = 3f, distanceM = 0.0, chainageM = 0.0, speedMps = 0f)
        compose.setContent {
            RoadGpsTheme(darkTheme = true) {
                SessionDetailScreen(SessionDetailState(markers = listOf(marker), loading = false), {}, {}, allMarkers = true)
            }
        }
        compose.onNodeWithText("POINT_001").assertIsDisplayed()
        compose.onNodeWithText("Ruas asal: Ruas Lama").assertIsDisplayed()
        compose.onNodeWithText("Ruas telah dihapus · titik tetap tersimpan").assertIsDisplayed()
        compose.onNodeWithText("UBAH TITIK").performClick()
        compose.onNodeWithText("SIMPAN").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val target = File("build/test-screenshots/$name.png")
        target.parentFile?.mkdirs()
        target.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
