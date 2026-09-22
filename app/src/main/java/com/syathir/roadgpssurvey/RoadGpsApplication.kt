package com.syathir.roadgpssurvey

import android.app.Application
import com.syathir.roadgpssurvey.data.AppDatabase
import com.syathir.roadgpssurvey.data.SettingsRepository
import com.syathir.roadgpssurvey.data.SurveyRepository
import com.syathir.roadgpssurvey.ui.i18n.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoadGpsApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { settingsRepository.settings.collect { AppLanguage.select(it.language) } }
    }
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val surveyRepository: SurveyRepository by lazy { SurveyRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
