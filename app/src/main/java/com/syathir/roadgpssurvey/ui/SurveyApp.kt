package com.syathir.roadgpssurvey.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.GpsSettings
import com.syathir.roadgpssurvey.export.ExportFormat
import com.syathir.roadgpssurvey.ui.history.HistoryScreen
import com.syathir.roadgpssurvey.ui.history.HistoryViewModel
import com.syathir.roadgpssurvey.ui.history.SessionDetailScreen
import com.syathir.roadgpssurvey.ui.history.SessionDetailViewModel
import com.syathir.roadgpssurvey.ui.history.SavedMarkersViewModel
import com.syathir.roadgpssurvey.ui.i18n.LocalAppStrings
import com.syathir.roadgpssurvey.ui.i18n.appStringsFor
import com.syathir.roadgpssurvey.ui.settings.SettingsScreen
import com.syathir.roadgpssurvey.ui.settings.SettingsViewModel

@Composable
fun SurveyApp(
    mainViewModel: MainViewModel,
    onStartRequested: () -> Unit,
    onOverlayChange: (Boolean) -> Unit,
    onExport: (Long, ExportFormat) -> Unit,
    onExportMultiple: (Long, List<ExportFormat>) -> Unit,
) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as RoadGpsApplication
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = GpsSettings())
    val strings = appStringsFor(settings.language)

    CompositionLocalProvider(LocalAppStrings provides strings) {
        FinishSurveyFlow(onExport = onExportMultiple)
        NavHost(navController = navController, startDestination = "main", modifier = Modifier.safeDrawingPadding()) {
        composable("main") {
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            MainScreen(
                state = state,
                onStart = onStartRequested,
                onPauseMark = mainViewModel::pauseAndMark,
                onMarkOnly = mainViewModel::markOnly,
                onResume = mainViewModel::resumeTracking,
                onFinish = mainViewModel::finishTracking,
                onHistory = { navController.navigate("history") },
                onSettings = { navController.navigate("settings") },
            )
        }
        composable("history") {
            val historyViewModel: HistoryViewModel = viewModel()
            val sessions by historyViewModel.sessions.collectAsStateWithLifecycle()
            val deletion by historyViewModel.deletion.collectAsStateWithLifecycle()
            HistoryScreen(
                sessions = sessions,
                deletion = deletion,
                onDelete = historyViewModel::deleteSession,
                onBack = navController::popBackStack,
                onOpen = { navController.navigate("session/$it") },
                onExport = onExport,
                onSavedMarkers = { navController.navigate("markers") },
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            val form by settingsViewModel.form.collectAsStateWithLifecycle()
            val currentSettings by settingsViewModel.savedSettings.collectAsStateWithLifecycle()
            SettingsScreen(
                form = form,
                settings = currentSettings,
                onIntervalChange = settingsViewModel::updateInterval,
                onAccuracyChange = settingsViewModel::updateAccuracy,
                onMovementChange = settingsViewModel::updateMovement,
                onStaChange = settingsViewModel::updateStartSta,
                onLanguageChange = settingsViewModel::updateLanguage,
                onOverlayChange = onOverlayChange,
                onSave = settingsViewModel::save,
                onBack = navController::popBackStack,
            )
        }
        composable("markers") {
            val model: SavedMarkersViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            val error by model.error.collectAsStateWithLifecycle()
            val deletingId by model.deletingId.collectAsStateWithLifecycle()
            SessionDetailScreen(state, navController::popBackStack, model::updateMarker, allMarkers = true,
                error = error, onDeleteMarker = model::deleteMarker, deletingMarkerId = deletingId)
        }
        composable(
            route = "session/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            val detailViewModel: SessionDetailViewModel = viewModel()
            val state by detailViewModel.state.collectAsStateWithLifecycle()
            val error by detailViewModel.error.collectAsStateWithLifecycle()
            SessionDetailScreen(
                state = state,
                onBack = navController::popBackStack,
                onUpdateMarker = detailViewModel::updateMarker,
                error = error,
            )
        }
    }
    }
}
