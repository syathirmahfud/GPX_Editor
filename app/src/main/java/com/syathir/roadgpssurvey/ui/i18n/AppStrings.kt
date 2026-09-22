package com.syathir.roadgpssurvey.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import com.syathir.roadgpssurvey.data.MarkerType

interface AppStrings {
    val languageCode: String
    val languageDisplayName: String
    val appName: String
    fun surveyPoints(count: Int): String

    // Metrics
    val distance: String
    val sta: String
    val time: String
    val gpsAccuracy: String
    val speed: String
    val speedUnit: String
    val waiting: String
    val accuracyGood: String
    val accuracyFair: String
    val accuracyPoor: String

    // Actions
    val start: String
    val pauseAndMark: String
    val mark: String
    val resume: String
    val finish: String
    val history: String
    val settings: String
    val back: String
    val save: String
    val cancel: String
    val delete: String
    val deleting: String
    val later: String
    val edit: String

    // Settings Screen
    val gpsSettings: String
    val language: String
    val languageIndonesian: String
    val languageEnglish: String
    val filterHint: String
    val themeHint: String
    val intervalLabel: String
    val intervalSupport: String
    val accuracyLabel: String
    val accuracySupport: String
    val movementLabel: String
    val movementSupport: String
    val staLabel: String
    val staSupport: String
    val overlayLabel: String
    val overlaySupport: String
    val saveSettings: String
    val settingsSaved: String
    val errorInterval: String
    val errorAccuracy: String
    val errorMovement: String
    val errorSta: String
    val attribution: String

    // History Screen
    val historyTitle: String
    val savedMarkers: String
    val noSessions: String
    val deleteSessionTitle: String
    fun deleteSessionMessage(name: String): String
    val exportSection: String

    // Detail & Markers Screen
    val sessionDetail: String
    val allMarkersTitle: String
    val noMarkers: String
    fun sourceRuas(name: String): String
    val detachedMarkerInfo: String
    val editMarker: String
    val deleteMarker: String
    val deleteMarkerTitle: String
    fun deleteMarkerMessage(name: String): String

    // Marker Edit Dialog
    val editMarkerTitle: String
    val pointName: String
    val markerTypeLabel: String
    val noteLabel: String

    // Finish Dialog
    val finishTitle: String
    val finishMessage: String
    val autoName: String
    val exportTitle: String
    fun exportMessage(name: String): String
    val exportCsv: String
    val exportGpx: String
    val exportKmz: String
    val exportAll: String
    val cancelExportHint: String

    // Marker Types
    fun markerTypeName(type: MarkerType): String
}

object IndonesianStrings : AppStrings {
    override val languageCode: String = "in"
    override val languageDisplayName: String = "Bahasa Indonesia"
    override val appName: String = "Survei GPS Jalan"
    override fun surveyPoints(count: Int): String = "SURVEI GPS JALAN · $count TITIK"

    override val distance: String = "JARAK"
    override val sta: String = "STA"
    override val time: String = "WAKTU"
    override val gpsAccuracy: String = "AKURASI GPS"
    override val speed: String = "KECEPATAN"
    override val speedUnit: String = "km/jam"
    override val waiting: String = "Menunggu…"
    override val accuracyGood: String = "BAIK"
    override val accuracyFair: String = "CUKUP"
    override val accuracyPoor: String = "BURUK"

    override val start: String = "MULAI"
    override val pauseAndMark: String = "JEDA + TITIK"
    override val mark: String = "TANDAI"
    override val resume: String = "LANJUT"
    override val finish: String = "SELESAI"
    override val history: String = "RIWAYAT"
    override val settings: String = "PENGATURAN"
    override val back: String = "KEMBALI"
    override val save: String = "SIMPAN"
    override val cancel: String = "BATAL"
    override val delete: String = "HAPUS"
    override val deleting: String = "MENGHAPUS…"
    override val later: String = "NANTI"
    override val edit: String = "UBAH TITIK"

