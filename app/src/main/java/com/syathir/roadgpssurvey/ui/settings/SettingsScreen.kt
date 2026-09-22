package com.syathir.roadgpssurvey.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.syathir.roadgpssurvey.data.GpsSettings
import com.syathir.roadgpssurvey.ui.i18n.appStringsFor

@Composable
fun SettingsScreen(
    form: SettingsFormState,
    settings: GpsSettings,
    onIntervalChange: (String) -> Unit,
    onAccuracyChange: (String) -> Unit,
    onMovementChange: (String) -> Unit,
    onStaChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit = {},
    onOverlayChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val strings = appStringsFor(form.language)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.gpsSettings, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text(strings.back) }
        }
        Text(strings.filterHint)
        Text(strings.themeHint)

        Text(strings.language, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isIndonesian = form.language != "en"
            if (isIndonesian) {
                Button(onClick = { onLanguageChange("in") }, modifier = Modifier.weight(1f)) {
                    Text(strings.languageIndonesian)
                }
                OutlinedButton(onClick = { onLanguageChange("en") }, modifier = Modifier.weight(1f)) {
                    Text(strings.languageEnglish)
                }
            } else {
                OutlinedButton(onClick = { onLanguageChange("in") }, modifier = Modifier.weight(1f)) {
                    Text(strings.languageIndonesian)
                }
                Button(onClick = { onLanguageChange("en") }, modifier = Modifier.weight(1f)) {
                    Text(strings.languageEnglish)
                }
            }
        }

        OutlinedTextField(
            value = form.intervalSeconds,
            onValueChange = onIntervalChange,
            label = { Text(strings.intervalLabel) },
            supportingText = { Text(strings.intervalSupport) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.maximumAccuracyM,
            onValueChange = onAccuracyChange,
            label = { Text(strings.accuracyLabel) },
            supportingText = { Text(strings.accuracySupport) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.minimumMovementM,
            onValueChange = onMovementChange,
            label = { Text(strings.movementLabel) },
            supportingText = { Text(strings.movementSupport) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.startSta,
            onValueChange = onStaChange,
            label = { Text(strings.staLabel) },
            supportingText = { Text(strings.staSupport) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(strings.overlayLabel, fontWeight = FontWeight.SemiBold)
                Text(strings.overlaySupport)
            }
            Switch(checked = settings.overlayEnabled, onCheckedChange = onOverlayChange)
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (form.saved) Text(strings.settingsSaved, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(strings.saveSettings) }
        Text(
            text = strings.attribution,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
