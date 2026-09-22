package com.syathir.roadgpssurvey.export

import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.data.TrackPoint
import com.syathir.roadgpssurvey.util.ChainageFormatter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exports a finished road survey as a Google Earth KMZ archive.
 *
 * Track points form one LineString. Saved markers remain at their original
 * recorded GNSS coordinates; they are deliberately not snapped to the line.
 */
class KmzExporter {
    fun write(data: SurveyExportData, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"))
            val writer = OutputStreamWriter(zip, Charsets.UTF_8)
            writeKml(data, writer)
            writer.flush()
            zip.closeEntry()
        }
    }

    internal fun writeKml(data: SurveyExportData, writer: Appendable) {
        writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        writer.appendLine("<kml xmlns=\"http://www.opengis.net/kml/2.2\">")
        writer.appendLine("  <Document>")
        writer.appendLine("    <name>${ExportFormatting.xmlEscape(data.session.name)}</name>")
        writer.appendLine("    <Style id=\"roadTrack\"><LineStyle><color>ff00a5ff</color><width>5</width></LineStyle></Style>")
        writer.appendLine("    <Style id=\"surveyPoint\"><IconStyle><scale>1.1</scale></IconStyle></Style>")
        writeTrack(data, writer)
        if (data.markers.isNotEmpty()) {
            writer.appendLine("    <Folder>")
            writer.appendLine("      <name>Saved Points</name>")
            data.markers.forEach { writeMarker(it, writer) }
            writer.appendLine("    </Folder>")
        }
        writer.appendLine("  </Document>")
        writer.appendLine("</kml>")
    }

    private fun writeTrack(data: SurveyExportData, writer: Appendable) {
        writer.appendLine("    <Placemark>")
        writer.appendLine("      <name>${ExportFormatting.xmlEscape(data.session.name)} - Road Track</name>")
        writer.appendLine("      <styleUrl>#roadTrack</styleUrl>")
        writer.appendLine("      <description>${ExportFormatting.xmlEscape(trackDescription(data))}</description>")
        writer.appendLine("      <LineString>")
        writer.appendLine("        <tessellate>1</tessellate>")
        writer.appendLine("        <altitudeMode>clampToGround</altitudeMode>")
        writer.appendLine("        <coordinates>")
        data.points.forEach { point ->
            writer.appendLine("          ${coordinate(point)}")
        }
        writer.appendLine("        </coordinates>")
        writer.appendLine("      </LineString>")
        writer.appendLine("    </Placemark>")
    }

    private fun writeMarker(marker: SurveyMarker, writer: Appendable) {
        writer.appendLine("      <Placemark>")
        writer.appendLine("        <name>${ExportFormatting.xmlEscape(marker.pointName)}</name>")
        writer.appendLine("        <styleUrl>#surveyPoint</styleUrl>")
        writer.appendLine("        <description>${ExportFormatting.xmlEscape(markerDescription(marker))}</description>")
        writer.appendLine("        <ExtendedData>")
        dataField("STA", ChainageFormatter.format(marker.chainageM), writer)
        dataField("Type", marker.markerType.displayName, writer)
        dataField("AccuracyM", format(marker.accuracyM.toDouble(), 1), writer)
        dataField("Timestamp", ExportFormatting.isoTimestamp(marker.timestamp), writer)
        if (marker.note.isNotBlank()) dataField("Note", marker.note, writer)
        writer.appendLine("        </ExtendedData>")
        writer.appendLine("        <Point>")
        writer.appendLine("          <altitudeMode>clampToGround</altitudeMode>")
        writer.appendLine("          <coordinates>${coordinate(marker)}</coordinates>")
        writer.appendLine("        </Point>")
        writer.appendLine("      </Placemark>")
    }

    private fun dataField(name: String, value: String, writer: Appendable) {
        writer.appendLine("          <Data name=\"${ExportFormatting.xmlEscape(name)}\"><value>${ExportFormatting.xmlEscape(value)}</value></Data>")
    }

    private fun trackDescription(data: SurveyExportData): String = buildString {
        append("Road: ${data.session.name}\n")
        append("Distance: ${format(data.session.totalDistanceM / 1_000.0, 3)} km\n")
        append("Start STA: ${ChainageFormatter.format(data.session.startChainageM)}\n")
        append("End STA: ${ChainageFormatter.format(data.session.startChainageM + data.session.totalDistanceM)}\n")
        append("Track points: ${data.points.size}\n")
        append("Saved points: ${data.markers.size}")
    }

    private fun markerDescription(marker: SurveyMarker): String = buildString {
        append("STA: ${ChainageFormatter.format(marker.chainageM)}\n")
        append("Type: ${marker.markerType.displayName}\n")
        append("Accuracy: ±${format(marker.accuracyM.toDouble(), 1)} m\n")
        append("Time: ${ExportFormatting.isoTimestamp(marker.timestamp)}")
        if (marker.note.isNotBlank()) append("\nNote: ${marker.note}")
    }

    private fun coordinate(point: TrackPoint): String =
        "${format(point.longitude, 8)},${format(point.latitude, 8)},${format(point.altitudeM ?: 0.0, 2)}"

    private fun coordinate(marker: SurveyMarker): String =
        "${format(marker.longitude, 8)},${format(marker.latitude, 8)},${format(marker.altitudeM ?: 0.0, 2)}"

    private fun format(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)
}
