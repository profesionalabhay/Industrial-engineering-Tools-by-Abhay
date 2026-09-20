package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
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

    private var dashboardJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                repository.getOeeRecords(projectId),
                repository.getRcaRecords(projectId),
                repository.getAllKaizenRecords()
            ) { oeeRecords, rcaRecords, kaizenRecords ->
                val metrics = oeeRecords.map { repository.calculateOeeMetrics(it) }
                val openRcaCount = rcaRecords.count { it.status != RcaStatus.CLOSED }
                val pendingKaizenCount = kaizenRecords.count { it.status != KaizenStatus.CLOSED && it.status != KaizenStatus.REJECTED }
                
                // For losses, we might need more specific flows or just collect first
                val recent = if (oeeRecords.isNotEmpty()) {
                     repository.getLossEvents(oeeRecords.first().id).first().take(5)
                } else emptyList()

                _uiState.update { it.copy(
                    oeeSummary = metrics,
                    openRcas = openRcaCount,
                    pendingKaizens = pendingKaizenCount,
                    recentLosses = recent,
                    isLoading = false
                ) }
            }.collect()
        }
    }
}
