package com.syathir.roadgpssurvey.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.GpsSettings
import com.syathir.roadgpssurvey.util.ChainageFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsFormState(
    val intervalSeconds: String = "1",
    val maximumAccuracyM: String = "10",
    val minimumMovementM: String = "2",
    val startSta: String = "0+000",
    val language: String = "in",
    val error: String? = null,
    val saved: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RoadGpsApplication).settingsRepository
    val savedSettings: StateFlow<GpsSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        GpsSettings(),
    )
    private val mutableForm = MutableStateFlow(SettingsFormState())
    val form: StateFlow<SettingsFormState> = mutableForm.asStateFlow()
    private var initialized = false

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                if (!initialized) {
                    initialized = true
                    mutableForm.value = SettingsFormState(
                        intervalSeconds = (settings.updateIntervalMs / 1_000.0).toString().trimEnd('0').trimEnd('.'),
                        maximumAccuracyM = settings.maximumAccuracyM.toString().trimEnd('0').trimEnd('.'),
                        minimumMovementM = settings.minimumMovementM.toString().trimEnd('0').trimEnd('.'),
                        startSta = ChainageFormatter.format(settings.startChainageM),
                        language = settings.language,
                    )
                }
            }
        }
    }

    fun updateInterval(value: String) = edit { copy(intervalSeconds = value, saved = false) }
    fun updateAccuracy(value: String) = edit { copy(maximumAccuracyM = value, saved = false) }
    fun updateMovement(value: String) = edit { copy(minimumMovementM = value, saved = false) }
    fun updateStartSta(value: String) = edit { copy(startSta = value, saved = false) }
    fun updateLanguage(value: String) = edit { copy(language = value, saved = false) }

    fun save() {
        val form = mutableForm.value
        val strings = com.syathir.roadgpssurvey.ui.i18n.appStringsFor(form.language)
        val intervalSeconds = form.intervalSeconds.replace(',', '.').toDoubleOrNull()
        val accuracyM = form.maximumAccuracyM.replace(',', '.').toDoubleOrNull()
        val movementM = form.minimumMovementM.replace(',', '.').toDoubleOrNull()
        val startM = ChainageFormatter.parse(form.startSta)
        val error = when {
            intervalSeconds == null || !intervalSeconds.isFinite() || intervalSeconds < 0.5 ->
                strings.errorInterval
            accuracyM == null || !accuracyM.isFinite() || accuracyM <= 0.0 ->
                strings.errorAccuracy
            movementM == null || !movementM.isFinite() || movementM < 0.0 ->
                strings.errorMovement
            startM == null -> strings.errorSta
            else -> null
        }
        if (error != null) {
            mutableForm.value = form.copy(error = error, saved = false)
            return
        }
        viewModelScope.launch {
            repository.update(
                savedSettings.value.copy(
                    updateIntervalMs = (intervalSeconds!! * 1_000.0).toLong(),
                    maximumAccuracyM = accuracyM!!,
                    minimumMovementM = movementM!!,
                    startChainageM = startM!!,
                    language = form.language,
                ),
            )
            mutableForm.value = mutableForm.value.copy(error = null, saved = true)
        }
    }

    private fun edit(block: SettingsFormState.() -> SettingsFormState) {
        mutableForm.value = mutableForm.value.block().copy(error = null)
    }
}
