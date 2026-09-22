package com.syathir.roadgpssurvey.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syathir.roadgpssurvey.data.MarkerType
import com.syathir.roadgpssurvey.data.SurveyMarker
import com.syathir.roadgpssurvey.util.ChainageFormatter
import java.util.Locale

import com.syathir.roadgpssurvey.ui.i18n.LocalAppStrings
import com.syathir.roadgpssurvey.ui.i18n.AppStrings

@Composable
fun SessionDetailScreen(
    state: SessionDetailState,
    onBack: () -> Unit,
    onUpdateMarker: (SurveyMarker) -> Unit,
    modifier: Modifier = Modifier,
    allMarkers: Boolean = false,
    error: String? = null,
    onDeleteMarker: ((Long) -> Unit)? = null,
    deletingMarkerId: Long? = null,
) {
    val strings = LocalAppStrings.current
    var editing by remember { mutableStateOf<SurveyMarker?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = state.markers.firstOrNull { it.id == pendingDeleteId && it.sessionId == null }
    if (pendingDelete != null && onDeleteMarker != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(strings.deleteMarkerTitle) },
            text = { Text(strings.deleteMarkerMessage(pendingDelete.pointName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = null
                    if (editing?.id == pendingDelete.id) editing = null
                    onDeleteMarker(pendingDelete.id)
                }, enabled = deletingMarkerId == null) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text(strings.cancel) } },
        )
    }
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (allMarkers) strings.allMarkersTitle else state.session?.name ?: strings.sessionDetail,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text(strings.back) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val session = state.session
        if (session != null) {
            Text(String.format(Locale.US, if (strings.languageCode == "en") "Distance %.2f km" else "Jarak %.2f km", session.totalDistanceM / 1_000.0))
            Text(if (strings.languageCode == "en") "Marked points: ${state.markers.size}" else "Titik penanda: ${state.markers.size}")
        }
        when {
            state.loading -> Text(if (strings.languageCode == "en") "Loading data…" else "Memuat data…")
            !allMarkers && session == null -> Text(if (strings.languageCode == "en") "Section not available. Marked points can still be viewed in Saved Points." else "Ruas tidak tersedia. Titik penanda tetap dapat dilihat di Titik Tersimpan.")
            state.markers.isEmpty() -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(strings.noMarkers, fontWeight = FontWeight.Bold)
                    Text(if (strings.languageCode == "en") "Use MARK during recording to save points without pausing." else "Gunakan TANDAI saat merekam untuk menyimpan titik tanpa menjeda perekaman.")
                }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.markers, key = { it.id }) { marker ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(marker.pointName, fontWeight = FontWeight.Bold)
                    if (allMarkers) {
                        Text(strings.sourceRuas(marker.sourceRuasName.ifBlank { if (strings.languageCode == "en") "Not available" else "Tidak tersedia" }))
                        if (marker.sessionId == null) Text(strings.detachedMarkerInfo)
                    }
                    Text("${strings.markerTypeName(marker.markerType)}  •  STA ${ChainageFormatter.format(marker.chainageM)}")
                    Text(String.format(Locale.US, "WGS84 · %.7f, %.7f  ±%.1f m", marker.latitude, marker.longitude, marker.accuracyM))
                    if (marker.note.isNotBlank()) Text(marker.note)
                    TextButton(onClick = { editing = marker }) { Text(strings.edit) }
                    if (marker.sessionId == null && onDeleteMarker != null) {
                        OutlinedButton(onClick = { pendingDeleteId = marker.id }, enabled = deletingMarkerId == null) {
                            Text(if (deletingMarkerId == marker.id) strings.deleting else strings.deleteMarker, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        }
    }
    editing?.let { marker ->
        MarkerEditDialog(
            marker = marker,
            strings = strings,
            onDismiss = { editing = null },
            onSave = {
                onUpdateMarker(it)
                editing = null
            },
        )
    }
}

@Composable
private fun MarkerEditDialog(
    marker: SurveyMarker,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (SurveyMarker) -> Unit,
) {
    val name = rememberSaveable(marker.id, saver = TextFieldState.Saver) { TextFieldState(marker.pointName) }
    val note = rememberSaveable(marker.id, saver = TextFieldState.Saver) { TextFieldState(marker.note) }
    var type by remember(marker.id) { mutableStateOf(marker.markerType) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editMarkerTitle) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(state = name, label = { Text(strings.pointName) }, lineLimits = TextFieldLineLimits.SingleLine)
                OutlinedTextField(state = note, label = { Text(strings.noteLabel) }, lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2))
                Text(strings.markerTypeLabel, fontWeight = FontWeight.SemiBold)
                MarkerType.entries.forEach { candidate ->
                    FilterChip(
                        selected = type == candidate,
                        onClick = { type = candidate },
                        label = { Text(strings.markerTypeName(candidate)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(marker.copy(pointName = name.text.toString().trim().ifBlank { marker.pointName }, note = note.text.toString().trim(), markerType = type)) },
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}
