/*
 * Patient Record — Detail + Upload (Route container + pure UI)
 */

package dev.atick.feature.records.ui.patient

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.atick.core.ui.utils.SnackbarAction
import dev.atick.core.ui.utils.StatefulComposable
import dev.atick.feature.records.viewmodel.RecordDetailUi
import dev.atick.feature.records.viewmodel.RecordDetailViewModel
import dev.atick.core.ui.utils.UiState

@Composable
fun RecordDetailRoute(
    recordId: String,
    onBack: () -> Unit,
    onShowSnackbar: suspend (String, SnackbarAction, Throwable?) -> Boolean,
    vm: RecordDetailViewModel = hiltViewModel()
) {
    val state: UiState<RecordDetailUi> by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(recordId) { vm.load(recordId) }

    StatefulComposable(
        state = state,
        onShowSnackbar = onShowSnackbar
    ) { data: RecordDetailUi ->
        RecordDetailScreen(
            ui = data,
            onBack = onBack,
            onSummaryChange = vm::onSummaryChange,
            onSave = vm::savePatientFields,
            onPickFiles = { uris -> vm.uploadAttachments(recordId, uris) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDetailScreen(
    ui: RecordDetailUi,
    onBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onSave: () -> Unit,
    onPickFiles: (List<Uri>) -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) onPickFiles(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.title.isBlank()) "Record" else ui.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ui.summary,
                onValueChange = onSummaryChange,
                label = { Text("Summary (patient)") },
                modifier = Modifier.fillMaxSize(fraction = 0.0f) // keep width default
            )

            Button(onClick = onSave, enabled = !ui.isSaving) {
                Text(if (ui.isSaving) "Saving..." else "Save")
            }

            Divider()

            Text(text = "Attachments (${ui.attachmentCount})")
            Button(onClick = {
                picker.launch(
                    arrayOf("application/pdf", "image/*", "audio/*", "text/plain")
                )
            }) {
                Text("Upload files")
            }

            // TODO: list attachments (thumbnails / filenames) once repo impl is hooked
        }
    }
}

/* ---------- (Optional) Previews ----------
@Preview(showBackground = true)
@Composable
private fun RecordDetailScreenPreview() {
    RecordDetailScreen(
        ui = RecordDetailUi(
            id = "rec1",
            title = "General Checkup",
            summary = "Fever since last night",
            attachmentCount = 3
        ),
        onBack = {},
        onSummaryChange = {},
        onSave = {},
        onPickFiles = {}
    )
}
------------------------------------------ */
