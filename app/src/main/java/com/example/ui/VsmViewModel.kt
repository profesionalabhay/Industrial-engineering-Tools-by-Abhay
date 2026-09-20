package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class VsmMetrics(
    val totalLeadTimeDays: Double,
    val totalVaTimeSec: Double,
    val pce: Double // Process Cycle Efficiency
)

class VsmViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)

    val scenarios: StateFlow<List<VsmState>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getVsmScenarios(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

    private val _metrics = MutableStateFlow(VsmMetrics(0.0, 0.0, 0.0))
    val metrics: StateFlow<VsmMetrics> = _metrics.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectingSourceId = MutableStateFlow<String?>(null)
    val connectingSourceId: StateFlow<String?> = _connectingSourceId.asStateFlow()
    
    private val _connectingEdgeType = MutableStateFlow(VsmEdgeType.MATERIAL)
    val connectingEdgeType: StateFlow<VsmEdgeType> = _connectingEdgeType.asStateFlow()

    val availableStations: StateFlow<List<Station>> = repository.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            _activeScenarioId.filterNotNull().collectLatest { sid ->
                scenarios.collect { list ->
                    list.find { it.id == sid }?.let { updateMetrics(it) }
                }
            }
        }
    }

    private fun updateMetrics(state: VsmState) {
        val leadTime = state.nodes.sumOf { it.leadTimeDays }
        val vaTime = state.nodes.sumOf { it.vaTimeSec }
        val totalLeadTimeSec = leadTime * 28800
        val pce = if (totalLeadTimeSec > 0) (vaTime / totalLeadTimeSec) * 100 else 0.0
        _metrics.value = VsmMetrics(leadTime, vaTime, pce)
    }

    fun updateActiveScenario(update: (VsmState) -> VsmState) {
        val scenario = scenarios.value.find { it.id == _activeScenarioId.value } ?: return
        viewModelScope.launch {
            repository.insertVsmScenario(update(scenario))
        }
    }

    fun switchScenario(isFuture: Boolean) {
        val target = scenarios.value.find { it.isFutureState == isFuture }
        if (target != null) {
            _activeScenarioId.value = target.id
        } else {
            val current = scenarios.value.find { !it.isFutureState } ?: return
            val futureState = current.copy(
                id = UUID.randomUUID().toString(),
                name = "Future State",
                isFutureState = true
            )
            viewModelScope.launch {
                repository.insertVsmScenario(futureState)
                _activeScenarioId.value = futureState.id
            }
        }
    }

    fun addNode(type: VsmNodeType) {
        val newNode = VsmNode(
            id = UUID.randomUUID().toString(),
            type = type,
            name = "New ${type.name.lowercase().capitalize()}",
            x = 100f, y = 100f
        )
        updateActiveScenario { it.copy(nodes = it.nodes + newNode) }
        _selectedNodeId.value = newNode.id
    }

    fun moveNode(nodeId: String, dx: Float, dy: Float) {
        updateActiveScenario { state ->
            val updatedNodes = state.nodes.map {
                if (it.id == nodeId) it.copy(x = it.x + dx, y = it.y + dy) else it
            }
            state.copy(nodes = updatedNodes)
        }
    }

    fun selectNode(nodeId: String?) {
        if (_isConnecting.value) {
            val sourceId = _connectingSourceId.value
            if (sourceId != null && nodeId != null && sourceId != nodeId) {
                addEdge(sourceId, nodeId, _connectingEdgeType.value)
            }
            cancelConnection()
        } else {
            _selectedNodeId.value = nodeId
        }
    }

    private fun addEdge(sourceId: String, targetId: String, type: VsmEdgeType) {
        val newEdge = VsmEdge(UUID.randomUUID().toString(), sourceId, targetId, type)
        updateActiveScenario { it.copy(edges = it.edges + newEdge) }
    }

    fun startConnection(edgeType: VsmEdgeType) {
        _selectedNodeId.value?.let {
            _isConnecting.value = true
            _connectingSourceId.value = it
            _connectingEdgeType.value = edgeType
        }
    }

    fun cancelConnection() {
        _isConnecting.value = false
        _connectingSourceId.value = null
    }

    fun linkStationToSelectedNode(stationId: String?) {
        val selId = _selectedNodeId.value ?: return
        if (stationId == null) {
            updateSelectedNode { copy(linkedStationId = null) }
            return
        }
        viewModelScope.launch {
            val elements = repository.getWorkElementsForProject(_projectId.value ?: "").first().filter { it.stationId == stationId }
            val ct = elements.sumOf { it.standardTime }
            val va = elements.filter { it.valueClassification == ValueClassification.VA }.sumOf { it.standardTime }
            val station = availableStations.value.find { it.id == stationId }
            updateSelectedNode {
                copy(
                    linkedStationId = stationId,
                    name = station?.name ?: name,
                    cycleTime = ct,
                    vaTimeSec = va
                )
            }
        }
    }

    fun updateSelectedNode(updates: VsmNode.() -> VsmNode) {
        val selId = _selectedNodeId.value ?: return
        updateActiveScenario { state ->
            state.copy(nodes = state.nodes.map { if (it.id == selId) it.updates() else it })
        }
    }
}
