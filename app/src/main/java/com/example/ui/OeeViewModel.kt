package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
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

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getOeeRecords(projectId).collect { records ->
                _uiState.update { it.copy(
                    availableRecords = records,
                    isLoading = false
                ) }
                if (records.isNotEmpty() && _uiState.value.selectedRecord == null) {
                    selectRecord(records.first())
                }
            }
        }
    }

    fun selectRecord(record: OeeRecord) {
        viewModelScope.launch {
            val metrics = repository.calculateOeeMetrics(record)
            
            repository.getLossEvents(record.id).collect { losses ->
                val pareto = calculateLossPareto(losses)
                _uiState.update { it.copy(
                    selectedRecord = record,
                    metrics = metrics,
                    losses = losses,
                    paretoData = pareto
                ) }
            }
        }
    }

    private fun calculateLossPareto(losses: List<LossEvent>): List<Pair<String, Double>> {
        return losses.groupBy { it.reason }
            .mapValues { it.value.sumOf { event -> event.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }
    }
}
