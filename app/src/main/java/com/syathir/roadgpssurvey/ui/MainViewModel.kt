package com.syathir.roadgpssurvey.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syathir.roadgpssurvey.location.LocationTracker
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.service.TrackingService
import com.syathir.roadgpssurvey.tracking.TrackingRuntime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay

data class MainUiState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val distanceM: Double = 0.0,
    val startChainageM: Double = 0.0,
    val activeElapsedMs: Long = 0L,
    val accuracyM: Float? = null,
    val speedKmh: Double = 0.0,
    val locationError: String? = null,
    val markerCount: Int = 0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val locationTracker = LocationTracker(application)
    private val preview = MutableStateFlow(LocationPreviewState())
    private var previewJob: Job? = null
    private val ticker = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(500L)
        }
    }
    val state: StateFlow<MainUiState> = dashboardStates(
        TrackingRuntime.snapshot,
        (application as RoadGpsApplication).surveyRepository.activeSession,
        preview,
        ticker,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MainUiState())

    fun startLocationPreview() {
        if (TrackingRuntime.snapshot.value.status == TrackingStatus.TRACKING ||
            TrackingRuntime.snapshot.value.status == TrackingStatus.PAUSED
        ) return
        if (previewJob != null) return
        previewJob = locationTracker.updates(1_000L)
            .onEach { fix ->
                preview.value = LocationPreviewState(
                    accuracyM = fix.horizontalAccuracyM,
                    speedKmh = fix.speedMps * 3.6,
                )
            }
            .catch {
                preview.value = preview.value.copy(
                    error = "Lokasi GPS belum tersedia. Periksa GPS dan izin lokasi.",
                )
                previewJob = null
            }
            .launchIn(viewModelScope)
    }

    fun startTracking() {
        previewJob?.cancel()
        previewJob = null
        TrackingService.send(getApplication(), TrackingService.ACTION_START)
    }

    fun pauseAndMark() = TrackingService.send(getApplication(), TrackingService.ACTION_PAUSE_MARK)

    fun markOnly() = TrackingService.send(getApplication(), TrackingService.ACTION_MARK)

    fun resumeTracking() = TrackingService.send(getApplication(), TrackingService.ACTION_RESUME)

    fun finishTracking() = TrackingService.send(getApplication(), TrackingService.ACTION_FINISH)

}
