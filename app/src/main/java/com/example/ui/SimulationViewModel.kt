package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.logic.SimulationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimulationUiState(
    val results: List<SimulationResult> = emptyList(),
    val latestResult: SimulationResult? = null,
    val isLoading: Boolean = false,
    val simulationHours: Double = 8.0
)

class SimulationViewModel(
    private val repository: ManufacturingRepository,
    private val engine: SimulationEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    fun loadResults(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val results = repository.getSimulationResults(projectId)
            _uiState.value = _uiState.value.copy(
                results = results,
                latestResult = results.lastOrNull(),
                isLoading = false
            )
        }
    }

    fun runSimulation(projectId: String, scenarioName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = engine.runDeterministicSimulation(projectId, scenarioName, _uiState.value.simulationHours)
            repository.addSimulationResult(result)
            _uiState.value = _uiState.value.copy(
                latestResult = result,
                results = _uiState.value.results + result,
                isLoading = false
            )
        }
    }

    fun updateDuration(hours: Double) {
        _uiState.value = _uiState.value.copy(simulationHours = hours)
    }
}
