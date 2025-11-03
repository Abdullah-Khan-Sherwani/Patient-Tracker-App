package dev.atick.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.atick.core.ui.utils.UiState
import dev.atick.data.records.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientRecordsUi(
    val isLoading: Boolean = false,     // kept for rendering convenience
    val items: List<RecordItemUi> = emptyList()
)

data class RecordItemUi(
    val id: String,
    val title: String,
    val status: String,
    val attachmentCount: Int
)

@HiltViewModel
class PatientRecordsViewModel @Inject constructor(
    private val repo: RecordRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<PatientRecordsUi>>(UiState.Loading)
    val ui: StateFlow<UiState<PatientRecordsUi>> = _ui

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        _ui.value = UiState.Loading
        repo.listForCurrentPatient()
            .catch { e -> _ui.value = UiState.Error(e) }
            .collectLatest { list ->
                val mapped = list.map {
                    RecordItemUi(
                        id = it.id,
                        title = it.title ?: "Untitled",
                        status = it.status.name,
                        attachmentCount = it.attachmentCount
                    )
                }
                _ui.value = UiState.Success(PatientRecordsUi(isLoading = false, items = mapped))
            }
    }

    fun createDraftRecord() = viewModelScope.launch {
        try {
            // (Optional) optimistic spinner
            val current = (_ui.value as? UiState.Success)?.data
            _ui.value = UiState.Success((current ?: PatientRecordsUi()).copy(isLoading = true))

            repo.createRecord(title = "New Record", summary = null)

            // After creation, flow collector above will emit the updated list
            val refreshed = (_ui.value as? UiState.Success)?.data
            _ui.value = UiState.Success((refreshed ?: PatientRecordsUi()).copy(isLoading = false))
        } catch (t: Throwable) {
            _ui.value = UiState.Error(t)
        }
    }
}
