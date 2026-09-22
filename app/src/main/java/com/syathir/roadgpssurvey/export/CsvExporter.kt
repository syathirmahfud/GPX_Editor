package com.syathir.roadgpssurvey.export

import com.syathir.roadgpssurvey.util.ChainageFormatter
import java.io.Writer
import java.util.Locale

class CsvExporter : SurveyExporter {
    override fun write(data: SurveyExportData, writer: Writer) {
        writer.appendLine(
            "timestamp,latitude,longitude,altitude,accuracy_m,speed_kmh,distance_m,sta,point_type,note,point_name",
        )
        data.points.forEach { point ->
            writeRow(
                writer,
                listOf(
                    ExportFormatting.isoTimestamp(point.timestamp),
                    decimal(point.latitude, 8),
                    decimal(point.longitude, 8),
                    point.altitudeM?.let { decimal(it, 3) }.orEmpty(),
                    decimal(point.accuracyM.toDouble(), 1),
                    decimal(point.speedMps * 3.6, 1),
                    decimal(point.distanceFromStartM, 3),
                    ChainageFormatter.format(point.chainageM),
                    "",
                    "",
                    "",
                ),
            )
        }
        data.markers.forEach { marker ->
            writeRow(
                writer,
                listOf(
                    ExportFormatting.isoTimestamp(marker.timestamp),
                    decimal(marker.latitude, 8),
                    decimal(marker.longitude, 8),
                    marker.altitudeM?.let { decimal(it, 3) }.orEmpty(),
                    decimal(marker.accuracyM.toDouble(), 1),
                    decimal(marker.speedMps * 3.6, 1),
                    decimal(marker.distanceM, 3),
                    ChainageFormatter.format(marker.chainageM),
                    marker.markerType.displayName,
                    marker.note,
                    marker.pointName,
                ),
            )
        }
        writer.flush()
    }

    private fun writeRow(writer: Writer, values: List<String>) {
        writer.appendLine(values.joinToString(",", transform = ::escapeCsv))
    }

    private fun escapeCsv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun decimal(value: Double, places: Int): String =
        String.format(Locale.US, "%.${places}f", value)
}

