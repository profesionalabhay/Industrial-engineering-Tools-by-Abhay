package com.example.engine

import com.example.data.*
import java.util.Locale

object MixedModelAiAdvisor {

    /**
     * Synthesizes an Industrial Engineering AI diagnostic report from deterministic calculations.
     * Guaranteed schema compliance across all 7 mandatory sections.
     */
    fun generateLineDiagnosis(
        plan: ProductionPlan,
        summary: MixedModelLineSummary,
        mixItems: List<ModelMixItem>,
        sequence: ProductionSequence?,
        elements: List<WorkElement>
    ): MultiModelAiDiagnosis {
        val observedFacts = mutableListOf<String>()
        val calculatedResults = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val risksAndConstraints = mutableListOf<String>()
        val assumptions = mutableListOf<String>()
        val validationRequired = mutableListOf<String>()

        // 1. OBSERVED FACTS
        observedFacts.add("Active Production Plan: '${plan.name}' with ${plan.plannedTotalQuantity} total units across ${mixItems.size} models.")
        observedFacts.add("Line Configuration: ${summary.stationCount} physical stations staffed by ${plan.operatorCount} operators under '${plan.shiftName}'.")
        mixItems.forEach { item ->
            observedFacts.add("Model '${item.modelName}' (${item.variant}): Demand = ${item.plannedQuantity} pcs, Mix = ${String.format(Locale.US, "%.1f%%", item.effectiveMixPercentage)}.")
        }
        val commonElementCount = elements.count { it.applicability == ElementApplicability.COMMON }
        val modelSpecificCount = elements.count { it.applicability != ElementApplicability.COMMON }
        observedFacts.add("Work Breakdown: $commonElementCount Common elements, $modelSpecificCount Model-specific elements.")

        // 2. CALCULATED RESULTS
        calculatedResults.add("Line Takt Time: ${String.format(Locale.US, "%.1f", summary.lineTakt)}s (Available operating window: ${String.format(Locale.US, "%.0f", summary.plannedOperatingSeconds)}s).")
        calculatedResults.add("Weighted Line Balance Efficiency: ${String.format(Locale.US, "%.1f%%", summary.weightedBalanceEfficiency)} (Balance Loss: ${String.format(Locale.US, "%.1f%%", summary.weightedLineBalanceLoss)}).")
        summary.modelBalanceEfficiencies.forEach { (mId, eff) ->
            val mName = mixItems.find { it.modelId == mId }?.modelName ?: mId
            calculatedResults.add("Model '$mName' Balance Efficiency: ${String.format(Locale.US, "%.1f%%", eff)}.")
        }
        val wbStation = summary.weightedBottleneckStation?.name ?: "None"
        calculatedResults.add("Weighted Bottleneck Station: $wbStation at ${String.format(Locale.US, "%.1f", summary.weightedBottleneckCycleTime)}s.")
        val mbStation = summary.maxBottleneckStation?.name ?: "None"
        calculatedResults.add("Maximum Workload Peak: $mbStation at ${String.format(Locale.US, "%.1f", summary.maxOverallCycleTime)}s.")
        calculatedResults.add("Effective Mixed Throughput: ${String.format(Locale.US, "%.1f", summary.effectiveMixedCapacityPerHour)} units/hour (Headroom: ${String.format(Locale.US, "%.1f%%", summary.capacityHeadroomPercent)}).")

        // 3. ENGINEERING INTERPRETATION
        val isOverTakt = summary.weightedBottleneckCycleTime > summary.lineTakt
        val hasMaxSpike = summary.maxOverallCycleTime > summary.lineTakt && !isOverTakt
        val interpretation = buildString {
            if (isOverTakt) {
                append("CRITICAL LINE DEFICIT: Weighted bottleneck station '$wbStation' (${String.format(Locale.US, "%.1f", summary.weightedBottleneckCycleTime)}s) exceeds line Takt (${String.format(Locale.US, "%.1f", summary.lineTakt)}s). The line cannot fulfill the target volume of ${plan.plannedTotalQuantity} units within standard shift hours without overtime or work redistribution.")
            } else if (hasMaxSpike) {
                append("MODEL MIX VOLATILITY DETECTED: While weighted average cycle time (${String.format(Locale.US, "%.1f", summary.weightedBottleneckCycleTime)}s) complies with Takt, specific high-content models cause station cycle time to peak at ${String.format(Locale.US, "%.1f", summary.maxOverallCycleTime)}s at '$mbStation'. A consecutive run of high-spec units will create buffer starvation or line stoppage.")
            } else {
                append("STABLE MIXED-MODEL EQUILIBRIUM: All stations operate within Takt (${String.format(Locale.US, "%.1f", summary.lineTakt)}s) under weighted loading. Total line efficiency is ${String.format(Locale.US, "%.1f%%", summary.weightedBalanceEfficiency)} with capacity headroom of ${String.format(Locale.US, "%.1f%%", summary.capacityHeadroomPercent)}.")
            }
        }

        // 4. RECOMMENDATION
        if (isOverTakt || summary.weightedBalanceEfficiency < 85.0) {
            recommendations.add("Transfer transferable work elements from bottleneck '$wbStation' to adjacent stations with high idle allowance.")
            recommendations.add("Target Non-Value-Added (NVA) elements on '$wbStation' for Kaizen elimination to reclaim cycle time without capital expenditure.")
        }
        if (sequence != null) {
            recommendations.add("Enforce Heijunka leveled sequencing (${sequence.modelPattern.joinToString(" → ")}) to alternate high-workload models with lower-content base units.")
        }
        recommendations.add("Establish a 2-unit buffer stock directly upstream of '${summary.maxBottleneckStation?.name ?: "Bottleneck"}' to dampen line wave shocks.")

        // 5. RISKS / CONSTRAINTS
        risksAndConstraints.add("Precedence Dependencies: Work elements with preceding mechanical alignments cannot be transferred upstream.")
        risksAndConstraints.add("Tooling Station Constraints: Pneumatic torque tools and test fixtures are fixed to specific physical stations.")
        risksAndConstraints.add("Unscheduled Batching: If production operators run high-spec models in unmonitored clusters instead of the specified sequence, downstream buffer overflows will occur.")

        // 6. ASSUMPTIONS
        assumptions.add("Constant production mix: Assumes customer order pull adheres to the stated percentages without intraday surges.")
        assumptions.add("Standard operator performance: Assumes operators perform at 100% standard rating with approved fatigue allowances.")
        assumptions.add("Zero unrecorded micro-stoppages: Calculations assume mechanical availability without unscheduled breakdowns.")

        // 7. VALIDATION REQUIRED
        validationRequired.add("Verify operator cross-training matrix before moving model-specific elements to adjacent stations.")
        validationRequired.add("Physically measure line clearance and part bin delivery envelope at target transfer stations.")
        validationRequired.add("Audit part kitting readiness for mixed-model sequence tracking at line ingress.")

        return MultiModelAiDiagnosis(
            observedFacts = observedFacts,
            calculatedResults = calculatedResults,
            engineeringInterpretation = interpretation,
            recommendations = recommendations,
            risksAndConstraints = risksAndConstraints,
            assumptions = assumptions,
            validationRequired = validationRequired
        )
    }
}