    override val gpsSettings: String = "PENGATURAN GPS"
    override val language: String = "BAHASA"
    override val languageIndonesian: String = "Bahasa Indonesia"
    override val languageEnglish: String = "English"
    override val filterHint: String = "Nilai penyaringan ini berlaku saat perekaman ruas berikutnya dimulai."
    override val themeHint: String = "Tema otomatis mengikuti waktu perangkat: putih pukul 06.00–17.59, hitam pukul 18.00–05.59."
    override val intervalLabel: String = "Interval pembaruan GPS (detik)"
    override val intervalSupport: String = "Bawaan: 1 detik"
    override val accuracyLabel: String = "Batas akurasi yang diterima (m)"
    override val accuracySupport: String = "Lokasi dengan akurasi lebih buruk tidak dihitung ke jarak. Bawaan: 10 m"
    override val movementLabel: String = "Pergerakan minimum (m)"
    override val movementSupport: String = "Mengurangi pergeseran GPS saat diam. Bawaan: 2 m"
    override val staLabel: String = "STA awal"
    override val staSupport: String = "Contoh: 12+300"
    override val overlayLabel: String = "Panel melayang"
    override val overlaySupport: String = "Memerlukan izin tampil di atas aplikasi lain saat diaktifkan."
    override val saveSettings: String = "SIMPAN PENGATURAN"
    override val settingsSaved: String = "Pengaturan tersimpan"
    override val errorInterval: String = "Interval GPS minimal 0,5 detik"
    override val errorAccuracy: String = "Batas akurasi harus lebih besar dari 0 m"
    override val errorMovement: String = "Pergerakan minimum tidak boleh negatif"
    override val errorSta: String = "Gunakan format STA awal seperti 12+300"
    override val attribution: String = "Dibuat oleh SyathirMahfud dengan bantuan pengembangan dari ChatGPT (GPT-5.6 Sol)."

    override val historyTitle: String = "RIWAYAT RUAS"
    override val savedMarkers: String = "TITIK TERSIMPAN"
    override val noSessions: String = "Belum ada riwayat survei."
    override val deleteSessionTitle: String = "Hapus ruas?"
    override fun deleteSessionMessage(name: String): String =
        "Hapus ruas $name beserta jejak rekamannya? Titik penanda tetap tersedia di Titik Tersimpan. Berkas hasil ekspor tidak dihapus."
    override val exportSection: String = "EKSPOR"

    override val sessionDetail: String = "DETAIL RUAS"
    override val allMarkersTitle: String = "TITIK TERSIMPAN"
    override val noMarkers: String = "Belum ada titik penanda"
    override fun sourceRuas(name: String): String = "Ruas asal: $name"
    override val detachedMarkerInfo: String = "Ruas telah dihapus · titik tetap tersimpan"
    override val editMarker: String = "UBAH TITIK"
    override val deleteMarker: String = "HAPUS TITIK"
    override val deleteMarkerTitle: String = "Hapus titik?"
    override fun deleteMarkerMessage(name: String): String = "Hapus titik $name? Tindakan ini tidak dapat dibatalkan."

    override val editMarkerTitle: String = "Ubah Titik"
    override val pointName: String = "Nama Titik"
    override val markerTypeLabel: String = "Jenis Titik"
    override val noteLabel: String = "Catatan"

    override val finishTitle: String = "Nama Ruas"
    override val finishMessage: String = "Perekaman selesai dan data sudah tersimpan. Masukkan nama ruas jalan."
    override val autoName: String = "NAMA OTOMATIS"
    override val exportTitle: String = "Ekspor ruas"
    override fun exportMessage(name: String): String = "$name sudah tersimpan. Pilih berkas yang ingin diekspor."
    override val exportCsv: String = "CSV"
    override val exportGpx: String = "GPX"
    override val exportKmz: String = "KMZ"
    override val exportAll: String = "CSV, GPX DAN KMZ"
    override val cancelExportHint: String = "Membatalkan ekspor tidak menghapus data ruas."

    override fun markerTypeName(type: MarkerType): String = type.displayName
}

object EnglishStrings : AppStrings {
    override val languageCode: String = "en"
    override val languageDisplayName: String = "English"
    override val appName: String = "Road GPS Survey"
    override fun surveyPoints(count: Int): String = "ROAD GPS SURVEY · $count POINTS"

    override val distance: String = "DISTANCE"
    override val sta: String = "STA"
    override val time: String = "TIME"
    override val gpsAccuracy: String = "GPS ACCURACY"
    override val speed: String = "SPEED"
    override val speedUnit: String = "km/h"
    override val waiting: String = "Waiting…"
    override val accuracyGood: String = "GOOD"
    override val accuracyFair: String = "FAIR"
    override val accuracyPoor: String = "POOR"

