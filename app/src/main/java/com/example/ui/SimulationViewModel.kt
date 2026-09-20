package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.logic.SimulationEngine
import kotlinx.coroutines.flow.*
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

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getSimulationResults(projectId).collect { results ->
                _uiState.update { it.copy(
                    results = results,
                    latestResult = results.lastOrNull(),
                    isLoading = false
                ) }
            }
        }
    }

    fun runSimulation(projectId: String, scenarioName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Collect latest data for simulation
            val stations = repository.getAllStations().first()
            val elements = repository.getWorkElementsForProject(projectId).first()
            
            val result = engine.runDeterministicSimulation(
                projectId = projectId,
                scenarioName = scenarioName,
                stations = stations,
                projectElements = elements,
                durationHours = _uiState.value.simulationHours
            )
            
            repository.insertSimulationResult(result)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateDuration(hours: Double) {
        _uiState.update { it.copy(simulationHours = hours) }
    }
}
