package com.example.data

enum class ValueClassification { VA, NVA, NNVA }
enum class WasteCategory { NONE, TRANSPORTATION, INVENTORY, MOTION, WAITING, OVERPRODUCTION, OVERPROCESSING, DEFECT, SKILLS, SEARCHING, EXCESS_HANDLING, REWORK, OTHER }
enum class TimeSource { OBSERVED, CALCULATED, ESTIMATED }
enum class ElementApplicability { COMMON, MODEL_SPECIFIC, VARIANT_SPECIFIC }

data class Plant(val id: String, val name: String, val location: String)
data class Line(val id: String, val plantId: String, val name: String, val taktTime: Double? = null)

data class Project(
    val id: String, 
    val lineId: String, 
    val name: String, 
    val description: String, 
    val status: String
)

data class Model(
    val id: String, 
    val projectId: String, 
    val name: String, 
    val demand: Int,
    val variant: String = "Standard",
    val family: String = "Main",
    val isActive: Boolean = true
) {
    init { require(demand >= 0) { "Demand cannot be negative." } }
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
    val ieOverridden: Boolean = false,
    val applicability: ElementApplicability = ElementApplicability.COMMON,
    val applicableModelIds: List<String> = emptyList(),
    val modelStandardTimes: Map<String, Double> = emptyMap(),
    val frequency: Double = 1.0
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

// ==========================================
// VERSION 2.1 — MULTI-MODEL LINE BALANCING ENTITIES
// ==========================================

data class ProductionPlan(
    val id: String,
    val projectId: String,
    val name: String,
    val period: String,
    val shiftName: String,
    val availableOperatingSeconds: Double,
    val plannedOperatingSeconds: Double,
    val plannedTotalQuantity: Int,
    val requiredTaktSeconds: Double,
    val operatorCount: Int = 4,
    val workingCalendar: String = "Standard 5-Day (2 Shifts)"
) {
    init {
        require(plannedTotalQuantity >= 0) { "Planned quantity cannot be negative." }
        require(availableOperatingSeconds >= 0) { "Available time cannot be negative." }
    }
}

data class ModelMixItem(
    val id: String,
    val planId: String,
    val modelId: String,
    val modelName: String,
    val variant: String,
    val plannedQuantity: Int,
    val calculatedMixPercentage: Double,
    val manualMixOverride: Double? = null,
    val requiredTakt: Double,
    val standardWorkContent: Double = 0.0,
    val isActive: Boolean = true
) {
    val effectiveMixPercentage: Double
        get() = manualMixOverride ?: calculatedMixPercentage
}

data class ProductionSequence(
    val id: String,
    val planId: String,
    val name: String,
    val modelPattern: List<String>,
    val batchSizes: Map<String, Int> = emptyMap(),
    val isRepeating: Boolean = true,
    val pitchMinutes: Double = 1.0
)

enum class ConstraintType {
    PRECEDENCE_VIOLATION,
    MODEL_NOT_APPLICABLE,
    TOOL_UNAVAILABLE,
    STATION_CAPABILITY_MISMATCH,
    ERGONOMIC_LIMIT_EXCEEDED
}

data class ConstraintViolation(
    val type: ConstraintType,
    val elementId: String,
    val elementName: String,
    val description: String
)

enum class RedistributionChangeType {
    MOVE_STATION,
    MOVE_OPERATOR,
    COMBINE_ELEMENTS,
    SPLIT_ELEMENT,
    ELIMINATE_NVA,
    CHANGE_SEQUENCE,
    CHANGE_MANPOWER
}

data class RedistributionProposal(
    val elementId: String,
    val changeType: RedistributionChangeType,
    val sourceStationId: String,
    val targetStationId: String,
    val sourceOperatorId: String? = null,
    val targetOperatorId: String? = null,
    val reason: String = ""
)

data class MultiModelAiDiagnosis(
    val observedFacts: List<String>,
    val calculatedResults: List<String>,
    val engineeringInterpretation: String,
    val recommendations: List<String>,
    val risksAndConstraints: List<String>,
    val assumptions: List<String>,
    val validationRequired: List<String>
)

// =============================================================================
// V2.2 ADVANCED AI VIDEO TIME STUDY ENTITIES
// =============================================================================

enum class VideoStudyStatus {
    DRAFT,
    ANALYZING,
    AWAITING_VALIDATION,
    PARTIALLY_VALIDATED,
    VALIDATED,
    FAILED
}

enum class VideoActivityCategory {
    REACH,
    PICK,
    MOVE,
    POSITION,
    ALIGN,
    INSERT,
    ASSEMBLE,
    FASTEN,
    TIGHTEN,
    WELD,
    INSPECT,
    MEASURE,
    WALK,
    SEARCH,
    WAIT,
    HANDLE_MATERIAL,
    PLACE,
    REMOVE,
    LOAD,
    UNLOAD,
    MACHINE_INTERACTION,
    TOOL_INTERACTION,
    OTHER
}

enum class ValidationStatus {
    AI_SUGGESTED,
    USER_VALIDATED,
    USER_EDITED,
    REJECTED
}

enum class DataSourceLabel {
    USER_INPUT,
    OBSERVED,
    AI_SUGGESTED,
    CALCULATED,
    USER_VALIDATED,
    USER_EDITED,
    ESTIMATED
}

data class VideoStudyMetadata(
    val id: String,
    val projectId: String,
    val name: String,
    val modelId: String,
    val variant: String = "Standard",
    val lineId: String = "L-01",
    val processId: String = "PRC-1",
    val stationId: String = "ST-04",
    val operatorId: String = "OP-1",
    val videoFileName: String,
    val videoDurationSeconds: Double,
    val shift: String = "Day Shift (08:00 - 16:30)",
    val date: String = "2026-09-18",
    val productionCondition: String = "Nominal Run Rate",
    val expectedTaktSeconds: Double = 60.0,
    val availableManpower: Int = 1,
    val targetOutputUnits: Int = 450,
    val notes: String = "",
    val status: VideoStudyStatus = VideoStudyStatus.AWAITING_VALIDATION,
    val createdAt: Long = System.currentTimeMillis()
)

data class ImmutableCandidateSnapshot(
    val name: String,
    val startTime: Double,
    val endTime: Double,
    val duration: Double,
    val activityCategory: VideoActivityCategory,
    val classification: ValueClassification,
    val confidenceScore: Double
)

data class AICandidateElement(
    val id: String,
    val studyId: String,
    val elementId: String? = null,
    val cycleNumber: Int = 1,
    val sequence: Int = 10,
    val name: String,
    val description: String = "",
    val startTime: Double,
    val endTime: Double,
    val duration: Double = (endTime - startTime).coerceAtLeast(0.0),
    val activityCategory: VideoActivityCategory = VideoActivityCategory.ASSEMBLE,
    val otherActivityDescription: String = "",
    val suggestedClassification: ValueClassification = ValueClassification.VA,
    val finalClassification: ValueClassification = suggestedClassification,
    val wasteCategory: WasteCategory = WasteCategory.NONE,
    val confidenceScore: Double = 0.90, // 0.0 - 1.0 (AI confidence in activity identification)
    val validationStatus: ValidationStatus = ValidationStatus.AI_SUGGESTED,
    val originalAiSuggestion: ImmutableCandidateSnapshot? = null,
    val toolNames: List<String> = emptyList(),
    val materialNames: List<String> = emptyList(),
    val notes: String = "",
    val isAbnormal: Boolean = false,
    val abnormalEventReason: String = ""
) {
    init {
        require(startTime >= 0.0) { "Start time must be non-negative: $startTime" }
        require(endTime >= startTime) { "End time ($endTime) cannot precede start time ($startTime)" }
    }

    val confidenceLabel: String
        get() = when {
            confidenceScore >= 0.85 -> "High Confidence"
            confidenceScore >= 0.65 -> "Medium Confidence"
            else -> "Low Confidence (Validation Required)"
        }
}

data class StudyCycle(
    val id: String,
    val studyId: String,
    val cycleNumber: Int,
    val startTime: Double,
    val endTime: Double,
    val duration: Double = (endTime - startTime).coerceAtLeast(0.0),
    val isNormal: Boolean = true,
    val isExcluded: Boolean = false,
    val exclusionReason: String = ""
)

data class CycleStatistics(
    val cycleCount: Int,
    val validCycleCount: Int,
    val excludedCycleCount: Int,
    val averageCycleTime: Double,
    val minCycleTime: Double,
    val maxCycleTime: Double,
    val medianCycleTime: Double,
    val rangeCycleTime: Double,
    val stdDevCycleTime: Double?, // null if insufficient valid cycles (< 2)
    val cvPercent: Double?,       // null if insufficient valid cycles (< 2)
    val averageVaTime: Double,
    val averageNnvaTime: Double,
    val averageNvaTime: Double,
    val vaPercent: Double,
    val nnvaPercent: Double,
    val nvaPercent: Double
)

data class VideoImprovementOpportunity(
    val id: String,
    val studyId: String,
    val title: String,
    val patternType: String,
    val evidence: String,
    val affectedElementIds: List<String>,
    val observedCalculatedData: String,
    val potentialCause: String,
    val suggestedAction: String,
    val risk: String,
    val validationRequired: String
)

data class VideoStudyAiReport(
    val observedFacts: List<String>,
    val calculatedResults: List<String>,
    val engineeringInterpretation: String,
    val potentialImprovements: List<String>,
    val risksAndConstraints: List<String>,
    val assumptions: List<String>,
    val validationRequired: List<String>
)

// =============================================================================
// V2.3 — AI IE COPILOT ENTITIES
// =============================================================================

enum class AIProviderType { GEMINI, NVIDIA_NIM, OPENAI_COMPATIBLE, LOCAL }
enum class AiRole { USER, ASSISTANT, SYSTEM }

data class AIProviderConfig(
    val id: String,
    val name: String,
    val type: AIProviderType,
    val baseUrl: String,
    val apiKey: String? = null,
    val isActive: Boolean = true,
    val capabilities: List<String> = emptyList() // "text", "vision", "tool-calling", "streaming"
)

data class AIModelConfig(
    val id: String,
    val providerId: String,
    val modelId: String,
    val name: String,
    val temperature: Float = 0.1f, // Low temperature for deterministic engineering analysis
    val maxTokens: Int = 4096,
    val isDefault: Boolean = false,
    val taskType: String = "GENERAL" // "GENERAL", "REASONING", "VISION", "WHAT_IF"
)

data class AiChatMessage(
    val id: String,
    val projectId: String,
    val role: AiRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val evidenceIds: List<String> = emptyList(),
    val modelUsed: String? = null,
    val providerUsed: String? = null
)

data class AiEvidence(
    val id: String,
    val source: String, // "STATION", "ELEMENT", "CALCULATION", "OBSERVATION", "SCENARIO"
    val referenceId: String,
    val dataLabel: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiIeAnalysis(
    val observedFacts: List<String>,
    val calculatedResults: List<String>,
    val engineeringInterpretation: String,
    val possibleRootCauses: List<String>,
    val recommendations: List<String>,
    val expectedImpact: String,
    val risksAndConstraints: List<String>,
    val assumptions: List<String>,
    val validationRequired: List<String>
)

data class AiProjectHealth(
    val stableAreas: List<String>,
    val attentionAreas: List<String>,
    val highRiskAreas: List<String>,
    val dataGaps: List<String>,
    val investigationRequired: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiActionPlanItem(
    val id: String,
    val problem: String,
    val evidence: String,
    val action: String,
    val area: String,
    val station: String,
    val element: String?,
    val owner: String,
    val dueDate: String,
    val expectedBenefit: String,
    val measurementMethod: String,
    val status: String = "OPEN",
    val isAiSuggested: Boolean = true
)

// =============================================================================
// V2.4 — INTEGRATED OPERATIONAL EXCELLENCE ENTITIES
// =============================================================================

enum class OeeLossCategory {
    PLANNED_DOWNTIME, // Management Loss, Meeting, Break
    AVAILABILITY_LOSS, // Breakdown, Setup, Tool Change, Minor Stop
    PERFORMANCE_LOSS, // Speed Loss, Waiting, Material Delay
    QUALITY_LOSS // Scrap, Rework
}

data class OeeRecord(
    val id: String,
    val projectId: String,
    val lineId: String,
    val date: String,
    val shift: String,
    val plannedProductionTimeMinutes: Double,
    val idealCycleTimeSeconds: Double,
    val totalCount: Int,
    val goodCount: Int,
    val rejectCount: Int
)

data class LossEvent(
    val id: String,
    val oeeRecordId: String,
    val category: OeeLossCategory,
    val reason: String,
    val startTime: Long,
    val durationMinutes: Double,
    val stationId: String? = null,
    val machineId: String? = null,
    val modelId: String? = null,
    val m4Category: String? = null, // Man, Machine, Material, Method
    val evidenceReference: String? = null
)

enum class RcaStatus { IDENTIFIED, ANALYZING, CAUSE_FOUND, COUNTERMEASURE_DEFINED, CLOSED }

data class FiveWhyStep(
    val whyNumber: Int,
    val whyText: String,
    val evidence: String? = null
)

data class RcaRecord(
    val id: String,
    val problemStatement: String,
    val evidenceReference: String?,
    val lossEventId: String? = null,
    val status: RcaStatus = RcaStatus.IDENTIFIED,
    val fiveWhys: List<FiveWhyStep> = emptyList(),
    val fishboneData: Map<String, List<String>> = emptyMap(), // Category -> List of Causes
    val rootCause: String? = null,
    val countermeasure: String? = null,
    val owner: String? = null,
    val dueDate: String? = null,
    val validationStatus: ValidationStatus = ValidationStatus.AI_SUGGESTED
)

enum class KaizenStatus { IDEA, UNDER_ANALYSIS, APPROVED, IN_PROGRESS, IMPLEMENTED, UNDER_VALIDATION, CLOSED, REJECTED }

data class KaizenRecord(
    val id: String,
    val title: String,
    val rcaId: String?,
    val status: KaizenStatus = KaizenStatus.IDEA,
    val problem: String,
    val countermeasure: String,
    val beforeMetricValue: Double? = null,
    val afterMetricValue: Double? = null,
    val metricUnit: String? = null,
    val owner: String,
    val targetDate: String,
    val actualDate: String? = null,
    val stationId: String? = null,
    val modelId: String? = null,
    val department: String? = null,
    val priority: String = "Medium",
    val evidenceBefore: String? = null,
    val evidenceAfter: String? = null,
    val validationStatus: ValidationStatus = ValidationStatus.AI_SUGGESTED
)

enum class SwStatus { DRAFT, IE_REVIEWED, APPROVED, RELEASED, SUPERSEDED }

data class StandardWorkElement(
    val id: String,
    val workElementId: String?, // Link to validated time study element
    val sequence: Int,
    val name: String,
    val durationSeconds: Double,
    val isQualityCheck: Boolean = false,
    val isSafetyPoint: Boolean = false,
    val tools: List<String> = emptyList(),
    val keyPoints: String = "",
    val criticalPoints: String = ""
)

data class StandardWorkRevision(
    val id: String,
    val projectId: String,
    val stationId: String,
    val modelId: String,
    val version: Int,
    val status: SwStatus = SwStatus.DRAFT,
    val elements: List<StandardWorkElement> = emptyList(),
    val taktTime: Double,
    val author: String,
    val kaizenId: String? = null, // Link to the improvement that triggered this revision
    val createdAt: Long = System.currentTimeMillis()
)

data class ErgoRiskFactor(
    val factor: String, // Posture, Force, Repetition, etc.
    val observation: String,
    val riskLevel: String, // Low, Medium, High
    val action: String? = null
)

data class ErgoAssessment(
    val id: String,
    val projectId: String,
    val stationId: String,
    val workElementId: String? = null,
    val method: String = "RULA", // RULA, REBA, etc.
    val assessmentDate: String,
    val assessor: String,
    val riskFactors: List<ErgoRiskFactor> = emptyList(),
    val findings: List<String> = emptyList(),
    val totalScore: Int,
    val riskLevel: String,
    val recommendation: String = "",
    val validationStatus: ValidationStatus = ValidationStatus.AI_SUGGESTED
)

data class VsmProcessStep(
    val id: String,
    val name: String,
    val cycleTime: Double,
    val vaTime: Double,
    val uptime: Double,
    val operators: Int,
    val wip: Int,
    val changeoverTime: Double = 0.0,
    val stationId: String? = null
)

data class VsmMap(
    val id: String,
    val projectId: String,
    val name: String,
    val isFutureState: Boolean = false,
    val steps: List<VsmProcessStep> = emptyList(),
    val demandPerDay: Int,
    val availableTimeSeconds: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class MaterialFlow(
    val id: String,
    val projectId: String,
    val sourceId: String,
    val destinationId: String,
    val materialId: String,
    val quantityPerUnit: Double,
    val frequencyPerShift: Int,
    val travelDistanceMeters: Double,
    val handlingTimeSeconds: Double
)

enum class BenefitType { TIME_SAVING, CAPACITY_INCREASE, MANPOWER_OPTIMISATION, QUALITY_IMPROVEMENT, SCRAP_REDUCTION, ENERGY_SAVING, SPACE_SAVING, LEAD_TIME_REDUCTION }

data class ImprovementBenefit(
    val id: String,
    val kaizenId: String,
    val type: BenefitType,
    val value: Double,
    val unit: String,
    val source: DataSourceLabel = DataSourceLabel.CALCULATED,
    val description: String = ""
)

data class OeeMetrics(
    val availability: Double,
    val performance: Double,
    val quality: Double,
    val oee: Double,
    val plannedProductionTimeMinutes: Double,
    val availableTimeMinutes: Double,
    val runTimeMinutes: Double,
    val availabilityLossMinutes: Double,
    val performanceLossMinutes: Double,
    val qualityLossCount: Int
)

// =============================================================================
// V2.5 — ENTERPRISE IE & DIGITAL SIMULATION ENTITIES
// =============================================================================

data class TimeStudyTemplate(
    val id: String,
    val name: String,
    val activityCategory: VideoActivityCategory,
    val defaultClassification: ValueClassification = ValueClassification.VA,
    val defaultWasteCategory: WasteCategory = WasteCategory.NONE,
    val colorHex: String? = null
)

data class SimulationNode(
    val stationId: String,
    val cycleTimeSeconds: Double,
    val uptimePercent: Double = 100.0,
    val operatorCount: Int = 1,
    val batchSize: Int = 1,
    val incomingWip: Int = 0
)

data class SimulationResult(
    val id: String,
    val projectId: String,
    val scenarioName: String,
    val throughputPerHour: Double,
    val cycleTimeSeconds: Double,
    val bottleneckStationId: String,
    val averageWip: Double,
    val operatorUtilisation: Map<String, Double>, // StationId -> Util %
    val stationIdleTime: Map<String, Double>, // StationId -> Seconds
    val leadTimeMinutes: Double,
    val capacityUtilization: Double,
    val simulatedUnits: Int,
    val simulationDurationHours: Double = 8.0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BenchmarkMetric {
    OEE, CYCLE_TIME, BALANCE_EFFICIENCY, VA_PERCENT, PRODUCTIVITY, CAPACITY_UTIL_PERCENT, LEAD_TIME
}

data class EnterpriseKpi(
    val id: String,
    val entityId: String, // Plant, Line, or Project ID
    val entityType: String, // "PLANT", "LINE", "PROJECT"
    val metric: BenchmarkMetric,
    val value: Double,
    val target: Double? = null,
    val period: String,
    val trend: Double? = null
)

data class ProductivityRecord(
    val id: String,
    val projectId: String,
    val date: String,
    val shift: String,
    val actualOutput: Int,
    val totalLabourHours: Double,
    val productivityIndex: Double, // Units per labour hour
    val labourContentSeconds: Double // Total seconds per unit
)

data class SavingsValidation(
    val id: String,
    val benefitId: String,
    val status: ValidationStatus = ValidationStatus.AI_SUGGESTED,
    val validatedBy: String? = null,
    val validatedAt: Long? = null,
    val notes: String = ""
)

