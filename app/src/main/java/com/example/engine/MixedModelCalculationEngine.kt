package com.example.engine

import com.example.data.*
import kotlin.math.max

data class MixedModelStationMetrics(
    val station: Station,
    val stationSequence: Int,
    val modelCycleTimes: Map<String, Double>, // modelId -> cycle time in seconds
    val modelVaTimes: Map<String, Double>,
    val modelNnvaTimes: Map<String, Double>,
    val modelNvaTimes: Map<String, Double>,
    val weightedCycleTime: Double, // sum(CT_m * MixRatio_m)
    val maxCycleTime: Double, // max(CT_m)
    val maxModelId: String,
    val taktTime: Double,
    val isBottleneckWeighted: Boolean = false,
    val isBottleneckMax: Boolean = false,
    val isOverTaktWeighted: Boolean = false,
    val isOverTaktMax: Boolean = false,
    val weightedIdleTime: Double = 0.0,
    val weightedUtilization: Double = 0.0,
    val weightedVaTime: Double = 0.0,
    val weightedNnvaTime: Double = 0.0,
    val weightedNvaTime: Double = 0.0
)

data class MixedModelLineSummary(
    val planId: String,
    val totalPlannedUnits: Int,
    val plannedOperatingSeconds: Double,
    val lineTakt: Double,
    val stationCount: Int,
    val operatorCount: Int,
    val weightedBalanceEfficiency: Double, // Total weighted workload / (stationCount * Takt) * 100
    val weightedLineBalanceLoss: Double, // 100 - weightedBalanceEfficiency
    val modelBalanceEfficiencies: Map<String, Double>, // modelId -> Model BE %
    val weightedBottleneckStation: Station?,
    val maxBottleneckStation: Station?,
    val weightedBottleneckCycleTime: Double,
    val maxOverallCycleTime: Double,
    val totalWeightedWorkContent: Double,
    val effectiveMixedCapacityPerHour: Double,
    val capacityHeadroomPercent: Double,
    val stationMetrics: List<MixedModelStationMetrics>
)

data class SequenceAnalysisResult(
    val patternLength: Int,
    val uniqueModels: List<String>,
    val maxConsecutiveOverTaktCount: Int,
    val bottleneckRiskStations: List<String>,
    val pitchTimeSeconds: Double,
    val smoothnessScore: Double // 0-100 heuristic of sequence balance
)

data class RedistributionSimulationResult(
    val baseSummary: MixedModelLineSummary,
    val draftSummary: MixedModelLineSummary,
    val violations: List<ConstraintViolation>,
    val deltaWeightedBalanceEfficiency: Double,
    val deltaMaxCycleTime: Double,
    val deltaEffectiveCapacityPerHour: Double,
    val isFeasible: Boolean
)

object MixedModelCalculationEngine {

    /**
     * Calculates automatic Model Mix percentages and individual model Takt requirements.
     * Mix % = Model Quantity / Total Quantity * 100
     */
    fun calculateModelMix(
        models: List<Model>,
        planId: String,
        plannedOperatingSeconds: Double,
        elements: List<WorkElement>
    ): List<ModelMixItem> {
        val totalQuantity = models.sumOf { if (it.isActive) it.demand else 0 }
        
        return models.map { model ->
            val mixPercent = if (totalQuantity > 0 && model.isActive) {
                (model.demand.toDouble() / totalQuantity.toDouble()) * 100.0
            } else 0.0

            val requiredTakt = if (model.demand > 0 && model.isActive && plannedOperatingSeconds > 0) {
                plannedOperatingSeconds / model.demand.toDouble()
            } else 0.0

            // Compute standard work content for this model
            val workContent = elements.sumOf { el ->
                getElementDurationForModel(el, model.id) ?: 0.0
            }

            ModelMixItem(
                id = "MIX-${model.id}",
                planId = planId,
                modelId = model.id,
                modelName = model.name,
                variant = model.variant,
                plannedQuantity = model.demand,
                calculatedMixPercentage = mixPercent,
                manualMixOverride = null,
                requiredTakt = requiredTakt,
                standardWorkContent = workContent,
                isActive = model.isActive
            )
        }
    }

    /**
     * Determines the duration of a work element for a given model.
     * Returns null if the element is NOT applicable to this model.
     */
    fun getElementDurationForModel(element: WorkElement, modelId: String): Double? {
        val isApplicable = when (element.applicability) {
            ElementApplicability.COMMON -> {
                // If applicableModelIds is empty, it applies to all models. Otherwise check inclusion.
                element.applicableModelIds.isEmpty() || element.applicableModelIds.contains(modelId)
            }
            ElementApplicability.MODEL_SPECIFIC, ElementApplicability.VARIANT_SPECIFIC -> {
                element.applicableModelIds.contains(modelId) || element.modelId == modelId
            }
        }

        if (!isApplicable) return null

        // Model specific duration override or standard time adjusted by frequency
        val baseDuration = element.modelStandardTimes[modelId] ?: element.standardTime
        return max(0.0, baseDuration * element.frequency)
    }

