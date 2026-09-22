package com.syathir.roadgpssurvey.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.SurveyMarker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedMarkersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RoadGpsApplication).surveyRepository
    val state = repository.allMarkers.map { SessionDetailState(markers = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailState())
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableDeletingId = MutableStateFlow<Long?>(null)
    val deletingId = mutableDeletingId.asStateFlow()

    fun deleteMarker(id: Long) {
        if (mutableDeletingId.value != null) return
        mutableDeletingId.value = id
        mutableError.value = null
        viewModelScope.launch {
            try {
                if (!repository.deleteRetainedMarker(id)) {
                    mutableError.value = "Titik sudah tidak tersedia atau masih terhubung ke ruas."
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableError.value = "Titik gagal dihapus. Silakan coba lagi." }
            finally { mutableDeletingId.value = null }
        }
    }

    fun updateMarker(marker: SurveyMarker) {
        viewModelScope.launch {
            try { repository.updateMarker(marker); mutableError.value = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableError.value = "Perubahan titik belum tersimpan. Silakan coba lagi." }
        }
    }
}
