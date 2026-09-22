package com.syathir.roadgpssurvey.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.data.SurveySession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class SessionDetailState(
    val session: SurveySession? = null,
    val markers: List<SurveyMarker> = emptyList(),
    val loading: Boolean = true,
)

class SessionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as RoadGpsApplication).surveyRepository
    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val state: StateFlow<SessionDetailState> = combine(
        repository.observeSession(sessionId),
        repository.observeMarkers(sessionId),
    ) { session, markers -> SessionDetailState(session, markers, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SessionDetailState())

    fun updateMarker(marker: SurveyMarker) {
        viewModelScope.launch {
            try { repository.updateMarker(marker); mutableError.value = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableError.value = "Perubahan titik belum tersimpan. Silakan coba lagi." }
        }
    }
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
}