    /**
     * Calculates station cycle time for a specific model.
     */
    fun calculateStationModelCycleTime(
        elements: List<WorkElement>,
        modelId: String
    ): Double {
        return elements.sumOf { getElementDurationForModel(it, modelId) ?: 0.0 }
    }

    /**
     * Calculates station metrics across all models, weighted CT, and max CT.
     */
    fun calculateStationMetrics(
        station: Station,
        stationSequence: Int,
        elements: List<WorkElement>,
        models: List<Model>,
        mixItems: List<ModelMixItem>,
        lineTakt: Double
    ): MixedModelStationMetrics {
        val activeMixMap = mixItems.filter { it.isActive }.associate { it.modelId to (it.effectiveMixPercentage / 100.0) }
        
        val modelCtMap = mutableMapOf<String, Double>()
        val modelVaMap = mutableMapOf<String, Double>()
        val modelNnvaMap = mutableMapOf<String, Double>()
        val modelNvaMap = mutableMapOf<String, Double>()

        models.filter { it.isActive }.forEach { model ->
            var ct = 0.0
            var va = 0.0
            var nnva = 0.0
            var nva = 0.0

            elements.forEach { el ->
                val duration = getElementDurationForModel(el, model.id)
                if (duration != null) {
                    ct += duration
                    when (el.valueClassification) {
                        ValueClassification.VA -> va += duration
                        ValueClassification.NNVA -> nnva += duration
                        ValueClassification.NVA -> nva += duration
                    }
                }
            }

            modelCtMap[model.id] = ct
            modelVaMap[model.id] = va
            modelNnvaMap[model.id] = nnva
            modelNvaMap[model.id] = nva
        }

        // Weighted Average CT = sum(CT_m * MixRatio_m)
        var weightedCt = 0.0
        var weightedVa = 0.0
        var weightedNnva = 0.0
        var weightedNva = 0.0

        modelCtMap.forEach { (mId, ct) ->
            val ratio = activeMixMap[mId] ?: 0.0
            weightedCt += ct * ratio
            weightedVa += (modelVaMap[mId] ?: 0.0) * ratio
            weightedNnva += (modelNnvaMap[mId] ?: 0.0) * ratio
            weightedNva += (modelNvaMap[mId] ?: 0.0) * ratio
        }

        val maxEntry = modelCtMap.maxByOrNull { it.value }
        val maxCt = maxEntry?.value ?: 0.0
        val maxModel = maxEntry?.key ?: ""

        val weightedIdle = if (lineTakt > 0) max(0.0, lineTakt - weightedCt) else 0.0
        val utilization = if (lineTakt > 0) (weightedCt / lineTakt) * 100.0 else 0.0

        return MixedModelStationMetrics(
            station = station,
            stationSequence = stationSequence,
            modelCycleTimes = modelCtMap,
            modelVaTimes = modelVaMap,
            modelNnvaTimes = modelNnvaMap,
            modelNvaTimes = modelNvaMap,
            weightedCycleTime = weightedCt,
            maxCycleTime = maxCt,
            maxModelId = maxModel,
            taktTime = lineTakt,
            isOverTaktWeighted = lineTakt > 0 && weightedCt > lineTakt,
            isOverTaktMax = lineTakt > 0 && maxCt > lineTakt,
            weightedIdleTime = weightedIdle,
            weightedUtilization = utilization,
            weightedVaTime = weightedVa,
            weightedNnvaTime = weightedNnva,
            weightedNvaTime = weightedNva
        )
    }

