package dev.atick.feature.records.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.atick.core.ui.utils.UiState
import dev.atick.data.records.AttachmentRepository
import dev.atick.data.records.RecordRepository
import dev.atick.data.records.model.RecordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordDetailUi(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val status: RecordStatus = RecordStatus.DRAFT,
    val attachmentCount: Int = 0,
    val isSaving: Boolean = false
)

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val records: RecordRepository,
    private val atts: AttachmentRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<RecordDetailUi>>(UiState.Loading)
    val ui: StateFlow<UiState<RecordDetailUi>> = _ui

    fun load(recordId: String) = viewModelScope.launch {
        _ui.value = UiState.Loading
        try {
            val r = records.get(recordId) ?: error("Record not found")
            _ui.value = UiState.Success(
                RecordDetailUi(
                    id = r.id,
                    title = r.title.orEmpty(),
                    summary = r.summary.orEmpty(),
                    status = r.status,
                    attachmentCount = r.attachmentCount
                )
            )
        } catch (t: Throwable) {
            _ui.value = UiState.Error(t)
        }
    }

    fun onSummaryChange(v: String) {
        val cur = (_ui.value as? UiState.Success)?.data ?: return
        _ui.value = UiState.Success(cur.copy(summary = v))
    }

    fun savePatientFields() = viewModelScope.launch {
        val cur = (_ui.value as? UiState.Success)?.data ?: return@launch
        _ui.value = UiState.Success(cur.copy(isSaving = true))
        try {
            records.updatePatientFields(cur.id, mapOf("summary" to cur.summary))
            // Re-fetch or just clear saving flag
            _ui.value = UiState.Success(cur.copy(isSaving = false))
        } catch (t: Throwable) {
            _ui.value = UiState.Error(t)
        }
    }

    fun uploadAttachments(recordId: String, uris: List<Uri>) = viewModelScope.launch {
        val cur = (_ui.value as? UiState.Success)?.data ?: return@launch
        _ui.value = UiState.Success(cur.copy(isSaving = true))
        try {
            uris.forEach { uri -> atts.uploadForCurrentPatient(recordId, uri) }
            // Option 1: refresh counts by reloading
            load(recordId)
        } catch (t: Throwable) {
            _ui.value = UiState.Error(t)
        }
    }
}
