package com.syathir.roadgpssurvey.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.export.ExportFormat
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.util.ChainageFormatter
import com.syathir.roadgpssurvey.util.TimeFormatter
import java.text.DateFormat
import java.util.Date
import java.util.Locale

import com.syathir.roadgpssurvey.ui.i18n.LocalAppStrings
import com.syathir.roadgpssurvey.ui.i18n.AppStrings

@Composable
fun HistoryScreen(
    sessions: List<SurveySession>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onExport: (Long, ExportFormat) -> Unit,
    onDelete: (Long) -> Unit,
    deletion: HistoryDeletionState = HistoryDeletionState(),
    onSavedMarkers: () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = sessions.firstOrNull { it.id == pendingDeleteId }
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(strings.deleteSessionTitle) },
            text = {
                Text(strings.deleteSessionMessage(pendingDelete.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = null
                    onDelete(pendingDelete.id)
                }, enabled = deletion.deletingId == null) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text(strings.cancel) } },
        )
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.historyTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text(strings.back) }
        }
        OutlinedButton(onClick = onSavedMarkers, modifier = Modifier.fillMaxWidth()) { Text(strings.savedMarkers) }
        deletion.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (sessions.isEmpty()) {
            Text(if (strings.languageCode == "en") "No sections yet. Active and completed survey sections will appear here." else "Belum ada ruas. Ruas yang sedang direkam dan sudah selesai akan ditampilkan di sini.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SurveyHistoryCard(session, onOpen, onExport, { pendingDeleteId = it }, deletion.deletingId, strings)
                }
            }
        }
    }
}

@Composable
private fun SurveyHistoryCard(
    session: SurveySession,
    onOpen: (Long) -> Unit,
    onExport: (Long, ExportFormat) -> Unit,
    onDelete: (Long) -> Unit,
    deletingId: Long?,
    strings: AppStrings,
) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(session.id) }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(session.name, fontWeight = FontWeight.Bold)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, if (strings.languageCode == "en") Locale.US else Locale.forLanguageTag("id-ID")).format(Date(session.startedAt)))
            Text(
                String.format(
                    Locale.US,
                    "%.2f km  •  STA %s  •  %s",
                    session.totalDistanceM / 1_000.0,
                    ChainageFormatter.format(session.startChainageM + session.totalDistanceM),
                    TimeFormatter.formatDuration(session.activeElapsedMs),
                ),
            )
            Text(when (session.status) {
                TrackingStatus.TRACKING -> if (strings.languageCode == "en") "RECORDING" else "MEREKAM"
                TrackingStatus.PAUSED -> if (strings.languageCode == "en") "PAUSED" else "DIJEDA"
                TrackingStatus.FINISHED -> if (strings.languageCode == "en") "FINISHED" else "SELESAI"
                else -> if (strings.languageCode == "en") "READY" else "SIAP"
            }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (session.status == TrackingStatus.FINISHED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onExport(session.id, ExportFormat.CSV) }) { Text("CSV") }
                    Button(onClick = { onExport(session.id, ExportFormat.GPX) }) { Text("GPX") }
                }
                OutlinedButton(onClick = { onDelete(session.id) }, enabled = deletingId == null) {
                    Text(if (deletingId == session.id) strings.deleting else strings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