    /**
     * Calculates the entire mixed-model line summary and efficiencies.
     */
    fun calculateLineSummary(
        plan: ProductionPlan,
        stations: List<Station>,
        elements: List<WorkElement>,
        models: List<Model>,
        mixItems: List<ModelMixItem>
    ): MixedModelLineSummary {
        val lineTakt = plan.requiredTaktSeconds
        val elementsByStation = elements.groupBy { it.stationId }

        val stationMetrics = stations.mapIndexed { index, station ->
            val stElements = elementsByStation[station.id] ?: emptyList()
            calculateStationMetrics(
                station = station,
                stationSequence = index + 1,
                elements = stElements,
                models = models,
                mixItems = mixItems,
                lineTakt = lineTakt
            )
        }

        val weightedBottleneckMetric = stationMetrics.maxByOrNull { it.weightedCycleTime }
        val maxBottleneckMetric = stationMetrics.maxByOrNull { it.maxCycleTime }

        val stationMetricsWithBottleneck = stationMetrics.map { sm ->
            sm.copy(
                isBottleneckWeighted = weightedBottleneckMetric != null && sm.station.id == weightedBottleneckMetric.station.id,
                isBottleneckMax = maxBottleneckMetric != null && sm.station.id == maxBottleneckMetric.station.id
            )
        }

        val totalWeightedWorkload = stationMetrics.sumOf { it.weightedCycleTime }
        val stationCount = stations.size

        // Metric A: Weighted Balance Efficiency = Total weighted station workload / (Number of stations * Takt)
        val weightedBalanceEfficiency = if (stationCount > 0 && lineTakt > 0) {
            (totalWeightedWorkload / (stationCount * lineTakt)) * 100.0
        } else 0.0

        val weightedBalanceLoss = max(0.0, 100.0 - weightedBalanceEfficiency)

        // Metric B: Model-specific Balance Efficiency for each model
        // Formula: sum(CT_m) / (Number of stations * max(CT_station, m))
        val modelEfficiencies = mutableMapOf<String, Double>()
        models.filter { it.isActive }.forEach { model ->
            val totalModelWorkload = stationMetrics.sumOf { it.modelCycleTimes[model.id] ?: 0.0 }
            val maxModelStationCt = stationMetrics.maxOfOrNull { it.modelCycleTimes[model.id] ?: 0.0 } ?: 0.0
            val eff = if (stationCount > 0 && maxModelStationCt > 0) {
                (totalModelWorkload / (stationCount * maxModelStationCt)) * 100.0
            } else 0.0
            modelEfficiencies[model.id] = eff
        }

        val weightedBottleneckCt = weightedBottleneckMetric?.weightedCycleTime ?: 0.0
        val effectiveCapacity = if (weightedBottleneckCt > 0) {
            3600.0 / weightedBottleneckCt
        } else 0.0

        val headroom = if (lineTakt > 0 && weightedBottleneckCt > 0) {
            ((lineTakt - weightedBottleneckCt) / lineTakt) * 100.0
        } else 0.0

        return MixedModelLineSummary(
            planId = plan.id,
            totalPlannedUnits = plan.plannedTotalQuantity,
            plannedOperatingSeconds = plan.plannedOperatingSeconds,
            lineTakt = lineTakt,
            stationCount = stationCount,
            operatorCount = plan.operatorCount,
            weightedBalanceEfficiency = weightedBalanceEfficiency,
            weightedLineBalanceLoss = weightedBalanceLoss,
            modelBalanceEfficiencies = modelEfficiencies,
            weightedBottleneckStation = weightedBottleneckMetric?.station,
            maxBottleneckStation = maxBottleneckMetric?.station,
            weightedBottleneckCycleTime = weightedBottleneckCt,
            maxOverallCycleTime = maxBottleneckMetric?.maxCycleTime ?: 0.0,
            totalWeightedWorkContent = totalWeightedWorkload,
            effectiveMixedCapacityPerHour = effectiveCapacity,
            capacityHeadroomPercent = headroom,
            stationMetrics = stationMetricsWithBottleneck
        )
    }

    /**
     * Constraint Validation Engine.
     * Enforces Precedence, Model Applicability, and Station sequence rules.
     */
    fun validatePrecedenceAndConstraints(
        element: WorkElement,
        targetStation: Station,
        targetSequence: Int,
        allElements: List<WorkElement>,
        allStations: List<Station>
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val stationSeqMap = allStations.mapIndexed { idx, st -> st.id to (idx + 1) }.toMap()
        val targetStationSeq = stationSeqMap[targetStation.id] ?: targetSequence

        // 1. Precedence check: All predecessors must be at or before target station
        element.predecessorIds.forEach { predId ->
            val predElement = allElements.find { it.id == predId }
            if (predElement != null) {
                val predStationSeq = stationSeqMap[predElement.stationId] ?: 0
                if (predStationSeq > targetStationSeq) {
                    val predStationName = allStations.find { it.id == predElement.stationId }?.name ?: "Unknown"
                    violations.add(
                        ConstraintViolation(
                            type = ConstraintType.PRECEDENCE_VIOLATION,
                            elementId = element.id,
                            elementName = element.name,
                            description = "Precedence constraint violated: Predecessor '${predElement.name}' is assigned downstream at '$predStationName' (Seq $predStationSeq), but target is '$targetStation' (Seq $targetStationSeq)."
                        )
                    )
                }
            }
        }

        // 2. Successor check: All successors must be at or after target station
        element.successorIds.forEach { succId ->
            val succElement = allElements.find { it.id == succId }
            if (succElement != null) {
                val succStationSeq = stationSeqMap[succElement.stationId] ?: 0
                if (succStationSeq < targetStationSeq) {
                    val succStationName = allStations.find { it.id == succElement.stationId }?.name ?: "Unknown"
                    violations.add(
                        ConstraintViolation(
                            type = ConstraintType.PRECEDENCE_VIOLATION,
                            elementId = element.id,
                            elementName = element.name,
                            description = "Successor constraint violated: Successor '${succElement.name}' is assigned upstream at '$succStationName' (Seq $succStationSeq), but target is '$targetStation' (Seq $targetStationSeq)."
                        )
                    )
                }
            }
        }

        // 3. Transferability flag
        if (!element.transferable && element.stationId != targetStation.id) {
            violations.add(
                ConstraintViolation(
                    type = ConstraintType.STATION_CAPABILITY_MISMATCH,
                    elementId = element.id,
                    elementName = element.name,
                    description = "Element '${element.name}' is marked non-transferable due to fixed station equipment or mechanical tooling."
                )
            )
        }

        return violations
    }

