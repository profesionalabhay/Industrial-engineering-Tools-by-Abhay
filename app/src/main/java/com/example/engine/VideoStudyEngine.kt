package com.example.engine

import com.example.data.*
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object VideoStudyEngine {

    /**
     * Deterministically calculates statistics across cycles and elemental observations.
     * Guaranteed: No fabricated numbers, returns null for StdDev and CV if N < 2.
     */
    fun calculateCycleStatistics(
        cycles: List<StudyCycle>,
        candidateElements: List<AICandidateElement>
    ): CycleStatistics {
        val totalCycles = cycles.size
        val validCycles = cycles.filter { !it.isExcluded }
        val excludedCount = totalCycles - validCycles.size

        if (validCycles.isEmpty()) {
            return CycleStatistics(
                cycleCount = totalCycles,
                validCycleCount = 0,
                excludedCycleCount = excludedCount,
                averageCycleTime = 0.0,
                minCycleTime = 0.0,
                maxCycleTime = 0.0,
                medianCycleTime = 0.0,
                rangeCycleTime = 0.0,
                stdDevCycleTime = null,
                cvPercent = null,
                averageVaTime = 0.0,
                averageNnvaTime = 0.0,
                averageNvaTime = 0.0,
                vaPercent = 0.0,
                nnvaPercent = 0.0,
                nvaPercent = 0.0
            )
        }

        val durations = validCycles.map { it.duration }.sorted()
        val n = durations.size
        val avg = durations.average()
        val min = durations.first()
        val max = durations.last()
        val range = max - min

        val median = if (n % 2 == 1) {
            durations[n / 2]
        } else {
            (durations[(n / 2) - 1] + durations[n / 2]) / 2.0
        }

        val (stdDev, cv) = if (n >= 2) {
            val variance = durations.sumOf { (it - avg).pow(2) } / (n - 1)
            val sd = sqrt(variance)
            val coeff = if (avg > 0) (sd / avg) * 100.0 else 0.0
            Pair(sd, coeff)
        } else {
            Pair(null, null)
        }

        // Calculate VA / NNVA / NVA distribution across elements in valid cycles
        val activeElements = candidateElements.filter { el ->
            el.validationStatus != ValidationStatus.REJECTED &&
            validCycles.any { c -> c.cycleNumber == el.cycleNumber }
        }

        val totalVaTime = activeElements.filter { it.finalClassification == ValueClassification.VA }.sumOf { it.duration }
        val totalNnvaTime = activeElements.filter { it.finalClassification == ValueClassification.NNVA }.sumOf { it.duration }
        val totalNvaTime = activeElements.filter { it.finalClassification == ValueClassification.NVA }.sumOf { it.duration }

        val avgVa = totalVaTime / n
        val avgNnva = totalNnvaTime / n
        val avgNva = totalNvaTime / n
        val totalSum = totalVaTime + totalNnvaTime + totalNvaTime

        val vaPct = if (totalSum > 0) (totalVaTime / totalSum) * 100.0 else 0.0
        val nnvaPct = if (totalSum > 0) (totalNnvaTime / totalSum) * 100.0 else 0.0
        val nvaPct = if (totalSum > 0) (totalNvaTime / totalSum) * 100.0 else 0.0

        return CycleStatistics(
            cycleCount = totalCycles,
            validCycleCount = n,
            excludedCycleCount = excludedCount,
            averageCycleTime = avg,
            minCycleTime = min,
            maxCycleTime = max,
            medianCycleTime = median,
            rangeCycleTime = range,
            stdDevCycleTime = stdDev,
            cvPercent = cv,
            averageVaTime = avgVa,
            averageNnvaTime = avgNnva,
            averageNvaTime = avgNva,
            vaPercent = vaPct,
            nnvaPercent = nnvaPct,
            nvaPercent = nvaPct
        )
    }

    /**
     * Splits an existing AI candidate into two distinct adjacent elements at splitTime.
     * Preserves original AI suggestion for full auditability.
     */
    fun splitElement(
        element: AICandidateElement,
        splitTime: Double,
        firstName: String? = null,
        secondName: String? = null
    ): Pair<AICandidateElement, AICandidateElement> {
        require(splitTime > element.startTime && splitTime < element.endTime) {
            "Split timestamp ($splitTime) must fall strictly between start (${element.startTime}) and end (${element.endTime})"
        }

        val originalSnapshot = element.originalAiSuggestion ?: ImmutableCandidateSnapshot(
            name = element.name,
            startTime = element.startTime,
            endTime = element.endTime,
            duration = element.duration,
            activityCategory = element.activityCategory,
            classification = element.suggestedClassification,
            confidenceScore = element.confidenceScore
        )

        val part1 = element.copy(
            id = UUID.randomUUID().toString(),
            name = firstName ?: "${element.name} [Part 1]",
            startTime = element.startTime,
            endTime = splitTime,
            duration = splitTime - element.startTime,
            validationStatus = ValidationStatus.USER_EDITED,
            originalAiSuggestion = originalSnapshot,
            notes = "Split at ${String.format(Locale.US, "%.2fs", splitTime)}: Originally '${element.name}'"
        )

        val part2 = element.copy(
            id = UUID.randomUUID().toString(),
            name = secondName ?: "${element.name} [Part 2]",
            startTime = splitTime,
            endTime = element.endTime,
            duration = element.endTime - splitTime,
            validationStatus = ValidationStatus.USER_EDITED,
            originalAiSuggestion = originalSnapshot,
            notes = "Split at ${String.format(Locale.US, "%.2fs", splitTime)}: Originally '${element.name}'"
        )

        return Pair(part1, part2)
    }

    /**
     * Merges two consecutive elements into a single continuous element.
     * Retains original traceability.
     */
    fun mergeElements(
        first: AICandidateElement,
        second: AICandidateElement,
        mergedName: String? = null
    ): AICandidateElement {
        val newStart = min(first.startTime, second.startTime)
        val newEnd = max(first.endTime, second.endTime)
        val newDuration = newEnd - newStart

        val combinedTools = (first.toolNames + second.toolNames).distinct()
        val combinedMaterials = (first.materialNames + second.materialNames).distinct()

        val primaryClassification = when {
            first.finalClassification == ValueClassification.VA || second.finalClassification == ValueClassification.VA -> ValueClassification.VA
            first.finalClassification == ValueClassification.NNVA || second.finalClassification == ValueClassification.NNVA -> ValueClassification.NNVA
            else -> ValueClassification.NVA
        }

        val originalSnapshot = first.originalAiSuggestion ?: ImmutableCandidateSnapshot(
            name = first.name,
            startTime = first.startTime,
            endTime = first.endTime,
            duration = first.duration,
            activityCategory = first.activityCategory,
            classification = first.suggestedClassification,
            confidenceScore = first.confidenceScore
        )

        return first.copy(
            id = UUID.randomUUID().toString(),
            name = mergedName ?: "${first.name} + ${second.name}",
            startTime = newStart,
            endTime = newEnd,
            duration = newDuration,
            finalClassification = primaryClassification,
            validationStatus = ValidationStatus.USER_EDITED,
            originalAiSuggestion = originalSnapshot,
            toolNames = combinedTools,
            materialNames = combinedMaterials,
            notes = "Merged: '${first.name}' (${String.format(Locale.US, "%.2f", first.duration)}s) & '${second.name}' (${String.format(Locale.US, "%.2f", second.duration)}s)"
        )
    }

    /**
     * Scans candidate and validated elements to identify evidence-based IE improvement opportunities.
     */
    fun scanImprovementOpportunities(
        studyId: String,
        elements: List<AICandidateElement>,
        cycles: List<StudyCycle>,
        stats: CycleStatistics
    ): List<VideoImprovementOpportunity> {
        val opportunities = mutableListOf<VideoImprovementOpportunity>()

        // 1. Check for Excessive Walking / Motion
        val walkingElements = elements.filter {
            it.activityCategory == VideoActivityCategory.WALK ||
            it.wasteCategory == WasteCategory.MOTION ||
            it.wasteCategory == WasteCategory.TRANSPORTATION
        }
        val totalWalkingTime = walkingElements.sumOf { it.duration }
        if (totalWalkingTime > 3.0) {
            opportunities.add(
                VideoImprovementOpportunity(
                    id = "OPP-WALK-${UUID.randomUUID().toString().take(6)}",
                    studyId = studyId,
                    title = "Excessive Operator Walking / Travel",
                    patternType = "EXCESSIVE_WALKING",
                    evidence = "${walkingElements.size} walking segments detected totaling ${String.format(Locale.US, "%.1fs", totalWalkingTime)}.",
                    affectedElementIds = walkingElements.map { it.id },
                    observedCalculatedData = "Walking accounts for ${String.format(Locale.US, "%.1fs", totalWalkingTime)} across observed cycles.",
                    potentialCause = "Component racks, secondary tools, or bins are positioned outside the operator's primary reach zone.",
                    suggestedAction = "Relocate parts bins and staging tables to Golden Zone (point of use) to eliminate operator travel.",
                    risk = "Line layout footprint, ergonomic clearance, and replenishment pathway constraints.",
                    validationRequired = "Confirm physical clearance for material replenishment cart and station floor boundary."
                )
            )
        }

        // 2. Check for Searching / Hesitation
        val searchElements = elements.filter {
            it.activityCategory == VideoActivityCategory.SEARCH ||
            it.name.contains("search", ignoreCase = true) ||
            it.name.contains("find", ignoreCase = true)
        }
        if (searchElements.isNotEmpty()) {
            val totalSearch = searchElements.sumOf { it.duration }
            opportunities.add(
                VideoImprovementOpportunity(
                    id = "OPP-SEARCH-${UUID.randomUUID().toString().take(6)}",
                    studyId = studyId,
                    title = "Part / Tool Searching Waste Detected",
                    patternType = "REPEATED_SEARCHING",
                    evidence = "Identified ${searchElements.size} searching motion instances totaling ${String.format(Locale.US, "%.1fs", totalSearch)}.",
                    affectedElementIds = searchElements.map { it.id },
                    observedCalculatedData = "Search hesitation time = ${String.format(Locale.US, "%.1fs", totalSearch)}.",
                    potentialCause = "Unsorted bins, missing shadow boards, or mixed fastener containers causing operator hesitation.",
                    suggestedAction = "Implement 5S shadow boards and dedicated part gravity dividers.",
                    risk = "Operator retraining and standard work adherence during ramp-up.",
                    validationRequired = "Confirm tool tethering feasibility and fixture bin geometry."
                )
            )
        }

        // 3. Check for Long Inspection / Gaging
        val inspectionElements = elements.filter {
            it.activityCategory == VideoActivityCategory.INSPECT ||
            it.activityCategory == VideoActivityCategory.MEASURE ||
            it.name.contains("inspect", ignoreCase = true)
        }
        val totalInspection = inspectionElements.sumOf { it.duration }
        if (totalInspection > 4.0) {
            opportunities.add(
                VideoImprovementOpportunity(
                    id = "OPP-INSPECT-${UUID.randomUUID().toString().take(6)}",
                    studyId = studyId,
                    title = "Lengthy In-Cycle Inspection",
                    patternType = "LONG_INSPECTION",
                    evidence = "In-cycle quality checks consume ${String.format(Locale.US, "%.1fs", totalInspection)} per cycle.",
                    affectedElementIds = inspectionElements.map { it.id },
                    observedCalculatedData = "Inspection time = ${String.format(Locale.US, "%.1fs", totalInspection)} (${String.format(Locale.US, "%.1f%%", if (stats.averageCycleTime > 0) (totalInspection / stats.averageCycleTime) * 100.0 else 0.0)} of cycle).",
                    potentialCause = "Manual visual validation without poka-yoke fixture verification or go/no-go sensor interlocking.",
                    suggestedAction = "Introduce optical sensor or mechanical poka-yoke guide to verify seating automatically upon clamp closure.",
                    risk = "Sensor calibration maintenance and fail-safe interlocking protocols.",
                    validationRequired = "Quality engineering sign-off on automated verification capability."
                )
            )
        }

        // 4. High Cycle Variation Check (CV > 15%)
        if (stats.cvPercent != null && stats.cvPercent > 15.0) {
            opportunities.add(
                VideoImprovementOpportunity(
                    id = "OPP-VARIATION-${UUID.randomUUID().toString().take(6)}",
                    studyId = studyId,
                    title = "High Cycle-to-Cycle Workload Volatility",
                    patternType = "HIGH_VARIATION",
                    evidence = "Observed Coefficient of Variation is ${String.format(Locale.US, "%.1f%%", stats.cvPercent)} (Std Dev = ${String.format(Locale.US, "%.2fs", stats.stdDevCycleTime ?: 0.0)}).",
                    affectedElementIds = elements.map { it.id },
                    observedCalculatedData = "Range = ${String.format(Locale.US, "%.1fs", stats.rangeCycleTime)} (Min = ${String.format(Locale.US, "%.1fs", stats.minCycleTime)}, Max = ${String.format(Locale.US, "%.1fs", stats.maxCycleTime)}).",
                    potentialCause = "Inconsistent manual presentation, variable part fit tolerances, or lack of standardized work sequence.",
                    suggestedAction = "Establish standard work combination sheet and verify consistent parts presentation tolerances.",
                    risk = "Over-constraining operator movement without root-cause part tolerance control.",
                    validationRequired = "Conduct multi-operator capability verification across consecutive shifts."
                )
            )
        }

        // 5. High Non-Value-Added Time (> 20%)
        if (stats.nvaPercent > 20.0) {
            opportunities.add(
                VideoImprovementOpportunity(
                    id = "OPP-NVA-${UUID.randomUUID().toString().take(6)}",
                    studyId = studyId,
                    title = "High Proportion of Non-Value-Added Time",
                    patternType = "HIGH_NVA",
                    evidence = "Non-Value Added (Waste) time is ${String.format(Locale.US, "%.1f%%", stats.nvaPercent)} of the validated cycle (${String.format(Locale.US, "%.1fs", stats.averageNvaTime)}).",
                    affectedElementIds = elements.filter { it.finalClassification == ValueClassification.NVA }.map { it.id },
                    observedCalculatedData = "NVA time = ${String.format(Locale.US, "%.1fs", stats.averageNvaTime)} per cycle.",
                    potentialCause = "Accumulation of unnecessary handling, waiting, or reorientation motions.",
                    suggestedAction = "Target NVA elements for Kaizen elimination or combine sequential handling steps.",
                    risk = "Ensure necessary support motions (NNVA) are not inadvertently classified as pure waste.",
                    validationRequired = "Line supervisor and IE engineer joint Kaizen review."
                )
            )
        }

        return opportunities
    }

    /**
     * Synthesizes a strict 7-section Industrial Engineering Video Study Diagnostic Report.
     */
    fun generateAiReport(
        metadata: VideoStudyMetadata,
        elements: List<AICandidateElement>,
        cycles: List<StudyCycle>,
        stats: CycleStatistics,
        opportunities: List<VideoImprovementOpportunity>
    ): VideoStudyAiReport {
        val observedFacts = mutableListOf<String>()
        val calculatedResults = mutableListOf<String>()
        val potentialImprovements = mutableListOf<String>()
        val risksAndConstraints = mutableListOf<String>()
        val assumptions = mutableListOf<String>()
        val validationRequired = mutableListOf<String>()

        // 1. OBSERVED FACTS
        observedFacts.add("Video Study: '${metadata.name}' for Model '${metadata.modelId}' (${metadata.variant}) at Station '${metadata.stationId}'.")
        observedFacts.add("Operator: '${metadata.operatorId}' under '${metadata.shift}' on date '${metadata.date}'.")
        observedFacts.add("Recorded Footage: ${String.format(Locale.US, "%.1fs", metadata.videoDurationSeconds)} total runtime across ${cycles.size} observable cycles (${stats.validCycleCount} valid, ${stats.excludedCycleCount} excluded).")
        val validatedCount = elements.count { it.validationStatus == ValidationStatus.USER_VALIDATED || it.validationStatus == ValidationStatus.USER_EDITED }
        val aiSuggestedCount = elements.count { it.validationStatus == ValidationStatus.AI_SUGGESTED }
        observedFacts.add("Candidate Work Elements: ${elements.size} total ($validatedCount validated by IE, $aiSuggestedCount pending human validation).")
        val abnormalCount = elements.count { it.isAbnormal }
        if (abnormalCount > 0) {
            observedFacts.add("Abnormal Events: $abnormalCount elements flagged as non-standard interruptions.")
        }

        // 2. CALCULATED RESULTS
        calculatedResults.add("Average Cycle Time: ${String.format(Locale.US, "%.2fs", stats.averageCycleTime)} (Min = ${String.format(Locale.US, "%.2fs", stats.minCycleTime)}, Max = ${String.format(Locale.US, "%.2fs", stats.maxCycleTime)}, Range = ${String.format(Locale.US, "%.2fs", stats.rangeCycleTime)}).")
        if (stats.stdDevCycleTime != null && stats.cvPercent != null) {
            calculatedResults.add("Cycle Variation: Standard Deviation = ${String.format(Locale.US, "%.2fs", stats.stdDevCycleTime)}, Coefficient of Variation = ${String.format(Locale.US, "%.1f%%", stats.cvPercent)}.")
        } else {
            calculatedResults.add("Cycle Variation: Insufficient valid observations (N = ${stats.validCycleCount} < 2) for statistical sample variance calculation.")
        }
        calculatedResults.add("Value-Stream Breakdown: VA = ${String.format(Locale.US, "%.1f%%", stats.vaPercent)} (${String.format(Locale.US, "%.2fs", stats.averageVaTime)}), NNVA = ${String.format(Locale.US, "%.1f%%", stats.nnvaPercent)} (${String.format(Locale.US, "%.2fs", stats.averageNnvaTime)}), NVA = ${String.format(Locale.US, "%.1f%%", stats.nvaPercent)} (${String.format(Locale.US, "%.2fs", stats.averageNvaTime)}).")
        val taktComparison = if (metadata.expectedTaktSeconds > 0) {
            val delta = stats.averageCycleTime - metadata.expectedTaktSeconds
            if (delta > 0) "exceeds Takt by ${String.format(Locale.US, "%.2fs", delta)} (Deficit)" else "within Takt by ${String.format(Locale.US, "%.2fs", -delta)} (Buffer)"
        } else "Takt unassigned"
        calculatedResults.add("Takt Performance: Target Takt = ${String.format(Locale.US, "%.1fs", metadata.expectedTaktSeconds)} — Observed Cycle is $taktComparison.")

        // 3. ENGINEERING INTERPRETATION
        val engineeringInterpretation = buildString {
            if (metadata.expectedTaktSeconds > 0 && stats.averageCycleTime > metadata.expectedTaktSeconds) {
                append("TAKT DEFICIT OBSERVED: Validated station cycle time (${String.format(Locale.US, "%.2fs", stats.averageCycleTime)}) exceeds target Takt (${String.format(Locale.US, "%.1fs", metadata.expectedTaktSeconds)}). ")
            } else {
                append("STABLE CYCLE TIME: Average cycle time is within target Takt. ")
            }
            if (stats.nvaPercent > 20.0) {
                append("Significant waste concentration detected (${String.format(Locale.US, "%.1f%%", stats.nvaPercent)} NVA), primarily consisting of avoidable operator handling and walking. ")
            }
            if (stats.cvPercent != null && stats.cvPercent > 15.0) {
                append("High cycle-to-cycle inconsistency (${String.format(Locale.US, "%.1f%%", stats.cvPercent)} CV) indicates variable process execution or lack of standard work stabilization.")
            } else {
                append("Cycle pace shows repeatable execution across observed cycles.")
            }
        }

        // 4. POTENTIAL IMPROVEMENTS
        opportunities.forEach { opp ->
            potentialImprovements.add("${opp.title}: ${opp.suggestedAction} (Potential: ${opp.observedCalculatedData})")
        }
        if (potentialImprovements.isEmpty()) {
            potentialImprovements.add("Process is well-balanced. Continue monitoring operator pacing and standard work compliance.")
        }

        // 5. RISKS / CONSTRAINTS
        risksAndConstraints.add("Camera perspective and partial occlusions may obscure micro-grasp or fastener alignment details.")
        risksAndConstraints.add("Single operator observation: Operator pacing and technique may not reflect general multi-shift population capability.")
        risksAndConstraints.add("Mechanical tooling and station boundary constraints must prevent unauthorized element transfers.")

        // 6. ASSUMPTIONS
        assumptions.add("Observed footage represents nominal operating conditions without deliberate slowdowns or artificial acceleration.")
        assumptions.add("Video frame rate (30 fps) provides ±0.033s boundary resolution.")
        assumptions.add("Standard allowance factor of 10% assumed for baseline standard time calculation until formal rating study is approved.")

        // 7. VALIDATION REQUIRED
        validationRequired.add("Mandatory human Industrial Engineer review of all AI-suggested timestamps and VA classifications prior to committing to master database.")
        validationRequired.add("Confirm whether abnormal event cycles are isolated occurrences or systematic line stoppages.")
        validationRequired.add("Validate tooling and fixture compatibility with station supervisor prior to implementing redistribution proposals.")

        return VideoStudyAiReport(
            observedFacts = observedFacts,
            calculatedResults = calculatedResults,
            engineeringInterpretation = engineeringInterpretation,
            potentialImprovements = potentialImprovements,
            risksAndConstraints = risksAndConstraints,
            assumptions = assumptions,
            validationRequired = validationRequired
        )
    }
}
