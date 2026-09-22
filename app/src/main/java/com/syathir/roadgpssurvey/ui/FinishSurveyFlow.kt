package com.syathir.roadgpssurvey.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.export.ExportFormat
import com.syathir.roadgpssurvey.ui.i18n.LocalAppStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinishSurveyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RoadGpsApplication).surveyRepository
    val pending = repository.pendingFinish.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()

    fun name(id: Long, name: String) = perform { repository.nameFinishedSession(id, name) }

    fun complete(id: Long, formats: List<ExportFormat>, onExport: (Long, List<ExportFormat>) -> Unit) = perform {
        repository.dismissFinish(id)
        if (formats.isNotEmpty()) onExport(id, formats)
    }

    private fun perform(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableError.value = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableError.value = "Belum berhasil menyimpan. Silakan coba lagi." }
            finally { mutableBusy.value = false }
        }
    }
}

@Composable
fun FinishSurveyFlow(onExport: (Long, List<ExportFormat>) -> Unit) {
    val model: FinishSurveyViewModel = viewModel()
    val session by model.pending.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    session?.let {
        FinishSurveyDialog(it, busy, error, { name -> model.name(it.id, name) }) { formats ->
            model.complete(it.id, formats, onExport)
        }
    }
}

@Composable
fun FinishSurveyDialog(
    session: SurveySession,
    busy: Boolean,
    error: String?,
    onName: (String) -> Unit,
    onExport: (List<ExportFormat>) -> Unit,
) {
    val strings = LocalAppStrings.current
    val name = rememberSaveable(session.id, saver = TextFieldState.Saver) { TextFieldState() }
    if (session.finishStep == "NAME") {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(strings.finishTitle) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.finishMessage)
                    OutlinedTextField(state = name, label = { Text(strings.finishTitle) },
                        lineLimits = TextFieldLineLimits.SingleLine, inputTransformation = InputTransformation.maxLength(120),
                        enabled = !busy, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { onName(name.text.toString().trim()) }, enabled = !busy && name.text.isNotBlank()) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { onName(session.name) }, enabled = !busy) { Text(strings.autoName) }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!busy) onExport(emptyList()) },
            title = { Text(strings.exportTitle) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.exportMessage(session.name))
                    listOf(strings.exportCsv to listOf(ExportFormat.CSV), strings.exportGpx to listOf(ExportFormat.GPX),
                        strings.exportBoth to ExportFormat.entries.toList()).forEach { (label, formats) ->
                        OutlinedButton(onClick = { onExport(formats) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                    Text(strings.cancelExportHint)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { onExport(emptyList()) }, enabled = !busy) { Text(strings.later) } },
        )
    }
}
