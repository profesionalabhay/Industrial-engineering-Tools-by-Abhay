package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import com.example.engine.MixedModelCalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

enum class YamazumiMode {
    MIXED_WEIGHTED,
    MAXIMUM_LOAD,
    MODEL_SPECIFIC
}

data class ModelYamazumiBreakdown(
    val modelId: String,
    val modelName: String,
    val variant: String,
    val totalTime: Double,
    val vaTime: Double,
    val nnvaTime: Double,
    val nvaTime: Double,
    val applicableElements: List<WorkElement>
)

data class StationYamazumiMetrics(
    val station: Station,
    val elements: List<WorkElement>,
    val totalTime: Double,
    val vaTime: Double,
    val nnvaTime: Double,
    val nvaTime: Double,
    val vaPercent: Double,
    val nvaPercent: Double,
    val utilization: Double,
    val idleTime: Double,
    // V2.1 Mixed-Model Extensions (Backward Compatible)
    val weightedCt: Double = totalTime,
    val maxCt: Double = totalTime,
    val maxModelName: String = "",
    val nnvaPercent: Double = 0.0,
    val balanceEfficiency: Double = 0.0,
    val modelBreakdowns: Map<String, ModelYamazumiBreakdown> = emptyMap()
)

class YamazumiViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _metrics = MutableStateFlow<List<StationYamazumiMetrics>>(emptyList())
    val metrics: StateFlow<List<StationYamazumiMetrics>> = _metrics.asStateFlow()

    private val _taktTime = MutableStateFlow<Double?>(null)
    val taktTime: StateFlow<Double?> = _taktTime.asStateFlow()

    private val _selectedStation = MutableStateFlow<StationYamazumiMetrics?>(null)
    val selectedStation: StateFlow<StationYamazumiMetrics?> = _selectedStation.asStateFlow()

    private val _selectedMode = MutableStateFlow(YamazumiMode.MIXED_WEIGHTED)
    val selectedMode: StateFlow<YamazumiMode> = _selectedMode.asStateFlow()

    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()

    private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
    val availableModels: StateFlow<List<Model>> = _availableModels.asStateFlow()

    private val _activePlan = MutableStateFlow<ProductionPlan?>(null)
    val activePlan: StateFlow<ProductionPlan?> = _activePlan.asStateFlow()

    init {
        loadData()
    }

    fun setMode(mode: YamazumiMode) {
        _selectedMode.value = mode
        loadData()
    }

    fun selectModel(modelId: String?) {
        _selectedModelId.value = modelId
        if (modelId != null) {
            _selectedMode.value = YamazumiMode.MODEL_SPECIFIC
        }
        loadData()
    }

    fun loadData() {
        val plan = repository.getProductionPlanForProject("P-001")
        _activePlan.value = plan
        val takt = plan.requiredTaktSeconds
        _taktTime.value = takt

        val projectModels = repository.models.filter { it.projectId == plan.projectId && it.isActive }
        _availableModels.value = projectModels

        val mixItems = repository.getModelMixForPlan(plan.id)
        val mixMap = mixItems.associate { it.modelId to (it.effectiveMixPercentage / 100.0) }

        val elementsByStation = repository.workElements.filter { it.projectId == plan.projectId }.groupBy { it.stationId }
        val allStations = repository.stations

        val newMetrics = allStations.map { station ->
            val stElements = elementsByStation[station.id] ?: emptyList()

            // Compute per-model breakdown
            val breakdowns = projectModels.associate { model ->
                val applicableElements = mutableListOf<WorkElement>()
                var mVa = 0.0
                var mNnva = 0.0
                var mNva = 0.0

                stElements.forEach { el ->
                    val dur = MixedModelCalculationEngine.getElementDurationForModel(el, model.id)
                    if (dur != null) {
                        applicableElements.add(el)
                        when (el.valueClassification) {
                            ValueClassification.VA -> mVa += dur
                            ValueClassification.NNVA -> mNnva += dur
                            ValueClassification.NVA -> mNva += dur
                        }
                    }
                }
                val mTotal = mVa + mNnva + mNva
                model.id to ModelYamazumiBreakdown(
                    modelId = model.id,
                    modelName = model.name,
                    variant = model.variant,
                    totalTime = mTotal,
                    vaTime = mVa,
                    nnvaTime = mNnva,
                    nvaTime = mNva,
                    applicableElements = applicableElements
                )
            }

            // Calculate weighted sums
            var weightedVa = 0.0
            var weightedNnva = 0.0
            var weightedNva = 0.0
            breakdowns.forEach { (mId, bd) ->
                val ratio = mixMap[mId] ?: 0.0
                weightedVa += bd.vaTime * ratio
                weightedNnva += bd.nnvaTime * ratio
                weightedNva += bd.nvaTime * ratio
            }
            val weightedTotal = weightedVa + weightedNnva + weightedNva

            // Maximum load
            val maxEntry = breakdowns.maxByOrNull { it.value.totalTime }
            val maxTotal = maxEntry?.value?.totalTime ?: 0.0
            val maxModelName = maxEntry?.value?.modelName ?: ""

            // Mode-specific display values
            val (displayTotal, displayVa, displayNnva, displayNva) = when (_selectedMode.value) {
                YamazumiMode.MIXED_WEIGHTED -> {
                    listOf(weightedTotal, weightedVa, weightedNnva, weightedNva)
                }
                YamazumiMode.MAXIMUM_LOAD -> {
                    val maxBd = maxEntry?.value
                    listOf(
                        maxBd?.totalTime ?: 0.0,
                        maxBd?.vaTime ?: 0.0,
                        maxBd?.nnvaTime ?: 0.0,
                        maxBd?.nvaTime ?: 0.0
                    )
                }
                YamazumiMode.MODEL_SPECIFIC -> {
                    val targetModelId = _selectedModelId.value ?: projectModels.firstOrNull()?.id ?: ""
                    val targetBd = breakdowns[targetModelId]
                    listOf(
                        targetBd?.totalTime ?: 0.0,
                        targetBd?.vaTime ?: 0.0,
                        targetBd?.nnvaTime ?: 0.0,
                        targetBd?.nvaTime ?: 0.0
                    )
                }
            }

            val vaPct = if (displayTotal > 0) (displayVa / displayTotal) * 100.0 else 0.0
            val nnvaPct = if (displayTotal > 0) (displayNnva / displayTotal) * 100.0 else 0.0
            val nvaPct = if (displayTotal > 0) (displayNva / displayTotal) * 100.0 else 0.0
            val idle = max(0.0, takt - displayTotal)
            val util = if (takt > 0) (displayTotal / takt) * 100.0 else 0.0
            val be = if (takt > 0) (displayTotal / takt) * 100.0 else 0.0

            StationYamazumiMetrics(
                station = station,
                elements = stElements.sortedBy { it.sequence },
                totalTime = displayTotal,
                vaTime = displayVa,
                nnvaTime = displayNnva,
                nvaTime = displayNva,
                vaPercent = vaPct,
                nvaPercent = nvaPct,
                utilization = util,
                idleTime = idle,
                weightedCt = weightedTotal,
                maxCt = maxTotal,
                maxModelName = maxModelName,
                nnvaPercent = nnvaPct,
                balanceEfficiency = be,
                modelBreakdowns = breakdowns
            )
        }
        _metrics.value = newMetrics

        _selectedStation.value?.let { current ->
            _selectedStation.value = newMetrics.find { it.station.id == current.station.id }
        }
    }

    fun selectStation(stationId: String?) {
        if (stationId == null) {
            _selectedStation.value = null
        } else {
            _selectedStation.value = _metrics.value.find { it.station.id == stationId }
        }
    }

    fun updateElementClassification(elementId: String, classification: ValueClassification, wasteCategory: WasteCategory, reason: String) {
        val index = repository.workElements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = repository.workElements[index]
            repository.workElements[index] = el.copy(
                valueClassification = classification,
                wasteCategory = wasteCategory,
                classificationReason = reason,
                ieOverridden = true
            )
            loadData()
        }
    }
}
