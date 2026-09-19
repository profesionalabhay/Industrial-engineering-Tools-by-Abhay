package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RcaUiState(
    val selectedRca: RcaRecord? = null,
    val availableRcas: List<RcaRecord> = emptyList(),
    val isLoading: Boolean = false
)

class RcaViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RcaUiState())
    val uiState: StateFlow<RcaUiState> = _uiState.asStateFlow()

    fun loadRcas(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rcas = repository.getRcaRecords(projectId)
            _uiState.value = _uiState.value.copy(
                availableRcas = rcas,
                isLoading = false
            )
            if (rcas.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(selectedRca = rcas.first())
            }
        }
    }

    fun selectRca(rca: RcaRecord) {
        _uiState.value = _uiState.value.copy(selectedRca = rca)
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
        _uiState.value = _uiState.value.copy(selectedRca = updated)
        saveRca(updated)
    }

    private fun saveRca(rca: RcaRecord) {
        val index = repository.rcaRecords.indexOfFirst { it.id == rca.id }
        if (index >= 0) {
            repository.rcaRecords[index] = rca
        } else {
            repository.rcaRecords.add(rca)
        }
    }
}
