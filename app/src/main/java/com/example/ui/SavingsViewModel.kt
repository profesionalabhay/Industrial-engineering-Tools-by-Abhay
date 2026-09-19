package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SavingsUiState(
    val records: List<SavingsRecord> = emptyList(),
    val benefits: List<ImprovementBenefit> = emptyList(),
    val validations: List<SavingsValidation> = emptyList(),
    val isLoading: Boolean = false
)

class SavingsViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    fun loadSavingsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = _uiState.value.copy(
                records = repository.savingsRecords,
                benefits = repository.improvementBenefits,
                validations = repository.savingsValidations,
                isLoading = false
            )
        }
    }

    fun validateBenefit(benefitId: String, validatorName: String, notes: String) {
        val validation = SavingsValidation(
            id = "VAL-${UUID.randomUUID()}",
            benefitId = benefitId,
            status = ValidationStatus.USER_VALIDATED,
            validatedBy = validatorName,
            validatedAt = System.currentTimeMillis(),
            notes = notes
        )
        repository.savingsValidations.add(validation)
        _uiState.value = _uiState.value.copy(
            validations = _uiState.value.validations + validation
        )
    }
}
