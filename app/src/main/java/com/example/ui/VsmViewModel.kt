package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.ManufacturingRepository
import com.example.data.Station
import com.example.data.ValueClassification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class VsmNodeType { SUPPLIER, CUSTOMER, PROCESS, INVENTORY, SUPERMARKET, KANBAN, PROD_CONTROL }
enum class VsmEdgeType { MATERIAL, INFORMATION, FIFO, SHIPMENT }

data class VsmNode(
    val id: String,
    val type: VsmNodeType,
    val name: String,
    val x: Float,
    val y: Float,
    val cycleTime: Double = 0.0,
    val uptime: Double = 100.0,
    val operators: Int = 1,
    val wip: Double = 0.0,
    val leadTimeDays: Double = 0.0, // Top of timeline
    val vaTimeSec: Double = 0.0,    // Bottom of timeline
    val nnvaTimeSec: Double = 0.0,
    val nvaTimeSec: Double = 0.0,
    val linkedStationId: String? = null
)

data class VsmEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val type: VsmEdgeType
)

data class VsmState(
    val id: String,
    val name: String,
    val isFutureState: Boolean,
    val nodes: List<VsmNode>,
    val edges: List<VsmEdge>
)

data class VsmMetrics(
    val totalLeadTimeDays: Double,
    val totalVaTimeSec: Double,
    val pce: Double // Process Cycle Efficiency
)

class VsmViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    val availableStations: List<Station> = repository.stations.toList()

    private val _scenarios = MutableStateFlow<List<VsmState>>(emptyList())
    val scenarios: StateFlow<List<VsmState>> = _scenarios.asStateFlow()

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

    init {
        createDefaultScenario()
    }

    private fun createDefaultScenario() {
        val n1 = VsmNode("S1", VsmNodeType.SUPPLIER, "Steel Supplier", 50f, 100f)
        val n2 = VsmNode("I1", VsmNodeType.INVENTORY, "Coil Inventory", 250f, 150f, wip = 500.0, leadTimeDays = 5.0)
        
        val p1 = VsmNode("P1", VsmNodeType.PROCESS, "Stamping", 400f, 200f, cycleTime = 45.0, vaTimeSec = 40.0, operators = 1)
        val i2 = VsmNode("I2", VsmNodeType.INVENTORY, "WIP", 600f, 150f, wip = 200.0, leadTimeDays = 2.0)
        
        val p2 = VsmNode("P2", VsmNodeType.PROCESS, "Assembly", 750f, 200f, cycleTime = 60.0, vaTimeSec = 50.0, operators = 2)
        
        val n3 = VsmNode("C1", VsmNodeType.CUSTOMER, "OEM Assembly", 950f, 100f)
        val ctrl = VsmNode("CTRL", VsmNodeType.PROD_CONTROL, "Production Control", 500f, 20f)

        val e1 = VsmEdge("E1", n1.id, n2.id, VsmEdgeType.SHIPMENT)
        val e2 = VsmEdge("E2", n2.id, p1.id, VsmEdgeType.MATERIAL)
        val e3 = VsmEdge("E3", p1.id, i2.id, VsmEdgeType.MATERIAL)
        val e4 = VsmEdge("E4", i2.id, p2.id, VsmEdgeType.FIFO)
        val e5 = VsmEdge("E5", p2.id, n3.id, VsmEdgeType.SHIPMENT)
        val e6 = VsmEdge("E6", ctrl.id, p1.id, VsmEdgeType.INFORMATION)

        val defaultState = VsmState(
            id = "VSM-CURRENT",
            name = "Current State",
            isFutureState = false,
            nodes = listOf(n1, n2, p1, i2, p2, n3, ctrl),
            edges = listOf(e1, e2, e3, e4, e5, e6)
        )

        _scenarios.value = listOf(defaultState)
        _activeScenarioId.value = defaultState.id
        updateMetrics(defaultState)
    }

    private fun updateMetrics(state: VsmState) {
        val leadTime = state.nodes.sumOf { it.leadTimeDays }
        val vaTime = state.nodes.sumOf { it.vaTimeSec }
        
        // PCE Calculation: 1 day = 24 * 60 * 60 = 86400 seconds (using a standard 24h cycle for simplicity, or 8h shift: 28800s)
        // We'll use 8-hour shift equivalent: 28800 seconds per day.
        val totalLeadTimeSec = leadTime * 28800
        val pce = if (totalLeadTimeSec > 0) (vaTime / totalLeadTimeSec) * 100 else 0.0

        _metrics.value = VsmMetrics(leadTime, vaTime, pce)
    }

    fun switchScenario(isFuture: Boolean) {
        val target = _scenarios.value.find { it.isFutureState == isFuture }
        if (target != null) {
            _activeScenarioId.value = target.id
            updateMetrics(target)
            _selectedNodeId.value = null
            cancelConnection()
        } else {
            // Create a Future State copy
            val current = _scenarios.value.find { !it.isFutureState } ?: return
            val futureState = current.copy(
                id = "VSM-FUTURE",
                name = "Future State",
                isFutureState = true
            )
            _scenarios.update { it + futureState }
            _activeScenarioId.value = futureState.id
            updateMetrics(futureState)
            _selectedNodeId.value = null
            cancelConnection()
        }
    }

    fun addNode(type: VsmNodeType) {
        val currentId = _activeScenarioId.value ?: return
        val newNode = VsmNode(
            id = UUID.randomUUID().toString(),
            type = type,
            name = "New ${type.name.lowercase().replaceFirstChar { it.uppercase() }}",
            x = 100f,
            y = 100f
        )
        
        updateActiveScenario { state ->
            state.copy(nodes = state.nodes + newNode)
        }
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
                // Complete connection
                addEdge(sourceId, nodeId, _connectingEdgeType.value)
            }
            cancelConnection()
        } else {
            _selectedNodeId.value = nodeId
        }
    }

    fun updateSelectedNode(updates: VsmNode.() -> VsmNode) {
        val selId = _selectedNodeId.value ?: return
        updateActiveScenario { state ->
            val updatedNodes = state.nodes.map {
                if (it.id == selId) it.updates() else it
            }
            state.copy(nodes = updatedNodes)
        }
    }

    fun linkStationToSelectedNode(stationId: String?) {
        val selId = _selectedNodeId.value ?: return
        if (stationId == null) {
            updateSelectedNode { copy(linkedStationId = null) }
            return
        }

        val stationElements = repository.workElements.filter { it.stationId == stationId }
        val ct = stationElements.sumOf { it.standardTime }
        val va = stationElements.filter { it.valueClassification == ValueClassification.VA }.sumOf { it.standardTime }
        val nnva = stationElements.filter { it.valueClassification == ValueClassification.NNVA }.sumOf { it.standardTime }
        val nva = stationElements.filter { it.valueClassification == ValueClassification.NVA }.sumOf { it.standardTime }
        val stationName = availableStations.find { it.id == stationId }?.name ?: "Station $stationId"

        updateSelectedNode {
            copy(
                linkedStationId = stationId,
                name = stationName,
                cycleTime = ct,
                vaTimeSec = va,
                nnvaTimeSec = nnva,
                nvaTimeSec = nva
            )
        }
    }

    fun deleteSelectedNode() {
        val selId = _selectedNodeId.value ?: return
        updateActiveScenario { state ->
            val updatedNodes = state.nodes.filter { it.id != selId }
            val updatedEdges = state.edges.filter { it.sourceId != selId && it.targetId != selId }
            state.copy(nodes = updatedNodes, edges = updatedEdges)
        }
        _selectedNodeId.value = null
    }

    fun duplicateSelectedNode() {
        val selId = _selectedNodeId.value ?: return
        val currentScenario = _scenarios.value.find { it.id == _activeScenarioId.value } ?: return
        val nodeToCopy = currentScenario.nodes.find { it.id == selId } ?: return
        
        val newNode = nodeToCopy.copy(
            id = UUID.randomUUID().toString(),
            name = "${nodeToCopy.name} (Copy)",
            x = nodeToCopy.x + 30f,
            y = nodeToCopy.y + 30f
        )
        updateActiveScenario { state -> state.copy(nodes = state.nodes + newNode) }
        _selectedNodeId.value = newNode.id
    }

    fun startConnection(edgeType: VsmEdgeType) {
        val selId = _selectedNodeId.value
        if (selId != null) {
            _isConnecting.value = true
            _connectingSourceId.value = selId
            _connectingEdgeType.value = edgeType
        }
    }

    fun cancelConnection() {
        _isConnecting.value = false
        _connectingSourceId.value = null
    }

    private fun addEdge(sourceId: String, targetId: String, type: VsmEdgeType) {
        val newEdge = VsmEdge(UUID.randomUUID().toString(), sourceId, targetId, type)
        updateActiveScenario { state ->
            state.copy(edges = state.edges + newEdge)
        }
    }

    private fun updateActiveScenario(update: (VsmState) -> VsmState) {
        val currentId = _activeScenarioId.value ?: return
        _scenarios.update { list ->
            list.map { scn ->
                if (scn.id == currentId) {
                    val updated = update(scn)
                    updateMetrics(updated)
                    updated
                } else scn
            }
        }
    }
}
