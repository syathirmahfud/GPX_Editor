package com.syathir.roadgpssurvey.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.syathir.roadgpssurvey.ui.i18n.AppLanguage

private val Context.settingsDataStore by preferencesDataStore("gps_settings")

data class GpsSettings(
    val updateIntervalMs: Long = 1_000L,
    val maximumAccuracyM: Double = 10.0,
    val minimumMovementM: Double = 2.0,
    val startChainageM: Double = 0.0,
    val overlayEnabled: Boolean = false,
    val language: String = "in",
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val updateIntervalMs = longPreferencesKey("update_interval_ms")
        val maximumAccuracyM = doublePreferencesKey("maximum_accuracy_m")
        val minimumMovementM = doublePreferencesKey("minimum_movement_m")
        val startChainageM = doublePreferencesKey("start_chainage_m")
        val overlayEnabled = booleanPreferencesKey("overlay_enabled")
        val language = stringPreferencesKey("language")
    }

    val settings: Flow<GpsSettings> = context.settingsDataStore.data.map { preferences ->
        GpsSettings(
            updateIntervalMs = preferences[Keys.updateIntervalMs] ?: 1_000L,
            maximumAccuracyM = preferences[Keys.maximumAccuracyM] ?: 10.0,
            minimumMovementM = preferences[Keys.minimumMovementM] ?: 2.0,
            startChainageM = preferences[Keys.startChainageM] ?: 0.0,
            overlayEnabled = preferences[Keys.overlayEnabled] ?: false,
            language = preferences[Keys.language] ?: "in",
        )
    }

    suspend fun update(value: GpsSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.updateIntervalMs] = value.updateIntervalMs
            preferences[Keys.maximumAccuracyM] = value.maximumAccuracyM
            preferences[Keys.minimumMovementM] = value.minimumMovementM
            preferences[Keys.startChainageM] = value.startChainageM
            preferences[Keys.overlayEnabled] = value.overlayEnabled
            preferences[Keys.language] = value.language
        }
        AppLanguage.select(value.language)
    }
}
