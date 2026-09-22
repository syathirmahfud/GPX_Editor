package com.syathir.roadgpssurvey.export

import java.io.Writer
import java.util.Locale

class GpxExporter : SurveyExporter {
    override fun write(data: SurveyExportData, writer: Writer) {
        writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        writer.appendLine("<gpx version=\"1.1\" creator=\"Road GPS Survey\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
        data.markers.forEach { marker ->
            writer.append("  <wpt lat=\"").append(decimal(marker.latitude, 8))
                .append("\" lon=\"").append(decimal(marker.longitude, 8)).appendLine("\">")
            marker.altitudeM?.let { writer.appendLine("    <ele>${decimal(it, 3)}</ele>") }
            writer.appendLine("    <time>${ExportFormatting.isoTimestamp(marker.timestamp)}</time>")
            writer.appendLine("    <name>${ExportFormatting.xmlEscape(marker.pointName)}</name>")
            writer.appendLine("    <type>${ExportFormatting.xmlEscape(marker.markerType.displayName)}</type>")
            if (marker.note.isNotBlank()) {
                writer.appendLine("    <desc>${ExportFormatting.xmlEscape(marker.note)}</desc>")
            }
            writer.appendLine("  </wpt>")
        }
        writer.appendLine("  <trk>")
        writer.appendLine("    <name>${ExportFormatting.xmlEscape(data.session.name)}</name>")
        writer.appendLine("    <trkseg>")
        data.points.forEach { point ->
            writer.append("      <trkpt lat=\"").append(decimal(point.latitude, 8))
                .append("\" lon=\"").append(decimal(point.longitude, 8)).appendLine("\">")
            point.altitudeM?.let { writer.appendLine("        <ele>${decimal(it, 3)}</ele>") }
            writer.appendLine("        <time>${ExportFormatting.isoTimestamp(point.timestamp)}</time>")
            writer.appendLine("      </trkpt>")
        }
        writer.appendLine("    </trkseg>")
        writer.appendLine("  </trk>")
        writer.appendLine("</gpx>")
        writer.flush()
    }

    private fun decimal(value: Double, places: Int): String =
        String.format(Locale.US, "%.${places}f", value)
}

