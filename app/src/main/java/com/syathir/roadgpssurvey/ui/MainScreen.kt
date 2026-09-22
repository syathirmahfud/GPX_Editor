package com.syathir.roadgpssurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.ui.i18n.AppStrings
import com.syathir.roadgpssurvey.ui.i18n.LocalAppStrings
import com.syathir.roadgpssurvey.ui.i18n.message
import com.syathir.roadgpssurvey.util.ChainageFormatter
import com.syathir.roadgpssurvey.util.TimeFormatter
import java.util.Locale

@Composable
fun MainScreen(
    state: MainUiState,
    onStart: () -> Unit,
    onPauseMark: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onMarkOnly: () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            val compact = maxHeight < 500.dp && maxWidth >= 560.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (compact) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FitText(strings.surveyPoints(state.markerCount), Modifier.weight(1f), 22, FontWeight.Bold)
                        NavigationButtons(onHistory, onSettings, Modifier.weight(1f), strings)
                    }
                } else {
                    FitText(strings.surveyPoints(state.markerCount), Modifier.fillMaxWidth(), 22, FontWeight.Bold)
                    NavigationButtons(onHistory, onSettings, Modifier.fillMaxWidth(), strings)
                }
                // Size metrics from the viewport, never from changing values.
                // In small windows only the metrics scroll, keeping actions visible.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val minHeight = if (compact) 184.dp else 352.dp
                    val contentHeight = maxOf(maxHeight, minHeight)
                    val needsScroll = maxHeight < minHeight
                    val scrollModifier = if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier
                    Column(Modifier.fillMaxSize().then(scrollModifier)) {
                        DashboardMetrics(state, compact, Modifier.fillMaxWidth().height(contentHeight), strings)
                    }
                }
                val active = state.status == TrackingStatus.TRACKING || state.status == TrackingStatus.PAUSED
                val primaryLabel = when (state.status) {
                    TrackingStatus.TRACKING -> strings.pauseAndMark
                    TrackingStatus.PAUSED -> strings.resume
                    else -> strings.start
                }
                val primaryAction = when (state.status) {
                    TrackingStatus.TRACKING -> onPauseMark
                    TrackingStatus.PAUSED -> onResume
                    else -> onStart
                }
                if (compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.status == TrackingStatus.TRACKING) ActionButton(strings.mark, onMarkOnly, Modifier.weight(1f))
                        ActionButton(primaryLabel, primaryAction, Modifier.weight(1f))
                        if (active) ActionButton(strings.finish, onFinish, Modifier.weight(1f))
                    }
                } else {
                    // Keep card geometry identical across recording states.
                    Column(Modifier.fillMaxWidth().height(140.dp), verticalArrangement = Arrangement.Bottom) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.status == TrackingStatus.TRACKING) ActionButton(strings.mark, onMarkOnly, Modifier.weight(1f))
                            ActionButton(primaryLabel, primaryAction, Modifier.weight(1f))
                        }
                        if (active) ActionButton(strings.finish, onFinish, Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationButtons(onHistory: () -> Unit, onSettings: () -> Unit, modifier: Modifier, strings: AppStrings) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f).height(48.dp)) { FitText(strings.history, Modifier.fillMaxWidth(), 14) }
        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f).height(48.dp)) { FitText(strings.settings, Modifier.fillMaxWidth(), 14) }
    }
}

@Composable
private fun DashboardMetrics(state: MainUiState, compact: Boolean, modifier: Modifier, strings: AppStrings) {
    val distance = String.format(Locale.US, "%.2f km", state.distanceM / 1_000.0)
    if (compact) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("DISTANCE", distance, Modifier.weight(1f).fillMaxHeight(), emphasized = true, strings = strings, error = state.locationError)
            SecondaryMetrics(state, Modifier.weight(2f).fillMaxHeight(), strings)
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("DISTANCE", distance, Modifier.weight(1.2f).fillMaxWidth(), emphasized = true, strings = strings, error = state.locationError)
            SecondaryMetrics(state, Modifier.weight(2f).fillMaxWidth(), strings)
        }
    }
}

@Composable
private fun SecondaryMetrics(state: MainUiState, modifier: Modifier, strings: AppStrings) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("STA", ChainageFormatter.format(state.startChainageM + state.distanceM), Modifier.weight(1f).fillMaxHeight(), strings = strings)
            MetricCard("TIME", TimeFormatter.formatDuration(state.activeElapsedMs), Modifier.weight(1f).fillMaxHeight(), strings = strings)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                "GPS ACCURACY",
                state.accuracyM?.let { String.format(Locale.US, "±%.1f m", it) } ?: strings.waiting,
                Modifier.weight(1f).fillMaxHeight(),
                valueColor = accuracyColor(state.accuracyM),
                subtitle = accuracyCondition(state.accuracyM, strings),
                strings = strings,
            )
            MetricCard("SPEED", String.format(Locale.US, "%.1f %s", state.speedKmh, strings.speedUnit), Modifier.weight(1f).fillMaxHeight(), strings = strings)
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(64.dp)) {
        FitText(label, Modifier.fillMaxWidth(), 20, FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    valueColor: Color = Color.Unspecified,
    subtitle: String? = null,
    strings: AppStrings = LocalAppStrings.current,
    error: String? = null,
) {
    Card(modifier.testTag("metric-$label")) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FitText(when (label) {
                "DISTANCE" -> strings.distance
                "TIME" -> strings.time
                "GPS ACCURACY" -> strings.gpsAccuracy
                "SPEED" -> strings.speed
                else -> label
            }, Modifier.fillMaxWidth(), 14)
            FitText(value, Modifier.fillMaxWidth(), if (emphasized) 48 else 28, FontWeight.Black, valueColor)
            // Reserve one status line in all secondary cards.
            if (!emphasized) FitText(subtitle ?: " ", Modifier.fillMaxWidth(), 14, FontWeight.Bold, valueColor)
            error?.let { Text(strings.message(it), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, maxLines = 2) }
        }
    }
}

@Composable
private fun FitText(
    value: String,
    modifier: Modifier,
    maxFontSize: Int,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
) {
    BasicText(
        text = value,
        modifier = modifier,
        style = TextStyle(
            color = if (color == Color.Unspecified) LocalContentColor.current else color,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = maxFontSize.sp, stepSize = 1.sp),
    )
}

@Composable
private fun accuracyColor(accuracyM: Float?): Color = when {
    accuracyM == null -> MaterialTheme.colorScheme.onSurface
    accuracyM <= 5f -> MaterialTheme.colorScheme.primary
    accuracyM <= 10f -> if (MaterialTheme.colorScheme.surface == Color.Black) Color(0xFFFFD54F) else Color(0xFF795000)
    else -> MaterialTheme.colorScheme.error
}

private fun accuracyCondition(accuracyM: Float?, strings: AppStrings): String? = when {
    accuracyM == null -> null
    accuracyM <= 5f -> strings.accuracyGood
    accuracyM <= 10f -> strings.accuracyFair
    else -> strings.accuracyPoor
}
