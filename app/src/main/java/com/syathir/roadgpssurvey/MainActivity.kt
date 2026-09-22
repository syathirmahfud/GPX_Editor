package com.syathir.roadgpssurvey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.syathir.roadgpssurvey.export.ExportCoordinator
import com.syathir.roadgpssurvey.export.ExportFormat
import com.syathir.roadgpssurvey.export.ExportFileName
import com.syathir.roadgpssurvey.overlay.TrackingOverlayService
import com.syathir.roadgpssurvey.ui.MainViewModel
import com.syathir.roadgpssurvey.ui.SurveyApp
import com.syathir.roadgpssurvey.ui.theme.RoadGpsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val app: RoadGpsApplication get() = application as RoadGpsApplication
    private val exportCoordinator by lazy { ExportCoordinator(this, app.surveyRepository) }
    private var startAfterPermission = false
    private val exportQueue = mutableListOf<Pair<Long, ExportFormat>>()
    private var exportPickerOpen = false
    private var preparingExport = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (hasLocationPermission()) {
            if (startAfterPermission) {
                startAfterPermission = false
                viewModel.startTracking()
            } else {
                viewModel.startLocationPreview()
            }
            if (!hasNotificationPermission()) {
                Toast.makeText(
                    this,
                    "Notifikasi nonaktif; gunakan aplikasi atau panel melayang untuk mengendalikan perekaman",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(this)) {
            setOverlayEnabled(true)
        } else {
            Toast.makeText(this, "Izin tampil di atas aplikasi lain belum diberikan", Toast.LENGTH_LONG).show()
        }
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { destination ->
        exportPickerOpen = false
        val request = exportQueue.removeFirstOrNull()
        if (destination != null && request != null) {
            lifecycleScope.launch {
                runCatching { exportCoordinator.export(request.first, request.second, destination) }
                    .onSuccess {
                        Toast.makeText(this@MainActivity, "Ekspor ${request.second.name} tersimpan", Toast.LENGTH_LONG).show()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        Toast.makeText(
                            this@MainActivity,
                            "Ekspor gagal. Data ruas tetap tersimpan; coba lagi melalui Riwayat.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                launchNextExport()
            }
        } else launchNextExport()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedInstanceState?.let { saved ->
            val ids = saved.getLongArray("exportIds") ?: longArrayOf()
            val formats = saved.getStringArrayList("exportFormats") ?: arrayListOf()
            ids.zip(formats).forEach { (id, format) -> exportQueue.add(id to ExportFormat.valueOf(format)) }
            exportPickerOpen = saved.getBoolean("exportPickerOpen")
        }
        if (!exportPickerOpen) launchNextExport()
        setContent {
            RoadGpsTheme {
                LaunchedEffect(Unit) {
                    if (hasLocationPermission()) viewModel.startLocationPreview()
                    val settings = app.settingsRepository.settings.first()
                    if (settings.overlayEnabled && canDrawOverlay()) TrackingOverlayService.show(this@MainActivity)
                }
                SurveyApp(
                    mainViewModel = viewModel,
                    onStartRequested = ::startWithPermission,
                    onOverlayChange = ::handleOverlayChange,
                    onExport = ::requestExport,
                    onExportMultiple = ::requestExports,
                )
            }
        }
    }

    private fun startWithPermission() {
        if (hasLocationPermission() && hasNotificationPermission()) viewModel.startTracking()
        else {
            startAfterPermission = true
            requestRuntimePermissions()
        }
    }

    private fun handleOverlayChange(enabled: Boolean) {
        if (!enabled) {
            setOverlayEnabled(false)
        } else if (canDrawOverlay()) {
            setOverlayEnabled(true)
        } else {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
            )
        }
    }

    private fun setOverlayEnabled(enabled: Boolean) {
        lifecycleScope.launch {
            val current = app.settingsRepository.settings.first()
            app.settingsRepository.update(current.copy(overlayEnabled = enabled))
            if (enabled) TrackingOverlayService.show(this@MainActivity)
            else TrackingOverlayService.hide(this@MainActivity)
        }
    }

    private fun requestExport(sessionId: Long, format: ExportFormat) {
        requestExports(sessionId, listOf(format))
    }

    private fun requestExports(sessionId: Long, formats: List<ExportFormat>) {
        exportQueue.addAll(formats.map { sessionId to it })
        launchNextExport()
    }

    private fun launchNextExport() {
        if (exportPickerOpen || preparingExport) return
        val request = exportQueue.firstOrNull() ?: return
        preparingExport = true
        lifecycleScope.launch {
            try {
                val session = app.surveyRepository.getSession(request.first)
                exportPickerOpen = true
                exportLauncher.launch(ExportFileName.forSession(session?.name ?: "Ruas", request.first, request.second))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                exportPickerOpen = false
                exportQueue.removeFirstOrNull()
                Toast.makeText(this@MainActivity, "Pemilih berkas tidak dapat dibuka. Data ruas tetap tersimpan.", Toast.LENGTH_LONG).show()
                preparingExport = false
                launchNextExport()
            } finally {
                preparingExport = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray("exportIds", exportQueue.map { it.first }.toLongArray())
        outState.putStringArrayList("exportFormats", ArrayList(exportQueue.map { it.second.name }))
        outState.putBoolean("exportPickerOpen", exportPickerOpen)
        super.onSaveInstanceState(outState)
    }

    private fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(this)

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
