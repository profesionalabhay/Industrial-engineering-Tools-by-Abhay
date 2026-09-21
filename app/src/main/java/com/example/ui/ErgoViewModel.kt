package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ErgoUiState(
    val assessments: List<ErgoAssessment> = emptyList(),
    val selectedAssessment: ErgoAssessment? = null,
    val isLoading: Boolean = false
)

class ErgoViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ErgoUiState())
    val uiState: StateFlow<ErgoUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getErgoAssessments(projectId).collect { all ->
                _uiState.value = _uiState.value.copy(
                    assessments = all,
                    selectedAssessment = all.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }
    
    fun selectAssessment(assessment: ErgoAssessment) {
        _uiState.value = _uiState.value.copy(selectedAssessment = assessment)
    }
}
