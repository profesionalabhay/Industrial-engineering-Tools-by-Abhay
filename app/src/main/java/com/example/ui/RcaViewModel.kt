package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RcaUiState(
    val selectedRca: RcaRecord? = null,
    val availableRcas: List<RcaRecord> = emptyList(),
    val isLoading: Boolean = false
)

class RcaViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RcaUiState())
    val uiState: StateFlow<RcaUiState> = _uiState.asStateFlow()

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getRcaRecords(projectId).collect { rcas ->
                _uiState.update { it.copy(
                    availableRcas = rcas,
                    isLoading = false
                ) }
                if (rcas.isNotEmpty() && _uiState.value.selectedRca == null) {
                    _uiState.update { it.copy(selectedRca = rcas.first()) }
                }
            }
        }
    }

    fun selectRca(rca: RcaRecord) {
        _uiState.update { it.copy(selectedRca = rca) }
    }

    fun updateFiveWhys(whyIndex: Int, text: String) {
        val current = _uiState.value.selectedRca ?: return
        val newWhys = current.fiveWhys.toMutableList()
        if (whyIndex < newWhys.size) {
            newWhys[whyIndex] = newWhys[whyIndex].copy(whyText = text)
        } else {
            newWhys.add(FiveWhyStep(whyIndex + 1, text))
        }
        val updated = current.copy(fiveWhys = newWhys)
        _uiState.update { it.copy(selectedRca = updated) }
        viewModelScope.launch {
            repository.insertRcaRecord(updated)
        }
    }
}
