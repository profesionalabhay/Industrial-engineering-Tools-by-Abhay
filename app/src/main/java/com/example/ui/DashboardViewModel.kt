package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeModelsCount: Int = 0,
    val modelMixSummary: String = "No active models",
    val lineTakt: Double = 0.0,
    val weightedBe: Double = 0.0,
    val weightedCt: Double = 0.0,
    val peakCt: Double = 0.0,
    val peakCtStation: String = "-",
    val oeeOverall: Double = 0.0,
    val maxCycleTime: Double = 0.0,
    val dailyOutput: Int = 0,
    val dailyPlan: Int = 0,
    val stationStatuses: List<StationStatusRow> = emptyList(),
    val isLoading: Boolean = true
)

data class StationStatusRow(
    val stationName: String,
    val operatorName: String,
    val cycleTime: Double,
    val taktDelta: Double,
    val status: String
)

class DashboardViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<DashboardUiState> = _projectId.flatMapLatest { pid ->
        if (pid == null) {
            flowOf(DashboardUiState(isLoading = false))
        } else {
            combine(
                repository.getModelsForProject(pid),
                repository.getWorkElementsForProject(pid),
                repository.getOeeRecords(pid),
                repository.getAllStations(),
                repository.getAllOperators()
            ) { models, elements, oeeRecords, stations, operators ->
                val activeModelsCount = models.size
                val modelSummary = if (models.isNotEmpty()) {
                    models.joinToString(" • ") { "${it.name}" }
                } else "No active models"

                // Simple calculations based on available data
                val avgCt = if (elements.isNotEmpty()) elements.map { it.standardTime }.average() else 0.0
                val maxCt = if (elements.isNotEmpty()) elements.maxOf { it.standardTime } else 0.0
                val peakStation = elements.maxByOrNull { it.standardTime }?.stationId ?: "-"

                val latestOee = oeeRecords.firstOrNull()
                val oeeVal = latestOee?.let { repository.calculateOeeMetrics(it).oee } ?: 0.0

                DashboardUiState(
                    activeModelsCount = activeModelsCount,
                    modelMixSummary = modelSummary,
                    lineTakt = 60.0, // Default for now
                    weightedBe = if (maxCt > 0) (avgCt / maxCt) * 100 else 0.0,
                    weightedCt = avgCt,
                    peakCt = maxCt,
                    peakCtStation = peakStation,
                    oeeOverall = oeeVal * 100,
                    maxCycleTime = maxCt,
                    dailyOutput = latestOee?.goodCount ?: 0,
                    dailyPlan = (latestOee?.totalCount ?: 0),
                    stationStatuses = stations.take(5).map { station ->
                        val stationElements = elements.filter { it.stationId == station.id }
                        val stCt = stationElements.sumOf { it.standardTime }
                        StationStatusRow(
                            stationName = station.name,
                            operatorName = operators.firstOrNull()?.name ?: "N/A",
                            cycleTime = stCt,
                            taktDelta = stCt - 60.0,
                            status = if (stCt <= 60.0) "BALANCED" else "OVER TAKT"
                        )
                    },
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun initialize(projectId: String) {
        _projectId.value = projectId
    }
}
