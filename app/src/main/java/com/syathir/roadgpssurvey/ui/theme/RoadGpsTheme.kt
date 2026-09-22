package com.syathir.roadgpssurvey.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EE6A8),
    onPrimary = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerHighest = Color(0xFF1B1B1B),
    onSurfaceVariant = Color.White,
    outline = Color(0xFFBDBDBD),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C49),
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceContainerHighest = Color(0xFFF0F0F0),
    onSurfaceVariant = Color.Black,
    outline = Color(0xFF606060),
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Color(0xFFBA1A1A),
)

@Composable
fun scheduledNightTheme(): Boolean {
    val night by remember { DayNightSchedule.observeNight() }
        .collectAsStateWithLifecycle(initialValue = DayNightSchedule.isNightNow())
    return night
}

@Composable
fun RoadGpsTheme(darkTheme: Boolean = scheduledNightTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
    }
}
