package com.example

import com.example.data.*
import com.example.engine.VideoStudyEngine
import org.junit.Assert.*
import org.junit.Test

class VideoStudyEngineTest {

    @Test
    fun testCycleStatisticsCalculation() {
        val cycles = listOf(
            StudyCycle("CYC-1", "VS-001", 1, 0.0, 20.0, 20.0, isNormal = true, isExcluded = false),
            StudyCycle("CYC-2", "VS-001", 2, 20.0, 42.0, 22.0, isNormal = true, isExcluded = false),
            StudyCycle("CYC-3", "VS-001", 3, 42.0, 66.0, 24.0, isNormal = true, isExcluded = false),
            StudyCycle("CYC-4", "VS-001", 4, 66.0, 100.0, 34.0, isNormal = false, isExcluded = true, exclusionReason = "Machine Jam")
        )

        val elements = listOf(
            AICandidateElement(
                id = "C-1", studyId = "VS-001", cycleNumber = 1, sequence = 10,
                name = "Assemble bracket", startTime = 0.0, endTime = 10.0, duration = 10.0,
                activityCategory = VideoActivityCategory.ASSEMBLE,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                confidenceScore = 0.95, validationStatus = ValidationStatus.USER_VALIDATED
            ),
            AICandidateElement(
                id = "C-2", studyId = "VS-001", cycleNumber = 1, sequence = 20,
                name = "Check fit", startTime = 10.0, endTime = 15.0, duration = 5.0,
                activityCategory = VideoActivityCategory.INSPECT,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                confidenceScore = 0.88, validationStatus = ValidationStatus.USER_VALIDATED
            ),
            AICandidateElement(
                id = "C-3", studyId = "VS-001", cycleNumber = 1, sequence = 30,
                name = "Walking to bin", startTime = 15.0, endTime = 20.0, duration = 5.0,
                activityCategory = VideoActivityCategory.WALK,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.90, validationStatus = ValidationStatus.USER_VALIDATED
            )
        )

        val stats = VideoStudyEngine.calculateCycleStatistics(cycles, elements)

        assertEquals(4, stats.cycleCount)
        assertEquals(3, stats.validCycleCount)
        assertEquals(1, stats.excludedCycleCount)
        // Average of 20, 22, 24 = 22.0
        assertEquals(22.0, stats.averageCycleTime, 0.001)
        assertEquals(20.0, stats.minCycleTime, 0.001)
        assertEquals(24.0, stats.maxCycleTime, 0.001)
        assertEquals(22.0, stats.medianCycleTime, 0.001)
        assertEquals(4.0, stats.rangeCycleTime, 0.001)

        // StdDev of [20, 22, 24]: mean=22, variance = ((20-22)^2 + (22-22)^2 + (24-22)^2) / 2 = (4 + 0 + 4)/2 = 4.0, sqrt=2.0
        assertNotNull(stats.stdDevCycleTime)
        assertEquals(2.0, stats.stdDevCycleTime!!, 0.001)
        assertNotNull(stats.cvPercent)
        assertEquals((2.0 / 22.0) * 100.0, stats.cvPercent!!, 0.01)

        // VA = 10s (50%), NNVA = 5s (25%), NVA = 5s (25%)
        assertEquals(50.0, stats.vaPercent, 0.01)
        assertEquals(25.0, stats.nnvaPercent, 0.01)
        assertEquals(25.0, stats.nvaPercent, 0.01)
    }

    @Test
    fun testInsufficientObservationsStdDevIsNull() {
        val singleCycle = listOf(
            StudyCycle("CYC-1", "VS-001", 1, 0.0, 25.0, 25.0, isNormal = true, isExcluded = false)
        )
        val stats = VideoStudyEngine.calculateCycleStatistics(singleCycle, emptyList())

        assertEquals(1, stats.validCycleCount)
        assertNull("StdDev must be null when valid cycles N < 2", stats.stdDevCycleTime)
        assertNull("CV% must be null when valid cycles N < 2", stats.cvPercent)
    }

    @Test
    fun testSplitElement() {
        val original = AICandidateElement(
            id = "ORIG-1", studyId = "VS-001", cycleNumber = 1, sequence = 10,
            name = "Retrieve part and install", startTime = 10.0, endTime = 20.0, duration = 10.0,
            activityCategory = VideoActivityCategory.ASSEMBLE,
            suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
            confidenceScore = 0.90, validationStatus = ValidationStatus.AI_SUGGESTED
        )

        val (part1, part2) = VideoStudyEngine.splitElement(original, 14.0, "Retrieve part", "Install part")

        assertEquals("Retrieve part", part1.name)
        assertEquals(10.0, part1.startTime, 0.001)
        assertEquals(14.0, part1.endTime, 0.001)
        assertEquals(4.0, part1.duration, 0.001)
        assertEquals(ValidationStatus.USER_EDITED, part1.validationStatus)
        assertNotNull(part1.originalAiSuggestion)
        assertEquals(original.name, part1.originalAiSuggestion?.name)

        assertEquals("Install part", part2.name)
        assertEquals(14.0, part2.startTime, 0.001)
        assertEquals(20.0, part2.endTime, 0.001)
        assertEquals(6.0, part2.duration, 0.001)
        assertEquals(ValidationStatus.USER_EDITED, part2.validationStatus)
    }

