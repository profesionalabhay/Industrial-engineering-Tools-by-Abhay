package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil

data class CapacityMetrics(
    val availableTimeSec: Double,
    val netOperatingTimeSec: Double,
    val taktTimeSec: Double,
    val uph: Double,
    val dailyCapacity: Int,
    val capacityGap: Int,
    val utilization: Double,
    val requiredOvertimeHours: Double,
    val calculatedManpowerRequirement: Int,
    val totalWorkContent: Double
)

data class ManpowerOpportunities(
    val status: String,
    val theoreticalPotential: Int, // e.g. calculated difference
    val validatedSaving: Int,
    val suggestions: List<String>
)

class CapacityViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)

    val scenarios: StateFlow<List<CapacityScenario>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getCapacityScenarios(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

    private val _metrics = MutableStateFlow<CapacityMetrics?>(null)
    val metrics: StateFlow<CapacityMetrics?> = _metrics.asStateFlow()

    private val _manpowerOpportunities = MutableStateFlow<ManpowerOpportunities?>(null)
    val manpowerOpportunities: StateFlow<ManpowerOpportunities?> = _manpowerOpportunities.asStateFlow()

    fun initialize(projectId: String) {
        _projectId.value = projectId
        viewModelScope.launch {
            scenarios.collectLatest { list ->
                if (_activeScenarioId.value == null && list.isNotEmpty()) {
                    _activeScenarioId.value = list.first().id
                }
            }
        }
        
        viewModelScope.launch {
            combine(_activeScenarioId.filterNotNull(), repository.getAllWorkElements()) { sid, elements ->
                val scenario = scenarios.value.find { it.id == sid } ?: return@combine
                val totalWork = elements.sumOf { it.standardTime }
                calculateMetricsAndOpportunities(scenario, totalWork)
            }.collect()
        }
    }

    private fun calculateMetricsAndOpportunities(scenario: CapacityScenario, totalWork: Double) {
        val totalTimeSec = scenario.shiftLengthHours * 3600
        val breaksSec = scenario.breaksMinutes * 60
        val downSec = scenario.plannedDowntimeMinutes * 60
        
        val availableTimeSec = totalTimeSec - breaksSec
        val netOperatingTimeSec = availableTimeSec - downSec
        
        val takt = if (scenario.dailyDemand > 0) availableTimeSec / scenario.dailyDemand else 0.0
        
        val effectiveCt = if (scenario.performanceEfficiency > 0) scenario.bottleneckCtSec / scenario.performanceEfficiency else scenario.bottleneckCtSec
        val grossCapacity = if (effectiveCt > 0) netOperatingTimeSec / effectiveCt else 0.0
        val dailyCapacity = (grossCapacity * scenario.qualityYield).toInt()
        
        val uph = if (scenario.shiftLengthHours > 0) dailyCapacity / scenario.shiftLengthHours else 0.0
        val capacityGap = dailyCapacity - scenario.dailyDemand
        
        val utilization = if (takt > 0) (scenario.bottleneckCtSec / takt) * 100 else 0.0
        
        val reqOvertime = if (capacityGap < 0 && uph > 0) kotlin.math.abs(capacityGap) / uph else 0.0
        
        val requiredManpower = ceil(totalWork / takt).toInt()
        
        _metrics.value = CapacityMetrics(
            availableTimeSec = availableTimeSec,
            netOperatingTimeSec = netOperatingTimeSec,
            taktTimeSec = takt,
            uph = uph,
            dailyCapacity = dailyCapacity,
            capacityGap = capacityGap,
            utilization = utilization,
            requiredOvertimeHours = reqOvertime,
            calculatedManpowerRequirement = requiredManpower,
            totalWorkContent = totalWork
        )
        
        val theoreticalDiff = scenario.actualManpower - requiredManpower
        val status = when {
            theoreticalDiff > 0 -> "Potential Excess Manpower"
            theoreticalDiff < 0 -> "Potential Shortage"
            else -> "Perfectly Balanced"
        }
        
        val suggestions = mutableListOf<String>()
        if (theoreticalDiff > 0) {
            suggestions.add("Floating operator opportunities: Consider assigning floaters for material handling or break relief.")
        } else if (theoreticalDiff < 0) {
            suggestions.add("Process is under-resourced for the current takt time.")
        }
        
        _manpowerOpportunities.value = ManpowerOpportunities(
            status = status,
            theoreticalPotential = kotlin.math.max(0, theoreticalDiff),
            validatedSaving = scenario.validatedManpowerSaving,
            suggestions = suggestions
        )
    }

    fun updateActiveScenario(update: (CapacityScenario) -> CapacityScenario) {
        val scenario = scenarios.value.find { it.id == _activeScenarioId.value } ?: return
        viewModelScope.launch {
            repository.insertCapacityScenario(update(scenario))
        }
    }

    fun switchScenario(isFuture: Boolean) {
        val target = scenarios.value.find { it.isFuture == isFuture }
        if (target != null) {
            _activeScenarioId.value = target.id
        } else {
            val current = scenarios.value.find { !it.isFuture } ?: return
            val futureState = current.copy(
                id = UUID.randomUUID().toString(),
                name = "Future Scenario",
                isFuture = true
            )
            viewModelScope.launch {
                repository.insertCapacityScenario(futureState)
                _activeScenarioId.value = futureState.id
            }
        }
    }

    fun applyWhatIfBottleneck(newBottleneckCt: Double) {
        updateActiveScenario { it.copy(bottleneckCtSec = newBottleneckCt) }
    }
}
