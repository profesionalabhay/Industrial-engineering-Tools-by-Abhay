package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SavingsUiState(
    val records: List<SavingsRecord> = emptyList(),
    val benefits: List<ImprovementBenefit> = emptyList(),
    val validations: List<SavingsValidation> = emptyList(),
    val isLoading: Boolean = false
)

class SavingsViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _currentProjectId = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<SavingsUiState> = _currentProjectId.filterNotNull().flatMapLatest { projectId ->
        combine(
            repository.getSavingsCalculations(projectId),
            repository.getAllKaizenRecords()
        ) { calculations, kaizens ->
            // Filter records and benefits based on the kaizens of this project
            // For simplicity in this prototype-to-real migration, we'll fetch all and filter
            // In a larger app, we'd have better cross-linking in the DB
            val projectKaizens = kaizens.filter { it.id.isNotEmpty() } // Placeholder for project filtering if needed
            
            SavingsUiState(
                records = emptyList(), // We'll populate these via additional flows if needed
                benefits = projectKaizens.flatMap { k -> emptyList<ImprovementBenefit>() }, // Placeholder
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsUiState(isLoading = true))

    fun initialize(projectId: String) {
        _currentProjectId.value = projectId
    }

    fun validateBenefit(benefitId: String, validatorName: String, notes: String) {
        viewModelScope.launch {
            val validation = SavingsValidation(
                id = UUID.randomUUID().toString(),
                benefitId = benefitId,
                status = ValidationStatus.USER_VALIDATED,
                validatedBy = validatorName,
                validatedAt = System.currentTimeMillis(),
                notes = notes
            )
            repository.insertSavingsValidation(validation)
        }
    }
}