    override val start: String = "START"
    override val pauseAndMark: String = "PAUSE + MARK"
    override val mark: String = "MARK"
    override val resume: String = "RESUME"
    override val finish: String = "FINISH"
    override val history: String = "HISTORY"
    override val settings: String = "SETTINGS"
    override val back: String = "BACK"
    override val save: String = "SAVE"
    override val cancel: String = "CANCEL"
    override val delete: String = "DELETE"
    override val deleting: String = "DELETING…"
    override val later: String = "LATER"
    override val edit: String = "EDIT POINT"

    override val gpsSettings: String = "GPS SETTINGS"
    override val language: String = "LANGUAGE"
    override val languageIndonesian: String = "Bahasa Indonesia"
    override val languageEnglish: String = "English"
    override val filterHint: String = "These filter values apply when recording the next section starts."
    override val themeHint: String = "Theme automatically follows device time: white from 06:00–17:59, black from 18:00–05:59."
    override val intervalLabel: String = "GPS update interval (seconds)"
    override val intervalSupport: String = "Default: 1 second"
    override val accuracyLabel: String = "Accepted accuracy threshold (m)"
    override val accuracySupport: String = "Locations with worse accuracy are not added to distance. Default: 10 m"
    override val movementLabel: String = "Minimum movement (m)"
    override val movementSupport: String = "Reduces GPS drift while stationary. Default: 2 m"
    override val staLabel: String = "Initial STA"
    override val staSupport: String = "Example: 12+300"
    override val overlayLabel: String = "Floating overlay"
    override val overlaySupport: String = "Requires overlay permission when enabled."
    override val saveSettings: String = "SAVE SETTINGS"
    override val settingsSaved: String = "Settings saved"
    override val errorInterval: String = "GPS interval must be at least 0.5 seconds"
    override val errorAccuracy: String = "Accuracy threshold must be greater than 0 m"
    override val errorMovement: String = "Minimum movement cannot be negative"
    override val errorSta: String = "Use initial STA format like 12+300"
    override val attribution: String = "Built by SyathirMahfud with development assistance from ChatGPT (GPT-5.6 Sol)."

    override val historyTitle: String = "SECTION HISTORY"
    override val savedMarkers: String = "SAVED POINTS"
    override val noSessions: String = "No survey history yet."
    override val deleteSessionTitle: String = "Delete section?"
    override fun deleteSessionMessage(name: String): String =
        "Delete section $name along with its tracks? Marked points will remain in Saved Points. Exported files will not be deleted."
    override val exportSection: String = "EXPORT"

    override val sessionDetail: String = "SECTION DETAIL"
    override val allMarkersTitle: String = "SAVED POINTS"
    override val noMarkers: String = "No marked points yet"
    override fun sourceRuas(name: String): String = "Source section: $name"
    override val detachedMarkerInfo: String = "Section was deleted · point remains saved"
    override val editMarker: String = "EDIT POINT"
    override val deleteMarker: String = "DELETE POINT"
    override val deleteMarkerTitle: String = "Delete point?"
    override fun deleteMarkerMessage(name: String): String = "Delete point $name? This action cannot be undone."

    override val editMarkerTitle: String = "Edit Point"
    override val pointName: String = "Point Name"
    override val markerTypeLabel: String = "Point Type"
    override val noteLabel: String = "Note"

    override val finishTitle: String = "Section Name"
    override val finishMessage: String = "Recording finished and data is saved. Enter the road section name."
    override val autoName: String = "AUTO NAME"
    override val exportTitle: String = "Export section"
    override fun exportMessage(name: String): String = "$name has been saved. Select files to export."
    override val exportCsv: String = "CSV"
    override val exportGpx: String = "GPX"
    override val exportKmz: String = "KMZ"
    override val exportAll: String = "CSV, GPX AND KMZ"
    override val cancelExportHint: String = "Canceling export does not delete section data."

    override fun markerTypeName(type: MarkerType): String = when (type) {
        MarkerType.BRIDGE -> "Bridge"
        MarkerType.CULVERT -> "Culvert"
        MarkerType.INTERSECTION -> "Intersection"
        MarkerType.PAVEMENT_CHANGE -> "Pavement Change"
        MarkerType.ROAD_DAMAGE -> "Road Damage"
        MarkerType.OTHER -> "Other"
    }
}

val LocalAppStrings = compositionLocalOf<AppStrings> { IndonesianStrings }

fun appStringsFor(code: String): AppStrings = when (code.lowercase()) {
    "en" -> EnglishStrings
    else -> IndonesianStrings
}
