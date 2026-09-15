package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.ManufacturingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.ceil

data class CapacityScenario(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isFuture: Boolean,
    val dailyDemand: Int = 400,
    val shiftLengthHours: Double = 8.0,
    val breaksMinutes: Double = 45.0,
    val plannedDowntimeMinutes: Double = 15.0,
    val performanceEfficiency: Double = 0.95,
    val qualityYield: Double = 0.98,
    val actualManpower: Int = 8,
    val bottleneckCtSec: Double = 60.0,
    val validatedManpowerSaving: Int = 0 // IE confirmed saving
)

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

class CapacityViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _scenarios = MutableStateFlow<List<CapacityScenario>>(emptyList())
    val scenarios: StateFlow<List<CapacityScenario>> = _scenarios.asStateFlow()

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

    private val _metrics = MutableStateFlow<CapacityMetrics?>(null)
    val metrics: StateFlow<CapacityMetrics?> = _metrics.asStateFlow()

    private val _manpowerOpportunities = MutableStateFlow<ManpowerOpportunities?>(null)
    val manpowerOpportunities: StateFlow<ManpowerOpportunities?> = _manpowerOpportunities.asStateFlow()

    init {
        createDefaultScenario()
    }

    private fun createDefaultScenario() {
        // Base data from repository
        val elements = repository.workElements
        val totalWork = elements.sumOf { it.standardTime }
        
        val stationTimes = elements.groupBy { it.stationId }
            .mapValues { (_, elList) -> elList.sumOf { it.standardTime } }
        val maxCt = stationTimes.values.maxOrNull() ?: 60.0
        val baseLine = repository.lines.firstOrNull()

        val defaultScenario = CapacityScenario(
            name = "Current State",
            isFuture = false,
            dailyDemand = 400, // typical default
            bottleneckCtSec = maxCt,
            actualManpower = repository.stations.size
        )
        
        _scenarios.value = listOf(defaultScenario)
        _activeScenarioId.value = defaultScenario.id
        calculateMetricsAndOpportunities(defaultScenario, totalWork)
    }

    private fun calculateMetricsAndOpportunities(scenario: CapacityScenario, totalWork: Double) {
        val totalTimeSec = scenario.shiftLengthHours * 3600
        val breaksSec = scenario.breaksMinutes * 60
        val downSec = scenario.plannedDowntimeMinutes * 60
        
        val availableTimeSec = totalTimeSec - breaksSec
        val netOperatingTimeSec = availableTimeSec - downSec
        
        val takt = if (scenario.dailyDemand > 0) availableTimeSec / scenario.dailyDemand else 0.0
        
        // Capacity factors in Efficiency and Quality
        val effectiveCt = if (scenario.performanceEfficiency > 0) scenario.bottleneckCtSec / scenario.performanceEfficiency else scenario.bottleneckCtSec
        val grossCapacity = if (effectiveCt > 0) netOperatingTimeSec / effectiveCt else 0.0
        val dailyCapacity = (grossCapacity * scenario.qualityYield).toInt()
        
        val uph = if (scenario.shiftLengthHours > 0) dailyCapacity / scenario.shiftLengthHours else 0.0
        val capacityGap = dailyCapacity - scenario.dailyDemand // Negative means shortage
        
        val utilization = if (takt > 0) (scenario.bottleneckCtSec / takt) * 100 else 0.0
        
        val reqOvertime = if (capacityGap < 0 && uph > 0) kotlin.math.abs(capacityGap) / uph else 0.0
        
        val requiredManpower = ceil(totalWork / takt).toInt()
        
        val metrics = CapacityMetrics(
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
        
        _metrics.value = metrics
        
        // Manpower logic
        val theoreticalDiff = scenario.actualManpower - requiredManpower
        val status = when {
            theoreticalDiff > 0 -> "Potential Excess Manpower"
            theoreticalDiff < 0 -> "Potential Shortage"
            else -> "Perfectly Balanced"
        }
        
        val suggestions = mutableListOf<String>()
        if (theoreticalDiff > 0) {
            suggestions.add("Floating operator opportunities: Consider assigning floaters for material handling or break relief.")
            suggestions.add("Shared operator opportunities: Combine adjacent sub-assembly tasks.")
            suggestions.add("Multi-machine opportunities: Implement chaku-chaku lines if machines have auto-eject.")
        } else if (theoreticalDiff < 0) {
            suggestions.add("Process is under-resourced for the current takt time.")
            suggestions.add("Line balancing optimization required in the Work Balance engine.")
        }
        
        val opportunities = ManpowerOpportunities(
            status = status,
            theoreticalPotential = kotlin.math.max(0, theoreticalDiff), // Only show excess as potential
            validatedSaving = scenario.validatedManpowerSaving,
            suggestions = suggestions
        )
        _manpowerOpportunities.value = opportunities
    }

    fun updateActiveScenario(update: (CapacityScenario) -> CapacityScenario) {
        val currentId = _activeScenarioId.value ?: return
        val elements = repository.workElements
        val totalWork = elements.sumOf { it.standardTime }

        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updated = update(scn)
                    calculateMetricsAndOpportunities(updated, totalWork)
                    updated
                } else scn
            }
        }
    }

    fun switchScenario(isFuture: Boolean) {
        val target = _scenarios.value.find { it.isFuture == isFuture }
        val elements = repository.workElements
        val totalWork = elements.sumOf { it.standardTime }

        if (target != null) {
            _activeScenarioId.value = target.id
            calculateMetricsAndOpportunities(target, totalWork)
        } else {
            val current = _scenarios.value.find { !it.isFuture } ?: return
            val futureState = current.copy(
                id = UUID.randomUUID().toString(),
                name = "Future Scenario",
                isFuture = true
            )
            _scenarios.update { it + futureState }
            _activeScenarioId.value = futureState.id
            calculateMetricsAndOpportunities(futureState, totalWork)
        }
    }

    fun applyWhatIfBottleneck(newBottleneckCt: Double) {
        updateActiveScenario { it.copy(bottleneckCtSec = newBottleneckCt) }
    }
}
