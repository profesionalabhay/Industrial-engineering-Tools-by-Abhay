package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
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

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getStandardWorkRevisions(projectId).collect { revisions ->
                val current = revisions.maxByOrNull { it.revisionNumber }
                
                _uiState.update { it.copy(
                    currentRevision = current,
                    history = revisions,
                    isLoading = false
                ) }
            }
        }
    }
}
