package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KaizenUiState(
    val selectedKaizen: KaizenRecord? = null,
    val kaizens: List<KaizenRecord> = emptyList(),
    val benefits: List<ImprovementBenefit> = emptyList(),
    val isLoading: Boolean = false
)

class KaizenViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(KaizenUiState())
    val uiState: StateFlow<KaizenUiState> = _uiState.asStateFlow()

    fun loadKaizens(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val records = repository.getKaizenRecords(projectId)
            _uiState.value = _uiState.value.copy(
                kaizens = records,
                isLoading = false
            )
            if (records.isNotEmpty()) {
                selectKaizen(records.first())
            }
        }
    }

    fun selectKaizen(kaizen: KaizenRecord) {
        val benefits = repository.improvementBenefits.filter { it.kaizenId == kaizen.id }
        _uiState.value = _uiState.value.copy(
            selectedKaizen = kaizen,
            benefits = benefits
        )
    }

    fun updateKaizenStatus(status: KaizenStatus) {
        val current = _uiState.value.selectedKaizen ?: return
        val updated = current.copy(status = status)
        val index = repository.kaizenRecords.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            repository.kaizenRecords[index] = updated
        }
        _uiState.value = _uiState.value.copy(selectedKaizen = updated)
        
        // Refresh list
        val records = repository.getKaizenRecords(updated.modelId ?: "P-001")
        _uiState.value = _uiState.value.copy(kaizens = records)
    }
}
