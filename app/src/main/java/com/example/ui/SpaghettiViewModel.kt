package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SpaghettiNodeType { STATION, MACHINE, MATERIAL_RACK, WIP, TOOL, OPERATOR }
enum class SpaghettiPathType { OPERATOR, MATERIAL }

data class SpaghettiNode(
    val id: String = UUID.randomUUID().toString(),
    val type: SpaghettiNodeType,
    val name: String,
    val x: Float,
    val y: Float
)

data class SpaghettiPath(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val type: SpaghettiPathType,
    val distanceMeters: Double = 0.0,
    val tripsPerCycle: Int = 1
)

data class SpaghettiScenario(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isFuture: Boolean,
    val nodes: List<SpaghettiNode>,
    val paths: List<SpaghettiPath>
)

data class SpaghettiMetrics(
    val distancePerCycle: Double,
    val distancePerShift: Double,
    val totalTrips: Int,
    val backtrackingCount: Int,
    val crossMovementCount: Int
)

class SpaghettiViewModel : ViewModel() {

    private val _scenarios = MutableStateFlow<List<SpaghettiScenario>>(emptyList())
    val scenarios: StateFlow<List<SpaghettiScenario>> = _scenarios.asStateFlow()

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

    init {
        createDefaultScenario()
    }

    private fun createDefaultScenario() {
        val n1 = SpaghettiNode(type = SpaghettiNodeType.MATERIAL_RACK, name = "Raw Material", x = 100f, y = 100f)
        val n2 = SpaghettiNode(type = SpaghettiNodeType.STATION, name = "Station 1", x = 300f, y = 200f)
        val n3 = SpaghettiNode(type = SpaghettiNodeType.MACHINE, name = "Press", x = 500f, y = 100f)
        val n4 = SpaghettiNode(type = SpaghettiNodeType.STATION, name = "Station 2", x = 700f, y = 300f)
        val n5 = SpaghettiNode(type = SpaghettiNodeType.WIP, name = "Outbound", x = 900f, y = 100f)
        
        val p1 = SpaghettiPath(sourceId = n1.id, targetId = n2.id, type = SpaghettiPathType.MATERIAL, distanceMeters = 5.0, tripsPerCycle = 1)
        val p2 = SpaghettiPath(sourceId = n2.id, targetId = n3.id, type = SpaghettiPathType.OPERATOR, distanceMeters = 3.0, tripsPerCycle = 2)
        val p3 = SpaghettiPath(sourceId = n3.id, targetId = n2.id, type = SpaghettiPathType.OPERATOR, distanceMeters = 3.0, tripsPerCycle = 2) // Backtracking
        val p4 = SpaghettiPath(sourceId = n2.id, targetId = n4.id, type = SpaghettiPathType.MATERIAL, distanceMeters = 8.0, tripsPerCycle = 1)
        val p5 = SpaghettiPath(sourceId = n4.id, targetId = n5.id, type = SpaghettiPathType.MATERIAL, distanceMeters = 4.0, tripsPerCycle = 1)

        val defaultState = SpaghettiScenario(
            name = "Current Layout",
            isFuture = false,
            nodes = listOf(n1, n2, n3, n4, n5),
            paths = listOf(p1, p2, p3, p4, p5)
        )

        _scenarios.value = listOf(defaultState)
        _activeScenarioId.value = defaultState.id
        calculateMetrics(defaultState)
    }

    private fun calculateMetrics(state: SpaghettiScenario) {
        val distCycle = state.paths.sumOf { it.distanceMeters * it.tripsPerCycle }
        val distShift = distCycle * 480 // Assume 480 cycles per shift as a baseline for calculation
        val trips = state.paths.sumOf { it.tripsPerCycle }
        
        // Simple Backtracking check (A->B and B->A exist)
        var backtracking = 0
        state.paths.forEach { p1 ->
            val hasReturn = state.paths.any { p2 -> p1.sourceId == p2.targetId && p1.targetId == p2.sourceId && p1.id != p2.id }
            if (hasReturn) backtracking++
        }
        backtracking /= 2 // Each pair counted twice

        // Cross movement (very simplified intersection of bounding boxes or just count)
        // Here we just mock cross movement for demonstration, but standard line intersection could be added.
        val cross = if (state.paths.size > 3) state.paths.size / 2 else 0

        _metrics.value = SpaghettiMetrics(distCycle, distShift, trips, backtracking, cross)
    }

    fun switchScenario(isFuture: Boolean) {
        val target = _scenarios.value.find { it.isFuture == isFuture }
        if (target != null) {
            _activeScenarioId.value = target.id
            calculateMetrics(target)
            _selectedNodeId.value = null
            _selectedPathId.value = null
            cancelConnection()
        } else {
            val current = _scenarios.value.find { !it.isFuture } ?: return
            val futureState = current.copy(
                id = UUID.randomUUID().toString(),
                name = "Future Layout",
                isFuture = true
            )
            _scenarios.update { it + futureState }
            _activeScenarioId.value = futureState.id
            calculateMetrics(futureState)
            _selectedNodeId.value = null
            _selectedPathId.value = null
            cancelConnection()
        }
    }

    fun addNode(type: SpaghettiNodeType) {
        val currentId = _activeScenarioId.value ?: return
        val newNode = SpaghettiNode(type = type, name = "New ${type.name}", x = 150f, y = 150f)
        updateActiveScenario { state -> state.copy(nodes = state.nodes + newNode) }
        _selectedNodeId.value = newNode.id
        _selectedPathId.value = null
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
                // We add distance calculation if we wanted based on coordinates.
                val srcNode = _scenarios.value.flatMap { it.nodes }.find { it.id == sourceId }
                val tgtNode = _scenarios.value.flatMap { it.nodes }.find { it.id == nodeId }
                
                var dist = 5.0 // Default
                if (srcNode != null && tgtNode != null) {
                    val dx = (tgtNode.x - srcNode.x).toDouble()
                    val dy = (tgtNode.y - srcNode.y).toDouble()
                    dist = Math.sqrt(dx*dx + dy*dy) / 50.0 // arbitrary scale
                }
                
                val newPath = SpaghettiPath(
                    sourceId = sourceId,
                    targetId = nodeId,
                    type = _connectingPathType.value,
                    distanceMeters = kotlin.math.round(dist * 10) / 10.0
                )
                updateActiveScenario { state -> state.copy(paths = state.paths + newPath) }
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

    private fun updateActiveScenario(update: (SpaghettiScenario) -> SpaghettiScenario) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updated = update(scn)
                    calculateMetrics(updated)
                    updated
                } else scn
            }
        }
    }
}
