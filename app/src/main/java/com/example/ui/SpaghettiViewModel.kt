package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SpaghettiMetrics(
    val distancePerCycle: Double,
    val distancePerShift: Double,
    val totalTrips: Int,
    val backtrackingCount: Int,
    val crossMovementCount: Int
)

class SpaghettiViewModel(private val repository: ManufacturingRepository) : ViewModel() {

    private val _projectId = MutableStateFlow<String?>(null)

    val scenarios: StateFlow<List<SpaghettiScenario>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getSpaghettiDiagrams(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeScenarioId = MutableStateFlow<String?>(null)
    val activeScenarioId: StateFlow<String?> = _activeScenarioId.asStateFlow()

    private val _metrics = MutableStateFlow(SpaghettiMetrics(0.0, 0.0, 0, 0, 0))
    val metrics: StateFlow<SpaghettiMetrics> = _metrics.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _selectedPathId = MutableStateFlow<String?>(null)
    val selectedPathId: StateFlow<String?> = _selectedPathId.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    private val _connectingSourceId = MutableStateFlow<String?>(null)
    val connectingSourceId: StateFlow<String?> = _connectingSourceId.asStateFlow()
    private val _connectingPathType = MutableStateFlow(SpaghettiPathType.OPERATOR)

    private val _aiAnalysis = MutableStateFlow<List<String>>(emptyList())
    val aiAnalysis: StateFlow<List<String>> = _aiAnalysis.asStateFlow()
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

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
                    list.find { it.id == sid }?.let { calculateMetrics(it) }
                }
            }
        }
    }

    private fun calculateMetrics(state: SpaghettiScenario) {
        val distCycle = state.paths.sumOf { it.distanceMeters * it.tripsPerCycle }
        val distShift = distCycle * 480 
        val trips = state.paths.sumOf { it.tripsPerCycle }
        
        var backtracking = 0
        state.paths.forEach { p1 ->
            val hasReturn = state.paths.any { p2 -> p1.sourceId == p2.targetId && p1.targetId == p2.sourceId && p1.id != p2.id }
            if (hasReturn) backtracking++
        }
        backtracking /= 2

        val cross = if (state.paths.size > 3) state.paths.size / 2 else 0

        _metrics.value = SpaghettiMetrics(distCycle, distShift, trips, backtracking, cross)
    }

    private fun updateActiveScenario(update: (SpaghettiScenario) -> SpaghettiScenario) {
        val scenario = scenarios.value.find { it.id == _activeScenarioId.value } ?: return
        viewModelScope.launch {
            repository.insertSpaghettiDiagram(update(scenario))
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
                name = "Future Layout",
                isFuture = true
            )
            viewModelScope.launch {
                repository.insertSpaghettiDiagram(futureState)
                _activeScenarioId.value = futureState.id
            }
        }
    }

    fun addNode(type: SpaghettiNodeType) {
        val newNode = SpaghettiNode(
            id = UUID.randomUUID().toString(),
            type = type,
            name = "New ${type.name}",
            x = 150f,
            y = 150f
        )
        updateActiveScenario { it.copy(nodes = it.nodes + newNode) }
        _selectedNodeId.value = newNode.id
    }

    fun moveNode(nodeId: String, dx: Float, dy: Float) {
        updateActiveScenario { state ->
            val updated = state.nodes.map { if (it.id == nodeId) it.copy(x = it.x + dx, y = it.y + dy) else it }
            state.copy(nodes = updated)
        }
    }

    fun selectNode(nodeId: String?) {
        if (_isConnecting.value) {
            val sourceId = _connectingSourceId.value
            if (sourceId != null && nodeId != null && sourceId != nodeId) {
                val current = scenarios.value.find { it.id == _activeScenarioId.value } ?: return
                val srcNode = current.nodes.find { it.id == sourceId }
                val tgtNode = current.nodes.find { it.id == nodeId }
                
                var dist = 5.0
                if (srcNode != null && tgtNode != null) {
                    val dx = (tgtNode.x - srcNode.x).toDouble()
                    val dy = (tgtNode.y - srcNode.y).toDouble()
                    dist = Math.sqrt(dx*dx + dy*dy) / 50.0
                }
                
                val newPath = SpaghettiPath(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    targetId = nodeId,
                    type = _connectingPathType.value,
                    distanceMeters = kotlin.math.round(dist * 10) / 10.0
                )
                updateActiveScenario { it.copy(paths = it.paths + newPath) }
            }
            cancelConnection()
        } else {
            _selectedNodeId.value = nodeId
            if (nodeId != null) _selectedPathId.value = null
        }
    }
    
    fun selectPath(pathId: String?) {
        _selectedPathId.value = pathId
        if (pathId != null) _selectedNodeId.value = null
    }

    fun deleteSelectedNode() {
        val selId = _selectedNodeId.value ?: return
        updateActiveScenario { state ->
            state.copy(
                nodes = state.nodes.filter { it.id != selId },
                paths = state.paths.filter { it.sourceId != selId && it.targetId != selId }
            )
        }
        _selectedNodeId.value = null
    }
    
    fun deleteSelectedPath() {
        val selId = _selectedPathId.value ?: return
        updateActiveScenario { state ->
            state.copy(paths = state.paths.filter { it.id != selId })
        }
        _selectedPathId.value = null
    }

    fun startConnection(type: SpaghettiPathType) {
        val selId = _selectedNodeId.value
        if (selId != null) {
            _isConnecting.value = true
            _connectingSourceId.value = selId
            _connectingPathType.value = type
        }
    }

    fun cancelConnection() {
        _isConnecting.value = false
        _connectingSourceId.value = null
    }
    
    fun updatePath(id: String, distance: Double, trips: Int) {
        updateActiveScenario { state ->
            val updated = state.paths.map {
                if (it.id == id) it.copy(distanceMeters = distance, tripsPerCycle = trips) else it
            }
            state.copy(paths = updated)
        }
    }
    
    fun updateNodeName(id: String, name: String) {
        updateActiveScenario { state ->
            val updated = state.nodes.map {
                if (it.id == id) it.copy(name = name) else it
            }
            state.copy(nodes = updated)
        }
    }

    fun analyzeLayout() {
        _isAnalyzing.value = true
        viewModelScope.launch {
            delay(2000)
            
            val recommendations = mutableListOf<String>()
            val m = _metrics.value
            
            if (m.backtrackingCount > 0) {
                recommendations.add("Layout Relocation: High backtracking detected (${m.backtrackingCount} paths). Consider moving interacting stations closer and arranging them in a U-shape cell.")
            }
            
            if (m.crossMovementCount > 0) {
                recommendations.add("Material Presentation: Path crossing creates congestion. Shift raw material racks to point-of-use directly at the stations.")
            }
            
            if (m.distancePerCycle > 20.0) {
                recommendations.add("Walking Reduction: Total travel per cycle (${String.format("%.1f", m.distancePerCycle)}m) is high. Investigate consolidating Tool storage to individual workstation shadow boards (Point-of-use storage).")
            }
            
            if (recommendations.isEmpty()) {
                recommendations.add("Layout is optimal based on current connections. No major backtracking or excessive travel detected.")
            }

            _aiAnalysis.value = recommendations
            _isAnalyzing.value = false
        }
    }
    
    fun closeAnalysis() {
        _aiAnalysis.value = emptyList()
    }
}
