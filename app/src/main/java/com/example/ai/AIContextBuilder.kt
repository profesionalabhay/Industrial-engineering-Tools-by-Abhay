package com.example.ai

import com.example.data.*
import java.util.Locale

class AIContextBuilder {

    fun buildProjectContext(
        project: Project,
        models: List<Model>,
        stations: List<Station>,
        elements: List<WorkElement>,
        plan: ProductionPlan?,
        mixItems: List<ModelMixItem>
    ): String {
        val sb = StringBuilder()
        sb.append("### PROJECT CONTEXT: ${project.name}\n")
        sb.append("Description: ${project.description}\n")
        sb.append("Status: ${project.status}\n\n")

        sb.append("#### MODELS & DEMAND\n")
        models.forEach { model ->
            val mix = mixItems.find { it.modelId == model.id }
            sb.append("- ${model.name}: Demand ${model.demand}, Mix: ${String.format(Locale.US, "%.1f", mix?.effectiveMixPercentage ?: 0.0)}%\n")
        }
        sb.append("\n")

        if (plan != null) {
            sb.append("#### PRODUCTION PLAN\n")
            sb.append("- Takt Time: ${String.format(Locale.US, "%.2f", plan.requiredTaktSeconds)}s\n")
            sb.append("- Planned Units: ${plan.plannedTotalQuantity}\n")
            sb.append("- Operator Count: ${plan.operatorCount}\n\n")
        }

        sb.append("#### STATION STATUS\n")
        stations.forEach { station ->
            val stationElements = elements.filter { it.stationId == station.id }
            val totalTime = stationElements.sumOf { it.standardTime }
            val vaTime = stationElements.filter { it.valueClassification == ValueClassification.VA }.sumOf { it.standardTime }
            val nvaTime = stationElements.filter { it.valueClassification == ValueClassification.NVA }.sumOf { it.standardTime }
            
            sb.append("- ${station.name}: Total CT ${String.format(Locale.US, "%.2f", totalTime)}s ")
            sb.append("(VA: ${String.format(Locale.US, "%.1f", if (totalTime > 0) (vaTime/totalTime)*100 else 0.0)}%, ")
            sb.append("NVA: ${String.format(Locale.US, "%.1f", if (totalTime > 0) (nvaTime/totalTime)*100 else 0.0)}%)\n")
            
            stationElements.take(5).forEach { el ->
                sb.append("  * [${el.valueClassification}] ${el.name} (${el.standardTime}s)\n")
            }
            if (stationElements.size > 5) sb.append("  * ... and ${stationElements.size - 5} more elements\n")
        }

        return sb.toString()
    }

    fun buildOpExContext(
        oee: List<OeeMetrics>,
        losses: List<LossEvent>,
        rcas: List<RcaRecord>,
        kaizens: List<KaizenRecord>
    ): String {
        val sb = StringBuilder()
        sb.append("### OPERATIONAL EXCELLENCE CONTEXT\n")
        
        if (oee.isNotEmpty()) {
            val avgOee = oee.map { it.oee }.average()
            sb.append("#### OEE STATUS\n")
            sb.append("- Current OEE: ${String.format(Locale.US, "%.1f", avgOee * 100)}%\n")
            oee.take(1).forEach { m ->
                sb.append("  * Availability: ${String.format(Locale.US, "%.1f", m.availability * 100)}%\n")
                sb.append("  * Performance: ${String.format(Locale.US, "%.1f", m.performance * 100)}%\n")
                sb.append("  * Quality: ${String.format(Locale.US, "%.1f", m.quality * 100)}%\n")
            }
        }

        if (losses.isNotEmpty()) {
            sb.append("\n#### TOP LOSSES\n")
            losses.groupBy { it.reason }.toList()
                .sortedByDescending { it.second.sumOf { l -> l.durationMinutes } }
                .take(3)
                .forEach { (reason, events) ->
                    sb.append("- $reason: ${events.sumOf { it.durationMinutes }.toInt()} min total\n")
                }
        }

        if (rcas.isNotEmpty()) {
            sb.append("\n#### ACTIVE RCA STUDIES\n")
            rcas.filter { it.status != RcaStatus.CLOSED }.take(3).forEach { rca ->
                sb.append("- [${rca.status}] ${rca.problemStatement}\n")
                sb.append("  * Root Cause: ${rca.rootCause ?: "Analyzing..."}\n")
            }
        }

        if (kaizens.isNotEmpty()) {
            sb.append("\n#### KAIZEN PROGRESS\n")
            kaizens.take(3).forEach { k ->
                sb.append("- [${k.status}] ${k.title} (Owner: ${k.owner})\n")
            }
        }

        return sb.toString()
    }

