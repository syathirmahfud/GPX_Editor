package com.syathir.roadgpssurvey.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.tracking.TrackingRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class HistoryDeletionState(val deletingId: Long? = null, val error: String? = null)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RoadGpsApplication).surveyRepository
    private val mutableDeletion = MutableStateFlow(HistoryDeletionState())
    val deletion = mutableDeletion.asStateFlow()

    val sessions: StateFlow<List<SurveySession>> =
        repository.allSessions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            emptyList(),
        )

    fun deleteSession(id: Long) {
        if (mutableDeletion.value.deletingId != null) return
        mutableDeletion.value = HistoryDeletionState(deletingId = id)
        viewModelScope.launch {
            try {
                val deleted = repository.deleteFinishedSession(id)
                if (deleted) TrackingRuntime.clearDeletedSession(id)
                mutableDeletion.value = HistoryDeletionState(
                    error = if (deleted) null else "Ruas belum dapat dihapus. Selesaikan perekaman terlebih dahulu.",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableDeletion.value = HistoryDeletionState(error = "Ruas gagal dihapus. Silakan coba lagi.")
            }
        }
    }
}
