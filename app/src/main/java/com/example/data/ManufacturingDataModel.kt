package com.example.data

enum class ValueClassification { VA, NVA, NNVA }
enum class WasteCategory { NONE, TRANSPORTATION, INVENTORY, MOTION, WAITING, OVERPRODUCTION, OVERPROCESSING, DEFECT, SKILLS, SEARCHING, EXCESS_HANDLING, REWORK, OTHER }
enum class TimeSource { OBSERVED, CALCULATED, ESTIMATED }

data class Plant(val id: String, val name: String, val location: String)
data class Line(val id: String, val plantId: String, val name: String, val taktTime: Double? = null)

data class Project(
    val id: String, 
    val lineId: String, 
    val name: String, 
    val description: String, 
    val status: String
)

data class Model(val id: String, val projectId: String, val name: String, val demand: Int) {
    init { require(demand > 0) { "Demand must be positive." } }
}

data class Process(val id: String, val lineId: String, val name: String)
data class Station(val id: String, val processId: String, val name: String)
data class Operator(val id: String, val name: String, val skillLevel: String)

data class WorkElement(
    val id: String,
    val projectId: String,
    val modelId: String,
    val processId: String,
    val stationId: String,
    val operatorId: String,
    val sequence: Int,
    val name: String,
    val description: String,
    val startTime: Double,
    val endTime: Double,
    val observedTime: Double,
    val normalTime: Double,
    val standardTime: Double,
    val performanceRating: Double,
    val allowance: Double,
    val valueClassification: ValueClassification,
    val wasteCategory: WasteCategory,
    val toolIds: List<String>,
    val materialIds: List<String>,
    val predecessorIds: List<String>,
    val successorIds: List<String>,
    val transferable: Boolean,
    val combinable: Boolean,
    val parallelizable: Boolean,
    val eliminable: Boolean,
    val automationOpportunity: Boolean,
    val confidence: Double,
    val evidenceReference: String?,
    val notes: String,
    val timeSource: TimeSource = TimeSource.OBSERVED,
    val classificationReason: String = "",
    val ieOverridden: Boolean = false
) {
    init {
        require(observedTime >= 0) { "Element cannot have negative time." }
        require(normalTime >= 0) { "Element cannot have negative normal time." }
        require(standardTime >= 0) { "Element cannot have negative standard time." }
        require(endTime >= startTime) { "End time cannot precede start time." }
    }
}

data class Observation(
    val id: String, 
    val workElementId: String, 
    val cycleId: String, 
    val observedTime: Double, 
    val notes: String,
    val isRejected: Boolean = false,
    val timeSource: TimeSource = TimeSource.OBSERVED
) {
    init { require(observedTime >= 0) { "Observation time cannot be negative." } }
}

data class Cycle(val id: String, val workElementId: String, val cycleNumber: Int, val isOutlier: Boolean)
data class Tool(val id: String, val name: String)
data class Material(val id: String, val name: String, val cost: Double)
data class Dependency(val id: String, val predecessorId: String, val successorId: String, val type: String)
data class Classification(val id: String, val name: String, val category: String)

data class Scenario(val id: String, val baseProjectId: String, val name: String, val description: String, val createdAt: Long)

data class ScenarioElement(
    val id: String,
    val scenarioId: String,
    val baseWorkElementId: String,
    val deltaObservedTime: Double?,
    val deltaStationId: String?,
    val deltaOperatorId: String?,
    val deltaSequence: Int?,
    val deltaValueClassification: ValueClassification?
)

data class VSMProcess(val id: String, val projectId: String, val name: String, val cycleTime: Double, val changeoverTime: Double, val uptime: Double)
data class LayoutObject(val id: String, val projectId: String, val type: String, val x: Double, val y: Double, val width: Double, val height: Double)
data class MotionEvent(val id: String, val workElementId: String, val therblig: String, val timeMs: Long)

data class CapacityPlan(val id: String, val projectId: String, val demand: Int, val availableTime: Double, val taktTime: Double) {
    init {
        require(taktTime > 0) { "Takt time must be positive." }
        require(demand > 0) { "Demand must be positive." }
        require(availableTime > 0) { "Available time must be positive." }
    }
}

data class ManpowerPlan(val id: String, val projectId: String, val requiredOperators: Int, val efficiency: Double)
data class KaizenAction(val id: String, val workElementId: String, val title: String, val status: String, val assignedTo: String)
data class SavingsRecord(val id: String, val kaizenId: String, val timeSavedSeconds: Double, val costSaved: Double)
data class StandardWork(val id: String, val stationId: String, val documentUrl: String, val version: String)
data class ErgonomicAssessment(val id: String, val workElementId: String, val rulaScore: Int, val rebaScore: Int, val riskLevel: String)
