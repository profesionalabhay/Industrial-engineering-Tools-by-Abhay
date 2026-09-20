package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

data class BalanceMetrics(
    val taktTime: Double,
    val totalWorkContent: Double,
    val theoreticalMinStations: Int,
    val numStations: Int,
    val maxCycleTime: Double,
    val idleTime: Double,
    val balanceEfficiency: Double,
    val balanceLoss: Double,
    val smoothnessIndex: Double
)

data class StationDraft(
    val station: Station,
    val elements: List<WorkElement>,
    val cycleTime: Double
)

data class ProposedBalance(
    val id: String,
    val methodName: String,
    val metrics: BalanceMetrics,
    val elementAssignments: Map<String, String>, // ElementId -> StationId
    val violations: Int = 0
)

class WorkBalanceViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)

    // Base data
    val stations: StateFlow<List<Station>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getProjectById(pid).flatMapLatest { project ->
            if (project != null) repository.getProcessesForLine(project.lineId).flatMapLatest { processes ->
                repository.getAllStations().map { all -> all.filter { st -> processes.any { it.id == st.processId } } }
            } else flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseElements: StateFlow<List<WorkElement>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getWorkElementsForProject(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Draft State: Tracks element ID to currently assigned Station ID
    private val _draftAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val draftAssignments: StateFlow<Map<String, String>> = _draftAssignments.asStateFlow()

    val baselineMetrics: StateFlow<BalanceMetrics?> = baseElements.map { elements ->
        if (elements.isEmpty()) return@map null
        calculateMetrics(elements.associate { it.id to it.stationId }, elements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val draftMetrics: StateFlow<BalanceMetrics?> = combine(
        _draftAssignments,
        baseElements
    ) { assignments, elements ->
        if (assignments.isEmpty() || elements.isEmpty()) return@combine null
        calculateMetrics(assignments, elements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val warnings: StateFlow<List<String>> = combine(
        _draftAssignments,
        baseElements,
        stations
    ) { assignments, elements, stations ->
        if (assignments.isEmpty() || elements.isEmpty() || stations.isEmpty()) return@combine emptyList()
        checkConstraints(assignments, elements, stations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _proposedBalances = MutableStateFlow<List<ProposedBalance>>(emptyList())
    val proposedBalances: StateFlow<List<ProposedBalance>> = _proposedBalances.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        viewModelScope.launch {
            baseElements.collectLatest { elements ->
                if (_draftAssignments.value.isEmpty() && elements.isNotEmpty()) {
                    _draftAssignments.value = elements.associate { it.id to it.stationId }
                }
            }
        }
    }

    fun initialize(id: String) {
        _projectId.value = id
    }

    fun moveElement(elementId: String, newStationId: String) {
        _draftAssignments.update { current ->
            current.toMutableMap().apply { put(elementId, newStationId) }
        }
    }

    private fun calculateMetrics(assignments: Map<String, String>, elements: List<WorkElement>): BalanceMetrics? {
        val takt = 60.0 // Default or from project context
        val totalWork = elements.sumOf { it.standardTime }
        val minStations = ceil(totalWork / takt).toInt()
        
        val stationTimes = assignments.entries.groupBy({ it.value }, { entry ->
            elements.find { it.id == entry.key }?.standardTime ?: 0.0
        }).mapValues { it.value.sum() }

        val activeStations = stationTimes.keys.size
        val maxCt = stationTimes.values.maxOrNull() ?: 0.0
        
        val idleTime = stationTimes.values.sumOf { maxCt - it }
        val efficiency = if (activeStations > 0 && maxCt > 0) totalWork / (activeStations * maxCt) else 0.0
        val loss = 1.0 - efficiency
        
        val smoothness = sqrt(stationTimes.values.sumOf { (maxCt - it).pow(2) })

        return BalanceMetrics(
            taktTime = takt,
            totalWorkContent = totalWork,
            theoreticalMinStations = minStations,
            numStations = activeStations,
            maxCycleTime = maxCt,
            idleTime = idleTime,
            balanceEfficiency = efficiency,
            balanceLoss = loss,
            smoothnessIndex = smoothness
        )
    }

    private fun checkConstraints(assignments: Map<String, String>, elements: List<WorkElement>, allStations: List<Station>): List<String> {
        val warnings = mutableListOf<String>()
        val stationOrder = allStations.mapIndexed { index, station -> station.id to index }.toMap()

        elements.forEach { el ->
            val elStationIdx = stationOrder[assignments[el.id]] ?: return@forEach
            
            el.predecessorIds.forEach { predId ->
                val predStationIdx = stationOrder[assignments[predId]]
                if (predStationIdx != null && predStationIdx > elStationIdx) {
                    warnings.add("Precedence Violation: '${el.name}' is scheduled before its predecessor.")
                }
            }
        }

        val takt = 60.0 // Mock
        val stationTimes = assignments.entries.groupBy({ it.value }, { entry ->
            elements.find { it.id == entry.key }?.standardTime ?: 0.0
        }).mapValues { it.value.sum() }

        stationTimes.forEach { (stationId, time) ->
            if (time > takt) {
                val sName = allStations.find { it.id == stationId }?.name ?: "Unknown"
                warnings.add("Takt Violation: $sName exceeds takt time (${"%.1f".format(time)} > $takt).")
            }
        }

        return warnings
    }

    fun getDraftStations(): List<StationDraft> {
        val assignments = _draftAssignments.value
        val elements = baseElements.value
        val allStations = stations.value
        
        return allStations.map { station ->
            val stationElements = elements
                .filter { assignments[it.id] == station.id }
                .sortedBy { it.sequence }
            
            StationDraft(
                station = station,
                elements = stationElements,
                cycleTime = stationElements.sumOf { it.standardTime }
            )
        }
    }

    fun generateSuggestions() {
        _isGenerating.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _isGenerating.value = false
            // Simplified for now
        }
    }

    fun applyProposal(proposal: ProposedBalance) {
        _draftAssignments.value = proposal.elementAssignments
    }

    fun saveDraftAsScenario(scenarioName: String) {
        // Implementation for real data saving
    }
}
