package com.syathir.roadgpssurvey.export

import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.data.TrackPoint
import java.io.Writer

enum class ExportFormat(val extension: String) {
    CSV("csv"),
    GPX("gpx"),
    KMZ("kmz"),
}

data class SurveyExportData(
    val session: SurveySession,
    val points: List<TrackPoint>,
    val markers: List<SurveyMarker>,
)

interface SurveyExporter {
    fun write(data: SurveyExportData, writer: Writer)
}
