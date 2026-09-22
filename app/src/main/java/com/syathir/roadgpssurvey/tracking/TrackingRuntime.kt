package com.syathir.roadgpssurvey.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.syathir.roadgpssurvey.model.TrackingStatus

object TrackingRuntime {
    private val mutableSnapshot = MutableStateFlow(TrackingSnapshot())
    val snapshot: StateFlow<TrackingSnapshot> = mutableSnapshot.asStateFlow()

    fun publish(value: TrackingSnapshot) {
        mutableSnapshot.value = value
    }

    fun clearDeletedSession(sessionId: Long) {
        mutableSnapshot.update { current ->
            if (current.sessionId == sessionId && current.status == TrackingStatus.FINISHED) {
                TrackingSnapshot(accuracyM = current.accuracyM)
            } else current
        }
    }
}