    @Test
    fun testMergeElements() {
        val el1 = AICandidateElement(
            id = "E1", studyId = "VS-001", cycleNumber = 1, sequence = 10,
            name = "Grasp bolt", startTime = 2.0, endTime = 4.0, duration = 2.0,
            activityCategory = VideoActivityCategory.PICK,
            suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
            confidenceScore = 0.92, validationStatus = ValidationStatus.USER_VALIDATED,
            materialNames = listOf("M4 Bolt")
        )

        val el2 = AICandidateElement(
            id = "E2", studyId = "VS-001", cycleNumber = 1, sequence = 20,
            name = "Insert and tighten bolt", startTime = 4.0, endTime = 9.0, duration = 5.0,
            activityCategory = VideoActivityCategory.FASTEN,
            suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
            confidenceScore = 0.96, validationStatus = ValidationStatus.USER_VALIDATED,
            toolNames = listOf("Torque Gun")
        )

        val merged = VideoStudyEngine.mergeElements(el1, el2, "Fasten M4 bolt assembly")

        assertEquals("Fasten M4 bolt assembly", merged.name)
        assertEquals(2.0, merged.startTime, 0.001)
        assertEquals(9.0, merged.endTime, 0.001)
        assertEquals(7.0, merged.duration, 0.001)
        assertEquals(ValueClassification.VA, merged.finalClassification)
        assertTrue(merged.toolNames.contains("Torque Gun"))
        assertTrue(merged.materialNames.contains("M4 Bolt"))
        assertEquals(ValidationStatus.USER_EDITED, merged.validationStatus)
    }

    @Test
    fun testAiFindingsAndReportGeneration() {
        val metadata = VideoStudyMetadata(
            id = "VS-TEST", projectId = "P-001", name = "Test Study", modelId = "MDL-1",
            videoFileName = "test.mp4", videoDurationSeconds = 60.0, expectedTaktSeconds = 25.0
        )
        val cycles = listOf(
            StudyCycle("C1", "VS-TEST", 1, 0.0, 24.0, 24.0),
            StudyCycle("C2", "VS-TEST", 2, 24.0, 50.0, 26.0)
        )
        val elements = listOf(
            AICandidateElement(
                id = "C-W1", studyId = "VS-TEST", cycleNumber = 1, sequence = 10,
                name = "Operator walks to far rack", startTime = 0.0, endTime = 5.0, duration = 5.0,
                activityCategory = VideoActivityCategory.WALK,
                wasteCategory = WasteCategory.MOTION,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA
            ),
            AICandidateElement(
                id = "C-W2", studyId = "VS-TEST", cycleNumber = 2, sequence = 10,
                name = "Operator walks to far rack", startTime = 24.0, endTime = 29.0, duration = 5.0,
                activityCategory = VideoActivityCategory.WALK,
                wasteCategory = WasteCategory.MOTION,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA
            )
        )

        val stats = VideoStudyEngine.calculateCycleStatistics(cycles, elements)
        val opps = VideoStudyEngine.scanImprovementOpportunities("VS-TEST", elements, cycles, stats)
        val report = VideoStudyEngine.generateAiReport(metadata, elements, cycles, stats, opps)

        // Validate 7 mandatory sections exist and are populated
        assertTrue("Observed facts must not be empty", report.observedFacts.isNotEmpty())
        assertTrue("Calculated results must not be empty", report.calculatedResults.isNotEmpty())
        assertTrue("Engineering interpretation must not be blank", report.engineeringInterpretation.isNotBlank())
        assertTrue("Potential improvements must not be empty", report.potentialImprovements.isNotEmpty())
        assertTrue("Risks and constraints must not be empty", report.risksAndConstraints.isNotEmpty())
        assertTrue("Assumptions must not be empty", report.assumptions.isNotEmpty())
        assertTrue("Validation required must not be empty", report.validationRequired.isNotEmpty())

        // Validate walking opportunity was detected
        val walkingOpp = opps.find { it.patternType == "EXCESSIVE_WALKING" }
        assertNotNull("Excessive walking pattern must be detected", walkingOpp)
    }
}
