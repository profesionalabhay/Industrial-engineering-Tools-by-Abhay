package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

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

class WhatIfViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val baseElements = repository.workElements.toList()
    private val baseStations = repository.stations.toList()
    private val baseTakt = repository.lines.firstOrNull()?.taktTime ?: 60.0

    private val _scenarios = MutableStateFlow<List<ScenarioState>>(emptyList())
    val scenarios: StateFlow<List<ScenarioState>> = _scenarios.asStateFlow()

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

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

    init {
        _baselineMetrics.value = calculateMetrics(baseElements, baseStations, baseTakt)
        createScenario("Scenario A")
    }

    private fun calculateMetrics(elements: List<WorkElement>, stations: List<Station>, takt: Double): ScenarioMetrics {
        val totalWork = elements.sumOf { it.standardTime }
        
        val stationTimes = elements.groupBy { it.stationId }
            .mapValues { (_, elList) -> elList.sumOf { it.standardTime } }
        
        val activeStationsCount = stations.size // Count of all configured stations in scenario
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

        return ScenarioMetrics(
            taktTime = takt,
            maxCycleTime = maxCt,
            totalWorkContent = totalWork,
            balanceEfficiency = efficiency,
            balanceLoss = 1.0 - efficiency,
            idleTime = idleTime,
            vaPercent = vaPct,
            nnvaPercent = nnvaPct,
            nvaPercent = nvaPct,
            stationCount = activeStationsCount,
            capacityPerHr = capacity
        )
    }

    private fun checkConstraints(elements: List<WorkElement>, stations: List<Station>, takt: Double): List<String> {
        val warnings = mutableListOf<String>()
        val stationOrder = stations.mapIndexed { index, station -> station.id to index }.toMap()

        // 1. Precedence
        elements.forEach { el ->
            val elStationIdx = stationOrder[el.stationId] ?: return@forEach
            el.predecessorIds.forEach { predId ->
                val predElement = elements.find { it.id == predId }
                if (predElement != null) {
                    val predStationIdx = stationOrder[predElement.stationId]
                    if (predStationIdx != null && predStationIdx > elStationIdx) {
                        warnings.add("Precedence Violation: '${el.name}' (Station ${elStationIdx + 1}) is scheduled before predecessor '${predElement.name}' (Station ${predStationIdx + 1}).")
                    }
                }
            }
        }

        // 2. Takt Time
        val stationTimes = elements.groupBy { it.stationId }
            .mapValues { (_, elList) -> elList.sumOf { it.standardTime } }
        
        stationTimes.forEach { (stationId, time) ->
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
            _aiAnalysis.value = null // Reset analysis on change
        }
    }

    fun createScenario(name: String) {
        val id = "SCN-${System.currentTimeMillis()}"
        val newScenario = ScenarioState(id, name, baseElements.map { it.copy() }, baseStations.map { it.copy() })
        _scenarios.update { it + newScenario }
        _activeScenarioId.value = id
        updateActiveState()
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
        _scenarios.update { list ->
            list.map {
                if (it.id == currentId) it.copy(elements = baseElements.map { e -> e.copy() }, stations = baseStations.map { s -> s.copy() })
                else it
            }
        }
        updateActiveState()
    }

    // Element Operations (What-If modifications)
    fun moveElement(elementId: String, newStationId: String) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updatedElements = scn.elements.map { el ->
                        if (el.id == elementId) el.copy(stationId = newStationId) else el
                    }
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
                    val updatedElements = scn.elements.map { el ->
                        if (el.id == elementId) el.copy(standardTime = maxOf(0.0, newTime)) else el
                    }
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
            // Simulate Gemini API processing
            delay(2000)
            
            val base = _baselineMetrics.value
            val active = _activeMetrics.value
            
            if (base != null && active != null) {
                val efficiencyDiff = (active.balanceEfficiency - base.balanceEfficiency) * 100
                val capacityDiff = active.capacityPerHr - base.capacityPerHr
                
                _aiAnalysis.value = AiAnalysisResult(
                    improvements = "Balance efficiency changed by ${String.format("%+.1f%%", efficiencyDiff)}. Capacity shifted by ${String.format("%+.1f", capacityDiff)} units/hr.",
                    tradeOffs = "Reallocating work elements may require cross-training operators for the modified stations. Eliminating tasks removes standard time but necessitates robust standard work documentation.",
                    risks = if (_warnings.value.isNotEmpty()) "High risk: There are ${warnings.value.size} active constraint violations that render this scenario infeasible in reality." else "Low risk: Precedence and Takt constraints are currently satisfied.",
                    assumptions = "Assumes operator skill levels are uniform and standard times are perfectly repeatable.",
                    remainingBottleneck = "The new bottleneck is Station(s) operating at ${String.format("%.1fs", active.maxCycleTime)}, constraining output to ${String.format("%.0f", active.capacityPerHr)} units/hr."
                )
            }
            _isAnalyzing.value = false
        }
    }
}
