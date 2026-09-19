package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OeeUiState(
    val selectedRecord: OeeRecord? = null,
    val metrics: OeeMetrics? = null,
    val losses: List<LossEvent> = emptyList(),
    val paretoData: List<Pair<String, Double>> = emptyList(),
    val availableRecords: List<OeeRecord> = emptyList(),
    val isLoading: Boolean = false
)

class OeeViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OeeUiState())
    val uiState: StateFlow<OeeUiState> = _uiState.asStateFlow()

    fun loadOeeData(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val records = repository.getOeeRecords(projectId)
            _uiState.value = _uiState.value.copy(
                availableRecords = records,
                isLoading = false
            )
            if (records.isNotEmpty()) {
                selectRecord(records.first())
            }
        }
    }

    fun selectRecord(record: OeeRecord) {
        val metrics = repository.calculateOeeMetrics(record)
        val losses = repository.getLossEvents(record.id)
        val pareto = calculateLossPareto(losses)
        
        _uiState.value = _uiState.value.copy(
            selectedRecord = record,
            metrics = metrics,
            losses = losses,
            paretoData = pareto
        )
    }

    private fun calculateLossPareto(losses: List<LossEvent>): List<Pair<String, Double>> {
        return losses.groupBy { it.reason }
            .mapValues { it.value.sumOf { event -> event.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }
    }
}
