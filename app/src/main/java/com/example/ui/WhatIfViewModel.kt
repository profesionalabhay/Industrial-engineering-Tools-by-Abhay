package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ScenarioMetrics(
    val taktTime: Double,
    val maxCycleTime: Double, // Bottleneck
    val totalWorkContent: Double,
    val balanceEfficiency: Double,
    val balanceLoss: Double,
    val idleTime: Double,
    val vaPercent: Double,
    val nnvaPercent: Double,
    val nvaPercent: Double,
    val stationCount: Int,
    val capacityPerHr: Double
)

data class AiAnalysisResult(
    val improvements: String,
    val tradeOffs: String,
    val risks: String,
    val assumptions: String,
    val remainingBottleneck: String
)

data class ScenarioState(
    val id: String,
    val name: String,
    val elements: List<WorkElement>,
    val stations: List<Station>
)

class WhatIfViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

    private val _scenarios = MutableStateFlow<List<ScenarioState>>(emptyList())
    val scenarios: StateFlow<List<ScenarioState>> = _scenarios.asStateFlow()

    private val _baselineMetrics = MutableStateFlow<ScenarioMetrics?>(null)
    val baselineMetrics: StateFlow<ScenarioMetrics?> = _baselineMetrics.asStateFlow()

    private val _activeMetrics = MutableStateFlow<ScenarioMetrics?>(null)
    val activeMetrics: StateFlow<ScenarioMetrics?> = _activeMetrics.asStateFlow()

    private val _warnings = MutableStateFlow<List<String>>(emptyList())
    val warnings: StateFlow<List<String>> = _warnings.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysis: StateFlow<AiAnalysisResult?> = _aiAnalysis.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var baseTakt = 60.0

    fun initialize(projectId: String) {
        _currentProjectId.value = projectId
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            val line = project?.let { repository.getAllLines().first().find { l -> l.id == it.lineId } }
            baseTakt = line?.taktTime ?: 60.0

            val baseElements = repository.getWorkElementsForProject(projectId).first()
            val baseStations = repository.getAllStations().first()
            
            _baselineMetrics.value = calculateMetrics(baseElements, baseStations, baseTakt)
            
            if (_scenarios.value.isEmpty()) {
                createScenario("Scenario A", baseElements, baseStations)
            }
        }
    }

    private fun calculateMetrics(elements: List<WorkElement>, stations: List<Station>, takt: Double): ScenarioMetrics {
        val totalWork = elements.sumOf { it.standardTime }
        val stationTimes = elements.groupBy { it.stationId }.mapValues { it.value.sumOf { e -> e.standardTime } }
        val activeStationsCount = stations.size
        val maxCt = stationTimes.values.maxOrNull() ?: 0.0
        val idleTime = stations.sumOf { maxOf(0.0, maxCt - (stationTimes[it.id] ?: 0.0)) }
        val efficiency = if (activeStationsCount > 0 && maxCt > 0) totalWork / (activeStationsCount * maxCt) else 0.0
        
        val vaTime = elements.filter { it.valueClassification == ValueClassification.VA }.sumOf { it.standardTime }
        val nnvaTime = elements.filter { it.valueClassification == ValueClassification.NNVA }.sumOf { it.standardTime }
        val nvaTime = elements.filter { it.valueClassification == ValueClassification.NVA }.sumOf { it.standardTime }
        
        val vaPct = if (totalWork > 0) (vaTime / totalWork) * 100 else 0.0
        val nnvaPct = if (totalWork > 0) (nnvaTime / totalWork) * 100 else 0.0
        val nvaPct = if (totalWork > 0) (nvaTime / totalWork) * 100 else 0.0
        val capacity = if (maxCt > 0) 3600.0 / maxCt else 0.0

        return ScenarioMetrics(takt, maxCt, totalWork, efficiency, 1.0 - efficiency, idleTime, vaPct, nnvaPct, nvaPct, activeStationsCount, capacity)
    }

    private fun checkConstraints(elements: List<WorkElement>, stations: List<Station>, takt: Double): List<String> {
        val warnings = mutableListOf<String>()
        val stationOrder = stations.mapIndexed { index, station -> station.id to index }.toMap()

        elements.forEach { el ->
            val elStationIdx = stationOrder[el.stationId] ?: return@forEach
            el.predecessorIds.forEach { predId ->
                elements.find { it.id == predId }?.let { predElement ->
                    val predStationIdx = stationOrder[predElement.stationId]
                    if (predStationIdx != null && predStationIdx > elStationIdx) {
                        warnings.add("Precedence Violation: '${el.name}' is scheduled before predecessor '${predElement.name}'.")
                    }
                }
            }
        }

        elements.groupBy { it.stationId }.mapValues { it.value.sumOf { e -> e.standardTime } }.forEach { (stationId, time) ->
            if (time > takt) {
                val sName = stations.find { it.id == stationId }?.name ?: "Unknown Station"
                warnings.add("Takt Violation: $sName exceeds Takt Time (${String.format("%.1f", time)}s > ${String.format("%.1f", takt)}s).")
            }
        }
        return warnings
    }

    private fun updateActiveState() {
        val activeScenario = _scenarios.value.find { it.id == _activeScenarioId.value }
        if (activeScenario != null) {
            _activeMetrics.value = calculateMetrics(activeScenario.elements, activeScenario.stations, baseTakt)
            _warnings.value = checkConstraints(activeScenario.elements, activeScenario.stations, baseTakt)
            _aiAnalysis.value = null
        }
    }

    fun createScenario(name: String, elements: List<WorkElement>? = null, stations: List<Station>? = null) {
        val id = "SCN-${System.currentTimeMillis()}"
        viewModelScope.launch {
            val els = elements ?: repository.getWorkElementsForProject(_currentProjectId.value ?: "").first()
            val sts = stations ?: repository.getAllStations().first()
            val newScenario = ScenarioState(id, name, els.map { it.copy() }, sts.map { it.copy() })
            _scenarios.update { it + newScenario }
            _activeScenarioId.value = id
            updateActiveState()
        }
    }

    fun duplicateScenario(id: String) {
        val toDuplicate = _scenarios.value.find { it.id == id } ?: return
        val newId = "SCN-${System.currentTimeMillis()}"
        val newScenario = toDuplicate.copy(id = newId, name = "${toDuplicate.name} (Copy)")
        _scenarios.update { it + newScenario }
        _activeScenarioId.value = newId
        updateActiveState()
    }

    fun selectScenario(id: String) {
        _activeScenarioId.value = id
        updateActiveState()
    }

    fun resetScenario() {
        val currentId = _activeScenarioId.value ?: return
        viewModelScope.launch {
            val els = repository.getWorkElementsForProject(_currentProjectId.value ?: "").first()
            val sts = repository.getAllStations().first()
            _scenarios.update { list ->
                list.map { if (it.id == currentId) it.copy(elements = els.map { e -> e.copy() }, stations = sts.map { s -> s.copy() }) else it }
            }
            updateActiveState()
        }
    }

    fun moveElement(elementId: String, newStationId: String) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updatedElements = scn.elements.map { el -> if (el.id == elementId) el.copy(stationId = newStationId) else el }
                    scn.copy(elements = updatedElements)
                } else scn
            }
        }
        updateActiveState()
    }

    fun updateElementTime(elementId: String, newTime: Double) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updatedElements = scn.elements.map { el -> if (el.id == elementId) el.copy(standardTime = maxOf(0.0, newTime)) else el }
                    scn.copy(elements = updatedElements)
                } else scn
            }
        }
        updateActiveState()
    }

    fun eliminateElement(elementId: String) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updatedElements = scn.elements.filter { it.id != elementId }
                    scn.copy(elements = updatedElements)
                } else scn
            }
        }
        updateActiveState()
    }

    fun analyzeScenario() {
        _isAnalyzing.value = true
        viewModelScope.launch {
            delay(1500)
            val base = _baselineMetrics.value
            val active = _activeMetrics.value
            if (base != null && active != null) {
                val efficiencyDiff = (active.balanceEfficiency - base.balanceEfficiency) * 100
                val capacityDiff = active.capacityPerHr - base.capacityPerHr
                _aiAnalysis.value = AiAnalysisResult(
                    improvements = "Balance efficiency changed by ${String.format("%.1f%%", efficiencyDiff)}. Capacity shifted by ${String.format("%.1f", capacityDiff)} units/hr.",
                    tradeOffs = "Reallocating work elements may require cross-training operators for the modified stations.",
                    risks = if (_warnings.value.isNotEmpty()) "High risk: There are ${_warnings.value.size} active constraint violations." else "Low risk: Precedence and Takt constraints are satisfied.",
                    assumptions = "Assumes operator skill levels are uniform.",
                    remainingBottleneck = "The new bottleneck is operating at ${String.format("%.1fs", active.maxCycleTime)}."
                )
            }
            _isAnalyzing.value = false
        }
    }
}
