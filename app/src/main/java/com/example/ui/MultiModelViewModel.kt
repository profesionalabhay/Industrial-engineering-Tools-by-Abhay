package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MultiModelViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _activePlanId = MutableStateFlow<String?>(null)
    val activePlanId: StateFlow<String?> = _activePlanId.asStateFlow()

    val productionPlans: StateFlow<List<ProductionPlan>> = _currentProjectId.filterNotNull().flatMapLatest { pid ->
        repository.getProductionPlans(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productionPlan: StateFlow<ProductionPlan?> = combine(productionPlans, _activePlanId) { plans, activeId ->
        plans.find { it.id == activeId } ?: plans.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null as ProductionPlan?)

    val modelMix: StateFlow<List<ModelMixItem>> = productionPlan.filterNotNull().flatMapLatest { plan ->
        repository.getModelMixForPlan(plan.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ModelMixItem>())

    val productionSequence: StateFlow<ProductionSequence?> = productionPlan.filterNotNull().flatMapLatest { plan ->
        repository.getSequenceForPlan(plan.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null as ProductionSequence?)

    val models: StateFlow<List<Model>> = _currentProjectId.filterNotNull().flatMapLatest { pid ->
        repository.getModelsForProject(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Model>())

    private val _lineSummary = MutableStateFlow<MixedModelLineSummary?>(null)
    val lineSummary: StateFlow<MixedModelLineSummary?> = _lineSummary.asStateFlow()

    private val _sequenceAnalysis = MutableStateFlow<SequenceAnalysisResult?>(null)
    val sequenceAnalysis: StateFlow<SequenceAnalysisResult?> = _sequenceAnalysis.asStateFlow()

    private val _redistributionSimulation = MutableStateFlow<RedistributionSimulationResult?>(null)
    val redistributionSimulation: StateFlow<RedistributionSimulationResult?> = _redistributionSimulation.asStateFlow()

    private val _aiDiagnosis = MutableStateFlow<MultiModelAiDiagnosis?>(null)
    val aiDiagnosis: StateFlow<MultiModelAiDiagnosis?> = _aiDiagnosis.asStateFlow()

    private val _selectedModelFilter = MutableStateFlow<String?>("ALL") 
    val selectedModelFilter: StateFlow<String?> = _selectedModelFilter.asStateFlow()

    private val _activeSubTab = MutableStateFlow(0)
    val activeSubTab: StateFlow<Int> = _activeSubTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    val allWorkElements: StateFlow<List<WorkElement>> = _currentProjectId.filterNotNull().flatMapLatest { pid ->
        repository.getWorkElementsForProject(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStations: StateFlow<List<Station>> = repository.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(projectId: String) {
        _currentProjectId.value = projectId
        
        viewModelScope.launch {
            combine(productionPlan, models, modelMix) { plan, models, mix ->
                Triple(plan, models, mix)
            }.collectLatest { (plan, models, mix) ->
                if (plan != null && models.isNotEmpty() && mix.isNotEmpty()) {
                    recalculateAll(plan, models, mix, null) // In a real app we'd fetch the sequence too
                }
            }
        }
    }

    fun selectSubTab(index: Int) {
        _activeSubTab.value = index
    }

    fun selectModelFilter(filter: String) {
        _selectedModelFilter.value = filter
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private suspend fun recalculateAll(
        plan: ProductionPlan,
        models: List<Model>,
        mix: List<ModelMixItem>,
        seq: ProductionSequence?
    ) {
        _isCalculating.value = true
        val stations = repository.getAllStations().first()
        val elements = repository.getWorkElementsForProject(plan.projectId).first()

        val summary = MixedModelCalculationEngine.calculateLineSummary(
            plan = plan,
            stations = stations,
            elements = elements,
            models = models,
            mixItems = mix
        )
        _lineSummary.value = summary

        if (seq != null) {
            val seqResult = MixedModelCalculationEngine.analyzeSequence(
                sequence = seq,
                stationMetrics = summary.stationMetrics,
                lineTakt = plan.requiredTaktSeconds
            )
            _sequenceAnalysis.value = seqResult
        } else {
            _sequenceAnalysis.value = null
        }

        val diagnosis = MixedModelAiAdvisor.generateLineDiagnosis(
            plan = plan,
            summary = summary,
            mixItems = mix,
            sequence = seq,
            elements = elements
        )
        _aiDiagnosis.value = diagnosis
        _isCalculating.value = false
    }

    fun updateModelQuantity(modelId: String, newQuantity: Int) {
        // Implementation for real persistence would involve repository update
    }

    fun overrideMixPercent(modelId: String, overridePct: Double?) {
        // Implementation for real persistence
    }

    fun updateSequencePattern(newPattern: List<String>) {
        // Implementation for real persistence
    }

    fun simulateMoveElement(elementId: String, targetStationId: String) {
        val plan = productionPlan.value ?: return
        val modelsVal = models.value
        val mixVal = modelMix.value

        viewModelScope.launch {
            val stations = repository.getAllStations().first()
            val elements = repository.getWorkElementsForProject(plan.projectId).first()

            val proposal = RedistributionProposal(
                elementId = elementId,
                changeType = RedistributionChangeType.MOVE_STATION,
                sourceStationId = elements.find { it.id == elementId }?.stationId ?: "",
                targetStationId = targetStationId
            )

            val simulation = MixedModelCalculationEngine.simulateRedistribution(
                proposal = proposal,
                baseElements = elements,
                stations = stations,
                models = modelsVal,
                mixItems = mixVal,
                plan = plan
            )
            _redistributionSimulation.value = simulation
        }
    }

    fun clearSimulation() {
        _redistributionSimulation.value = null
    }

    fun commitRedistribution(elementId: String, targetStationId: String) {
        val simulation = _redistributionSimulation.value ?: return
        if (!simulation.isFeasible) {
            _statusMessage.value = "Cannot commit invalid redistribution: Violations exist."
            return
        }

        viewModelScope.launch {
            val el = repository.getWorkElementById(elementId) ?: return@launch
            repository.insertWorkElement(el.copy(stationId = targetStationId))
            _redistributionSimulation.value = null
            _statusMessage.value = "Committed: '${el.name}' moved to station '$targetStationId'."
        }
    }
}
