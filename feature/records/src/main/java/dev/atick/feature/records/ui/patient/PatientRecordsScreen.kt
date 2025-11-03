/*
 * Patient Records — List + FAB (Route container + pure UI)
 */

package dev.atick.feature.records.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.atick.core.ui.utils.SnackbarAction
import dev.atick.core.ui.utils.StatefulComposable
import dev.atick.feature.records.viewmodel.PatientRecordsUi
import dev.atick.feature.records.viewmodel.PatientRecordsViewModel
import dev.atick.feature.records.viewmodel.RecordItemUi
import dev.atick.core.ui.utils.UiState

@Composable
fun PatientRecordsRoute(
    onOpenRecord: (String) -> Unit,
    onShowSnackbar: suspend (String, SnackbarAction, Throwable?) -> Boolean,
    vm: PatientRecordsViewModel = hiltViewModel()
) {
    val state: UiState<PatientRecordsUi> by vm.ui.collectAsStateWithLifecycle()
    StatefulComposable(
        state = state,
        onShowSnackbar = onShowSnackbar
    ) { data: PatientRecordsUi ->
        PatientRecordsScreen(
            data = data,
            onOpenRecord = onOpenRecord,
            onCreateRecord = vm::createDraftRecord
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientRecordsScreen(
    data: PatientRecordsUi,
    onOpenRecord: (String) -> Unit,
    onCreateRecord: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My Records") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRecord) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (data.isLoading) {
            LinearProgressIndicator(modifier = Modifier.padding(padding))
        }
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(data.items) { r: RecordItemUi ->
                ListItem(
                    headlineContent = { Text(r.title) },
                    supportingContent = { Text("${r.status} • ${r.attachmentCount} attachments") },
                    modifier = Modifier.clickable { onOpenRecord(r.id) }
                )
                Divider()
            }
        }
    }
}

/* ---------- (Optional) Previews ----------
@Preview(showBackground = true)
@Composable
private fun PatientRecordsScreenPreview() {
    PatientRecordsScreen(
        data = PatientRecordsUi(
            isLoading = false,
            items = listOf(
                RecordItemUi("1", "Blood Test", "DRAFT", 2),
                RecordItemUi("2", "MRI Report", "SUBMITTED", 1),
                RecordItemUi("3", "General Checkup", "REVIEWED", 4)
            )
        ),
        onOpenRecord = {},
        onCreateRecord = {}
    )
}
------------------------------------------ */
