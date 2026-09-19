package com.example

import com.example.data.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

class MixedModelCalculationEngineTest {

    @Test
    fun testProductionPlanCalculation() {
        val repo = ManufacturingRepository.getInstance()
        val plan = repo.getProductionPlanForProject("P-001")
        val models = repo.models.filter { it.projectId == "P-001" }
        val stations = repo.stations
        val elements = repo.workElements.filter { it.projectId == "P-001" }
        val mixItems = repo.getModelMixForPlan(plan.id)

        assertNotNull("Production plan should exist for P-001", plan)
        assertTrue("Plan should have planned operating seconds > 0", plan.plannedOperatingSeconds > 0)
        assertEquals("Planned total quantity should be 1000", 1000, plan.plannedTotalQuantity)

        val summary = MixedModelCalculationEngine.calculateLineSummary(
            plan = plan,
            stations = stations,
            elements = elements,
            models = models,
            mixItems = mixItems
        )

        // Verify summary calculations
        assertTrue("Line takt must be positive", summary.lineTakt > 0)
        assertTrue("Weighted balance efficiency must be positive", summary.weightedBalanceEfficiency > 0)
        assertTrue("Weighted balance efficiency must be <= 100", summary.weightedBalanceEfficiency <= 100.0)
        assertEquals(
            "Weighted balance efficiency + line loss must equal 100",
            100.0,
            summary.weightedBalanceEfficiency + summary.weightedLineBalanceLoss,
            0.01
        )
        assertNotNull("Weighted bottleneck station must exist", summary.weightedBottleneckStation)
        assertTrue("Station metrics count should equal station count", summary.stationMetrics.size == stations.size)
    }

    @Test
    fun testStationMetricsWeightedAndMax() {
        val repo = ManufacturingRepository.getInstance()
        val plan = repo.getProductionPlanForProject("P-001")
        val models = repo.models.filter { it.projectId == "P-001" }
        val stations = repo.stations
        val elements = repo.workElements.filter { it.projectId == "P-001" }
        val mixItems = repo.getModelMixForPlan(plan.id)

        val summary = MixedModelCalculationEngine.calculateLineSummary(
            plan = plan,
            stations = stations,
            elements = elements,
            models = models,
            mixItems = mixItems
        )

        summary.stationMetrics.forEach { stMetric: MixedModelStationMetrics ->
            assertTrue("Weighted cycle time >= 0", stMetric.weightedCycleTime >= 0)
            assertTrue("Max cycle time >= weighted cycle time", stMetric.maxCycleTime >= stMetric.weightedCycleTime - 0.001)
            assertTrue("Utilization >= 0", stMetric.weightedUtilization >= 0)
        }
    }

    @Test
    fun testSequenceAnalysisAndSmoothing() {
        val repo = ManufacturingRepository.getInstance()
        val plan = repo.getProductionPlanForProject("P-001")
        val models = repo.models.filter { it.projectId == "P-001" }
        val stations = repo.stations
        val elements = repo.workElements.filter { it.projectId == "P-001" }
        val mixItems = repo.getModelMixForPlan(plan.id)
        val sequence = repo.getProductionSequencesForPlan(plan.id).firstOrNull()

        assertNotNull("Production sequence should be configured", sequence)

        val summary = MixedModelCalculationEngine.calculateLineSummary(
            plan = plan,
            stations = stations,
            elements = elements,
            models = models,
            mixItems = mixItems
        )

        val analysis = MixedModelCalculationEngine.analyzeSequence(
            sequence = sequence!!,
            stationMetrics = summary.stationMetrics,
            lineTakt = summary.lineTakt
        )

        assertTrue("Pattern length should be > 0", analysis.patternLength > 0)
        assertTrue("Smoothness score should be between 0 and 100", analysis.smoothnessScore in 0.0..100.0)
        assertTrue("Max consecutive over-takt count >= 0", analysis.maxConsecutiveOverTaktCount >= 0)
    }

    @Test
    fun testRedistributionSimulationAndConstraintValidation() {
        val repo = ManufacturingRepository.getInstance()
        val plan = repo.getProductionPlanForProject("P-001")
        val models = repo.models.filter { it.projectId == "P-001" }
        val stations = repo.stations
        val elements = repo.workElements.filter { it.projectId == "P-001" }
        val mixItems = repo.getModelMixForPlan(plan.id)

        val elementToMove = elements.first()
        val targetStation = stations.last()

        val proposal = RedistributionProposal(
            elementId = elementToMove.id,
            changeType = RedistributionChangeType.MOVE_STATION,
            sourceStationId = elementToMove.stationId,
            targetStationId = targetStation.id,
            targetOperatorId = null
        )

        val simResult = MixedModelCalculationEngine.simulateRedistribution(
            proposal = proposal,
            baseElements = elements,
            stations = stations,
            models = models,
            mixItems = mixItems,
            plan = plan
        )

        assertNotNull("Simulation result should not be null", simResult)
        assertNotNull("Simulation base summary should exist", simResult.baseSummary)
        assertNotNull("Simulation draft summary should exist", simResult.draftSummary)
    }

    @Test
    fun testMultiModelAiDiagnosisGeneration() {
        val repo = ManufacturingRepository.getInstance()
        val plan = repo.getProductionPlanForProject("P-001")
        val models = repo.models.filter { it.projectId == "P-001" }
        val stations = repo.stations
        val elements = repo.workElements.filter { it.projectId == "P-001" }
        val mixItems = repo.getModelMixForPlan(plan.id)
        val sequence = repo.getProductionSequencesForPlan(plan.id).firstOrNull()

        val summary = MixedModelCalculationEngine.calculateLineSummary(
            plan = plan,
            stations = stations,
            elements = elements,
            models = models,
            mixItems = mixItems
        )

        val diagnosis = MixedModelAiAdvisor.generateLineDiagnosis(
            plan = plan,
            summary = summary,
            mixItems = mixItems,
            sequence = sequence,
            elements = elements
        )

        // Ensure all 7 strict IE sections are populated
        assertTrue("1. Observed Facts must not be empty", diagnosis.observedFacts.isNotEmpty())
        assertTrue("2. Calculated Results must not be empty", diagnosis.calculatedResults.isNotEmpty())
        assertTrue("3. Engineering Interpretation must be present", diagnosis.engineeringInterpretation.isNotBlank())
        assertTrue("4. Recommendations must not be empty", diagnosis.recommendations.isNotEmpty())
        assertTrue("5. Risks & Constraints must not be empty", diagnosis.risksAndConstraints.isNotEmpty())
        assertTrue("6. Assumptions must not be empty", diagnosis.assumptions.isNotEmpty())
        assertTrue("7. Validation Required must not be empty", diagnosis.validationRequired.isNotEmpty())
    }
}
