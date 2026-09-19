package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StandardWorkUiState(
    val currentRevision: StandardWorkRevision? = null,
    val history: List<StandardWorkRevision> = emptyList(),
    val relatedKaizen: KaizenRecord? = null,
    val isLoading: Boolean = false
)

class StandardWorkViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StandardWorkUiState())
    val uiState: StateFlow<StandardWorkUiState> = _uiState.asStateFlow()

    fun loadStandardWork(stationId: String, modelId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val revisions = repository.getStandardWorkRevisions(stationId, modelId)
            
            val current = revisions.firstOrNull()
            val kaizen = current?.kaizenId?.let { kid -> 
                repository.kaizenRecords.find { it.id == kid } 
            }

            _uiState.value = _uiState.value.copy(
                currentRevision = current,
                history = revisions,
                relatedKaizen = kaizen,
                isLoading = false
            )
        }
    }
}
