package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.MixedModelCalculationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

class YamazumiViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)
    
    private val _selectedMode = MutableStateFlow(YamazumiMode.MIXED_WEIGHTED)
    val selectedMode: StateFlow<YamazumiMode> = _selectedMode.asStateFlow()

    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()

    private val _selectedStationId = MutableStateFlow<String?>(null)

    val metrics: StateFlow<List<StationYamazumiMetrics>> = combine(
        _projectId.filterNotNull(),
        _selectedMode,
        _selectedModelId
    ) { projectId, mode, modelId ->
        calculateMetrics(projectId, mode, modelId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taktTime: StateFlow<Double?> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getProductionPlans(pid).map { it.firstOrNull()?.requiredTaktSeconds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableModels: StateFlow<List<Model>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getModelsForProject(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlan: StateFlow<ProductionPlan?> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getProductionPlans(pid).map { it.firstOrNull() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedStation: StateFlow<StationYamazumiMetrics?> = combine(
        metrics,
        _selectedStationId
    ) { allMetrics, id ->
        allMetrics.find { it.station.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun initialize(id: String) {
        _projectId.value = id
    }

    fun setMode(mode: YamazumiMode) {
        _selectedMode.value = mode
    }

    fun selectModel(modelId: String?) {
        _selectedModelId.value = modelId
        if (modelId != null) {
            _selectedMode.value = YamazumiMode.MODEL_SPECIFIC
        }
    }

    fun selectStation(stationId: String?) {
        _selectedStationId.value = stationId
    }

    private suspend fun calculateMetrics(projectId: String, mode: YamazumiMode, selectedModelId: String?): List<StationYamazumiMetrics> {
        val project = repository.getProjectById(projectId) ?: return emptyList()
        val plan = repository.getProductionPlans(projectId).first().firstOrNull() ?: return emptyList()
        val takt = plan.requiredTaktSeconds
        
        val projectModels = repository.getModelsForProject(projectId).first().filter { it.isActive }
        val mixItems = repository.getModelMixForPlan(plan.id).first()
        val mixMap = mixItems.associate { it.modelId to (it.effectiveMixPercentage / 100.0) }

        val elements = repository.getWorkElementsForProject(projectId).first()
        val elementsByStation = elements.groupBy { it.stationId }
        val processes = repository.getProcessesForLine(project.lineId).first()
        val allStations = repository.getAllStations().first().filter { st -> processes.any { it.id == st.processId } }

        return allStations.map { station ->
            val stElements = elementsByStation[station.id] ?: emptyList()

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

            val maxEntry = breakdowns.maxByOrNull { it.value.totalTime }
            val maxTotal = maxEntry?.value?.totalTime ?: 0.0
            val maxModelName = maxEntry?.value?.modelName ?: ""

            val (displayTotal, displayVa, displayNnva, displayNva) = when (mode) {
                YamazumiMode.MIXED_WEIGHTED -> listOf(weightedTotal, weightedVa, weightedNnva, weightedNva)
                YamazumiMode.MAXIMUM_LOAD -> {
                    val maxBd = maxEntry?.value
                    listOf(maxBd?.totalTime ?: 0.0, maxBd?.vaTime ?: 0.0, maxBd?.nnvaTime ?: 0.0, maxBd?.nvaTime ?: 0.0)
                }
                YamazumiMode.MODEL_SPECIFIC -> {
                    val targetModelId = selectedModelId ?: projectModels.firstOrNull()?.id ?: ""
                    val targetBd = breakdowns[targetModelId]
                    listOf(targetBd?.totalTime ?: 0.0, targetBd?.vaTime ?: 0.0, targetBd?.nnvaTime ?: 0.0, targetBd?.nvaTime ?: 0.0)
                }
            }

            val vaPct = if (displayTotal > 0) (displayVa / displayTotal) * 100.0 else 0.0
            val nnvaPct = if (displayTotal > 0) (displayNnva / displayTotal) * 100.0 else 0.0
            val nvaPct = if (displayTotal > 0) (displayNva / displayTotal) * 100.0 else 0.0
            val idle = max(0.0, takt - displayTotal)
            val util = if (takt > 0) (displayTotal / takt) * 100.0 else 0.0

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
                balanceEfficiency = util,
                modelBreakdowns = breakdowns
            )
        }
    }

    fun updateElementClassification(element: WorkElement, classification: ValueClassification, wasteCategory: WasteCategory, reason: String) {
        viewModelScope.launch {
            repository.insertWorkElement(element.copy(
                valueClassification = classification,
                wasteCategory = wasteCategory,
                classificationReason = reason,
                ieOverridden = true
            ))
        }
    }
}
