package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
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

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllKaizenRecords().collect { records ->
                _uiState.update { it.copy(
                    kaizens = records,
                    isLoading = false
                ) }
                if (records.isNotEmpty() && _uiState.value.selectedKaizen == null) {
                    selectKaizen(records.first())
                }
            }
        }
    }

    fun selectKaizen(kaizen: KaizenRecord) {
        viewModelScope.launch {
            repository.getImprovementBenefits(kaizen.id).collect { benefits ->
                _uiState.update { it.copy(
                    selectedKaizen = kaizen,
                    benefits = benefits
                ) }
            }
        }
    }

    fun updateKaizenStatus(status: KaizenStatus) {
        val current = _uiState.value.selectedKaizen ?: return
        viewModelScope.launch {
            repository.insertKaizenRecord(current.copy(status = status))
        }
    }
}
