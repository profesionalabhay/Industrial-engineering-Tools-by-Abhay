package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OpExDashboardUiState(
    val oeeSummary: List<OeeMetrics> = emptyList(),
    val openRcas: Int = 0,
    val pendingKaizens: Int = 0,
    val totalBenefits: Double = 0.0,
    val recentLosses: List<LossEvent> = emptyList(),
    val isLoading: Boolean = false
)

class OpExDashboardViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OpExDashboardUiState())
    val uiState: StateFlow<OpExDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val oeeRecords = repository.getOeeRecords(projectId)
            val metrics = oeeRecords.map { repository.calculateOeeMetrics(it) }
            
            val openRcaCount = repository.rcaRecords.count { it.status != RcaStatus.CLOSED }
            val pendingKaizenCount = repository.kaizenRecords.count { it.status != KaizenStatus.CLOSED && it.status != KaizenStatus.REJECTED }
            
            val benefitSum = repository.improvementBenefits.filter { it.type == BenefitType.TIME_SAVING }.sumOf { it.value }
            
            val recent = repository.lossEvents.take(5)

            _uiState.value = _uiState.value.copy(
                oeeSummary = metrics,
                openRcas = openRcaCount,
                pendingKaizens = pendingKaizenCount,
                totalBenefits = benefitSum,
                recentLosses = recent,
                isLoading = false
            )
        }
    }
}
