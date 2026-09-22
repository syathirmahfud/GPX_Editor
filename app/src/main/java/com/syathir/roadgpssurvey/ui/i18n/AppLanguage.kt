package com.syathir.roadgpssurvey.ui.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Saved app preference, independent of the phone language. Updated by the application. */
object AppLanguage {
    private val selected = MutableStateFlow("in")
    val code = selected.asStateFlow()
    val strings: AppStrings get() = appStringsFor(selected.value)

    fun select(code: String) { selected.value = if (code == "en") "en" else "in" }

    fun context(context: Context, code: String = selected.value): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(if (code == "en") "en" else "id"))
        return context.createConfigurationContext(config)
    }
}

/** Translate only known application messages; never alter names or survey notes. */
fun AppStrings.message(text: String): String = if (languageCode != "en") text else englishMessages[text] ?: text

private val englishMessages = mapOf(
    "Lokasi GPS belum tersedia. Periksa GPS dan izin lokasi." to "GPS location is unavailable. Check GPS and location permission.",
    "Ruas belum dapat dihapus. Selesaikan perekaman terlebih dahulu." to "Finish recording before deleting the section.",
    "Ruas gagal dihapus. Silakan coba lagi." to "Could not delete the section. Please try again.",
    "Titik sudah tidak tersedia atau masih terhubung ke ruas." to "This point no longer exists or is still linked to a section.",
    "Titik gagal dihapus. Silakan coba lagi." to "Could not delete the point. Please try again.",
    "Titik tidak tersedia atau ruas masih aktif. Selesaikan perekaman terlebih dahulu." to "The point is unavailable or its section is still active. Finish recording before deleting it.",
    "Perubahan titik belum tersimpan. Silakan coba lagi." to "Point changes were not saved. Please try again.",
    "Belum berhasil menyimpan. Silakan coba lagi." to "Could not save. Please try again.",
    "Tindakan gagal disimpan. Silakan coba lagi." to "The action could not be saved. Please try again.",
    "Perekaman dijeda karena perangkat dimulai ulang" to "Recording paused because the device restarted",
    "Pembaruan lokasi terhenti. Periksa GPS dan izin lokasi." to "Location updates stopped. Check GPS and location permission.",
    "Lokasi GPS belum tersedia; titik belum disimpan dan perekaman tetap berjalan." to "GPS is unavailable; the point was not saved and recording continues.",
    "Lokasi GPS terbaru belum tersedia; perekaman masih dijeda." to "A fresh GPS location is unavailable; recording remains paused.",
    "Notifikasi nonaktif; gunakan aplikasi atau panel melayang untuk mengendalikan perekaman" to "Notifications are off; use the app or floating panel to control recording",
    "Izin tampil di atas aplikasi lain belum diberikan" to "Overlay permission was not granted",
    "Ekspor gagal. Data ruas tetap tersimpan; coba lagi melalui Riwayat." to "Export failed. Section data is still saved; retry from History.",
    "Pemilih berkas tidak dapat dibuka. Data ruas tetap tersimpan." to "The file picker could not open. Section data is still saved.",
    "Ekspor CSV tersimpan" to "CSV export saved",
    "Ekspor GPX tersimpan" to "GPX export saved",
    "Ekspor KMZ tersimpan" to "KMZ export saved",
    "Menunggu GPS" to "Waiting for GPS",
    "Sembunyikan panel melayang" to "Hide floating panel",
    "Perekaman selesai" to "Recording finished",
    "Ketuk untuk mengisi Nama Ruas dan memilih ekspor." to "Tap to enter the section name and choose an export.",
    "SURVEI GPS — DIJEDA" to "GPS SURVEY — PAUSED",
    "SURVEI GPS — MEREKAM" to "GPS SURVEY — RECORDING",
)
