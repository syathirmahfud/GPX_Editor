package com.syathir.roadgpssurvey.export

import android.content.Context
import android.net.Uri
import com.syathir.roadgpssurvey.data.SurveyRepository
import com.syathir.roadgpssurvey.model.TrackingStatus
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportCoordinator(
    private val context: Context,
    private val repository: SurveyRepository,
) {
    suspend fun export(sessionId: Long, format: ExportFormat, destination: Uri) = withContext(Dispatchers.IO) {
        val session = requireNotNull(repository.getSession(sessionId)) { "Ruas sudah tidak tersedia" }
        require(session.status == TrackingStatus.FINISHED) { "Selesaikan perekaman sebelum mengekspor ruas" }
        val data = SurveyExportData(
            session = session,
            points = repository.getPoints(sessionId),
            markers = repository.getMarkers(sessionId),
        )
        val exporter: SurveyExporter = when (format) {
            ExportFormat.CSV -> CsvExporter()
            ExportFormat.GPX -> GpxExporter()
        }
        val output = requireNotNull(context.contentResolver.openOutputStream(destination, "wt")) {
            "Lokasi penyimpanan ekspor tidak dapat dibuka"
        }
        output.use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer -> exporter.write(data, writer) }
        }
    }
}