    fun buildVideoEvidenceContext(
        study: VideoStudyMetadata,
        elements: List<AICandidateElement>,
        stats: CycleStatistics
    ): String {
        val sb = StringBuilder()
        sb.append("### VIDEO STUDY EVIDENCE: ${study.name}\n")
        sb.append("Station: ${study.stationId}, Video: ${study.videoFileName}\n\n")
        
        sb.append("#### CYCLE STATISTICS\n")
        sb.append("- Avg Cycle Time: ${String.format(Locale.US, "%.2f", stats.averageCycleTime)}s\n")
        sb.append("- CV%: ${String.format(Locale.US, "%.1f", stats.cvPercent ?: 0.0)}%\n")
        sb.append("- VA%: ${String.format(Locale.US, "%.1f", stats.vaPercent)}%\n\n")

        sb.append("#### OBSERVED ACTIVITIES (SAMPLE)\n")
        elements.filter { it.isAbnormal || it.confidenceScore < 0.7 }.take(10).forEach { el ->
            sb.append("- [${el.activityCategory}] ${el.name}: ${el.duration}s (Confidence: ${String.format(Locale.US, "%.2f", el.confidenceScore)})")
            if (el.isAbnormal) sb.append(" **ABNORMAL: ${el.abnormalEventReason}**")
            sb.append("\n")
        }

        return sb.toString()
    }

    fun buildEnterpriseContext(
        enterpriseKpis: List<EnterpriseKpi>,
        simResults: List<SimulationResult>,
        productivity: List<ProductivityRecord>,
        savings: List<SavingsRecord>,
        benefits: List<ImprovementBenefit>
    ): String {
        val sb = StringBuilder()
        sb.append("### ENTERPRISE IE CONTEXT\n")

        if (enterpriseKpis.isNotEmpty()) {
            sb.append("#### PLANT-WIDE BENCHMARKS\n")
            enterpriseKpis.forEach { kpi ->
                sb.append("- [${kpi.entityType}: ${kpi.entityId}] ${kpi.metric}: ${String.format(Locale.US, "%.1f", kpi.value * 100)}%")
                kpi.target?.let { t -> sb.append(" (Target: ${String.format(Locale.US, "%.1f", t * 100)}%)") }
                sb.append("\n")
            }
        }

        if (simResults.isNotEmpty()) {
            sb.append("\n#### SIMULATION RESULTS\n")
            simResults.take(2).forEach { sim ->
                sb.append("- Scenario: ${sim.scenarioName}\n")
                sb.append("  * Throughput: ${String.format(Locale.US, "%.1f", sim.throughputPerHour)} u/h\n")
                sb.append("  * Bottleneck: ${sim.bottleneckStationId}\n")
                sb.append("  * Lead Time: ${String.format(Locale.US, "%.1f", sim.leadTimeMinutes)} min\n")
            }
        }

        if (benefits.isNotEmpty()) {
            sb.append("\n#### REALIZED SAVINGS & BENEFITS\n")
            benefits.take(5).forEach { b ->
                sb.append("- ${b.description}: ${b.value} ${b.unit} (${b.type})\n")
            }
        }

        return sb.toString()
    }

    fun buildSystemInstruction(): String {
        return """
            You are the IE COPILOT, an expert Industrial Engineering AI Assistant.
            Your goal is to help engineers analyze workstation performance, identify waste, and optimize production lines.
            
            STRICT RULES:
            1. NEVER invent numbers. Use only the data provided in the context.
            2. Distinguish between OBSERVED data (from video/time study) and CALCULATED results.
            3. Use Industrial Engineering terminology (Takt, Cycle Time, VA/NVA, Yamazumi, etc.).
            4. If data is missing for a calculation, say "INSUFFICIENT DATA" and list what is needed.
            5. Every substantive analysis MUST follow this structure:
               - OBSERVED FACTS
               - CALCULATED RESULTS
               - ENGINEERING INTERPRETATION
               - POSSIBLE ROOT CAUSES
               - RECOMMENDATION
               - EXPECTED IMPACT
               - RISKS / CONSTRAINTS
               - ASSUMPTIONS
               - VALIDATION REQUIRED
            
            You have access to deterministic IE engines for calculations. Trust their results over your own estimates.
        """.trimIndent()
    }
}
