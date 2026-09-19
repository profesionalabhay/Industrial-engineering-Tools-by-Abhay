package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MultiModelViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _currentProjectId = MutableStateFlow("P-001")
    val currentProjectId: StateFlow<String> = _currentProjectId.asStateFlow()

    private val _productionPlan = MutableStateFlow<ProductionPlan?>(null)
    val productionPlan: StateFlow<ProductionPlan?> = _productionPlan.asStateFlow()

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models.asStateFlow()

    private val _modelMix = MutableStateFlow<List<ModelMixItem>>(emptyList())
    val modelMix: StateFlow<List<ModelMixItem>> = _modelMix.asStateFlow()

    private val _productionSequence = MutableStateFlow<ProductionSequence?>(null)
    val productionSequence: StateFlow<ProductionSequence?> = _productionSequence.asStateFlow()

    private val _lineSummary = MutableStateFlow<MixedModelLineSummary?>(null)
    val lineSummary: StateFlow<MixedModelLineSummary?> = _lineSummary.asStateFlow()

    private val _sequenceAnalysis = MutableStateFlow<SequenceAnalysisResult?>(null)
    val sequenceAnalysis: StateFlow<SequenceAnalysisResult?> = _sequenceAnalysis.asStateFlow()

    private val _redistributionSimulation = MutableStateFlow<RedistributionSimulationResult?>(null)
    val redistributionSimulation: StateFlow<RedistributionSimulationResult?> = _redistributionSimulation.asStateFlow()

    private val _aiDiagnosis = MutableStateFlow<MultiModelAiDiagnosis?>(null)
    val aiDiagnosis: StateFlow<MultiModelAiDiagnosis?> = _aiDiagnosis.asStateFlow()

    private val _selectedModelFilter = MutableStateFlow<String?>("ALL") // "ALL" or modelId
    val selectedModelFilter: StateFlow<String?> = _selectedModelFilter.asStateFlow()

    private val _activeSubTab = MutableStateFlow(0)
    val activeSubTab: StateFlow<Int> = _activeSubTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadProjectData("P-001")
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

    fun loadProjectData(projectId: String) {
        _currentProjectId.value = projectId
        val plan = repository.getProductionPlanForProject(projectId)
        _productionPlan.value = plan

        val projectModels = repository.models.filter { it.projectId == projectId }
        _models.value = projectModels

        val mix = repository.getModelMixForPlan(plan.id)
        _modelMix.value = mix

        val sequences = repository.getProductionSequencesForPlan(plan.id)
        val seq = sequences.firstOrNull()
        _productionSequence.value = seq

        recalculateAll(plan, projectModels, mix, seq)
    }

    private fun recalculateAll(
        plan: ProductionPlan,
        models: List<Model>,
        mix: List<ModelMixItem>,
        seq: ProductionSequence?
    ) {
        val stations = repository.stations
        val elements = repository.workElements.filter { it.projectId == plan.projectId }

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

        // Generate deterministic IE AI diagnosis
        val diagnosis = MixedModelAiAdvisor.generateLineDiagnosis(
            plan = plan,
            summary = summary,
            mixItems = mix,
            sequence = seq,
            elements = elements
        )
        _aiDiagnosis.value = diagnosis
    }

    fun updateModelQuantity(modelId: String, newQuantity: Int) {
        val plan = _productionPlan.value ?: return
        repository.updateModelDemand(plan.id, modelId, newQuantity)
        loadProjectData(plan.projectId)
        _statusMessage.value = "Updated demand for $modelId to $newQuantity pcs. Model mix recomputed."
    }

    fun overrideMixPercent(modelId: String, overridePct: Double?) {
        val currentMix = _modelMix.value.find { it.modelId == modelId } ?: return
        val updatedItem = currentMix.copy(manualMixOverride = overridePct)
        repository.saveModelMixItem(updatedItem)
        val plan = _productionPlan.value ?: return
        val updatedMix = repository.getModelMixForPlan(plan.id)
        _modelMix.value = updatedMix
        recalculateAll(plan, _models.value, updatedMix, _productionSequence.value)
        _statusMessage.value = if (overridePct != null) "Manual Mix % override applied ($overridePct%)." else "Reset to calculated Mix %."
    }

    fun updateSequencePattern(newPattern: List<String>) {
        val seq = _productionSequence.value ?: return
        val updatedSeq = seq.copy(modelPattern = newPattern)
        repository.saveProductionSequence(updatedSeq)
        _productionSequence.value = updatedSeq
        val plan = _productionPlan.value ?: return
        recalculateAll(plan, _models.value, _modelMix.value, updatedSeq)
        _statusMessage.value = "Production sequence pattern updated."
    }

    fun simulateMoveElement(elementId: String, targetStationId: String) {
        val plan = _productionPlan.value ?: return
        val stations = repository.stations
        val elements = repository.workElements.filter { it.projectId == plan.projectId }
        val models = _models.value
        val mix = _modelMix.value

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
            models = models,
            mixItems = mix,
            plan = plan
        )
        _redistributionSimulation.value = simulation
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

        val elIdx = repository.workElements.indexOfFirst { it.id == elementId }
        if (elIdx >= 0) {
            val el = repository.workElements[elIdx]
            repository.workElements[elIdx] = el.copy(stationId = targetStationId)
            _redistributionSimulation.value = null
            val plan = _productionPlan.value ?: return
            loadProjectData(plan.projectId)
            _statusMessage.value = "Committed: '${el.name}' moved to station '$targetStationId'."
        }
    }
}