    /**
     * Simulates what-if element redistribution across stations.
     */
    fun simulateRedistribution(
        proposal: RedistributionProposal,
        baseElements: List<WorkElement>,
        stations: List<Station>,
        models: List<Model>,
        mixItems: List<ModelMixItem>,
        plan: ProductionPlan
    ): RedistributionSimulationResult {
        val targetStation = stations.find { it.id == proposal.targetStationId }
            ?: throw IllegalArgumentException("Target station not found: ${proposal.targetStationId}")
        val element = baseElements.find { it.id == proposal.elementId }
            ?: throw IllegalArgumentException("Element not found: ${proposal.elementId}")

        val targetStationSeq = stations.indexOfFirst { it.id == targetStation.id } + 1
        val violations = validatePrecedenceAndConstraints(element, targetStation, targetStationSeq, baseElements, stations)

        // Baseline summary
        val baseSummary = calculateLineSummary(plan, stations, baseElements, models, mixItems)

        // Draft elements with modification applied
        val draftElements = baseElements.map { el ->
            if (el.id == proposal.elementId) {
                el.copy(
                    stationId = proposal.targetStationId,
                    operatorId = proposal.targetOperatorId ?: el.operatorId
                )
            } else el
        }

        val draftSummary = calculateLineSummary(plan, stations, draftElements, models, mixItems)

        val deltaEfficiency = draftSummary.weightedBalanceEfficiency - baseSummary.weightedBalanceEfficiency
        val deltaMaxCt = draftSummary.maxOverallCycleTime - baseSummary.maxOverallCycleTime
        val deltaCapacity = draftSummary.effectiveMixedCapacityPerHour - baseSummary.effectiveMixedCapacityPerHour

        return RedistributionSimulationResult(
            baseSummary = baseSummary,
            draftSummary = draftSummary,
            violations = violations,
            deltaWeightedBalanceEfficiency = deltaEfficiency,
            deltaMaxCycleTime = deltaMaxCt,
            deltaEffectiveCapacityPerHour = deltaCapacity,
            isFeasible = violations.isEmpty()
        )
    }

    /**
     * Analyzes the effect of production sequencing on station workload fluctuation.
     */
    fun analyzeSequence(
        sequence: ProductionSequence,
        stationMetrics: List<MixedModelStationMetrics>,
        lineTakt: Double
    ): SequenceAnalysisResult {
        val pattern = sequence.modelPattern
        val patternLength = pattern.size
        val uniqueModels = pattern.distinct()

        var maxConsecutiveOverruns = 0
        val bottleneckRisks = mutableListOf<String>()

        stationMetrics.forEach { st ->
            var currentConsecutive = 0
            var maxForStation = 0
            pattern.forEach { modelId ->
                val ct = st.modelCycleTimes[modelId] ?: 0.0
                if (ct > lineTakt) {
                    currentConsecutive++
                    if (currentConsecutive > maxForStation) maxForStation = currentConsecutive
                } else {
                    currentConsecutive = 0
                }
            }
            if (maxForStation > 1) {
                bottleneckRisks.add(st.station.name)
            }
            if (maxForStation > maxConsecutiveOverruns) {
                maxConsecutiveOverruns = maxForStation
            }
        }

        // Pitch time: Average duration per unit in pattern
        val pitchTime = if (patternLength > 0 && lineTakt > 0) lineTakt * patternLength else 0.0
        val smoothness = max(0.0, 100.0 - (maxConsecutiveOverruns * 20.0))

        return SequenceAnalysisResult(
            patternLength = patternLength,
            uniqueModels = uniqueModels,
            maxConsecutiveOverTaktCount = maxConsecutiveOverruns,
            bottleneckRiskStations = bottleneckRisks,
            pitchTimeSeconds = pitchTime,
            smoothnessScore = smoothness
        )
    }
}
