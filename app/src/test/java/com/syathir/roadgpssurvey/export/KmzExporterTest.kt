package com.syathir.roadgpssurvey.export

import com.syathir.roadgpssurvey.data.MarkerType
import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.data.TrackPoint
import com.syathir.roadgpssurvey.model.TrackingStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KmzExporterTest {
    @Test fun kmzContainsRoadLineAndMarkersAtOriginalCoordinates() {
        val data = SurveyExportData(
            session = SurveySession(
                id = 7,
                name = "Jalan Uji & Sungai",
                startedAt = 1_000,
                finishedAt = 2_000,
                totalDistanceM = 123.4,
                startChainageM = 5_000.0,
                status = TrackingStatus.FINISHED,
                markerCount = 1,
            ),
            points = listOf(
                point(1, -0.95000000, 100.35000000, 0.0),
                point(2, -0.95010000, 100.35020000, 22.5),
            ),
            markers = listOf(
                SurveyMarker(
                    id = 9,
                    sessionId = 7,
                    pointName = "POINT_001",
                    timestamp = 1_500,
                    latitude = -0.95012345,
                    longitude = 100.35054321,
                    altitudeM = 25.0,
                    accuracyM = 2.4f,
                    distanceM = 80.0,
                    chainageM = 5_080.0,
                    speedMps = 0f,
                    note = "Bridge & culvert",
                    markerType = MarkerType.BRIDGE,
                    sourceRuasName = "Jalan Uji & Sungai",
                ),
            ),
        )

        val bytes = ByteArrayOutputStream().also { KmzExporter().write(data, it) }.toByteArray()
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }

        assertEquals(setOf("doc.kml"), entries.keys)
        val kml = entries.getValue("doc.kml")
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("100.35000000,-0.95000000,0.00"))
        assertTrue(kml.contains("100.35020000,-0.95010000,22.50"))
        // Marker uses the exact recorded GNSS coordinate rather than a snapped track coordinate.
        assertTrue(kml.contains("100.35054321,-0.95012345,25.00"))
        assertTrue(kml.contains("<name>POINT_001</name>"))
        assertTrue(kml.contains("<value>5+080</value>"))
        assertTrue(kml.contains("Jalan Uji &amp; Sungai"))
        assertTrue(kml.contains("Bridge &amp; culvert"))
    }

    private fun point(id: Long, lat: Double, lon: Double, altitude: Double) = TrackPoint(
        id = id,
        sessionId = 7,
        timestamp = id * 1_000,
        elapsedRealtimeNanos = id * 1_000_000_000,
        latitude = lat,
        longitude = lon,
        altitudeM = altitude,
        accuracyM = 3f,
        speedMps = 0f,
        distanceFromStartM = (id - 1) * 20.0,
        chainageM = 5_000.0 + (id - 1) * 20.0,
    )
}
