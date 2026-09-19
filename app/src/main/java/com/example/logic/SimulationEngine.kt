package com.example.logic

import com.example.data.*
import java.util.UUID

class SimulationEngine(private val repository: ManufacturingRepository) {

    fun runDeterministicSimulation(
        projectId: String,
        scenarioName: String,
        durationHours: Double = 8.0
    ): SimulationResult {
        val stations = repository.stations.filter { it.processId.startsWith("PRC") } // Simplified filter
        val projectElements = repository.workElements.filter { it.projectId == projectId }
        
        // Build simulation nodes
        val nodes = stations.map { station ->
            val stationElements = projectElements.filter { it.stationId == station.id }
            val cycleTime = stationElements.sumOf { it.standardTime }
            SimulationNode(
                stationId = station.id,
                cycleTimeSeconds = cycleTime.coerceAtLeast(1.0),
                operatorCount = 1 // Default to 1 for simulation
            )
        }.sortedBy { it.stationId } // Assumption: Station ID implies sequence

        if (nodes.isEmpty()) {
            return SimulationResult(
                id = "SIM-${UUID.randomUUID()}",
                projectId = projectId,
                scenarioName = scenarioName,
                throughputPerHour = 0.0,
                cycleTimeSeconds = 0.0,
                bottleneckStationId = "N/A",
                averageWip = 0.0,
                operatorUtilisation = emptyMap(),
                stationIdleTime = emptyMap(),
                leadTimeMinutes = 0.0,
                capacityUtilization = 0.0,
                simulatedUnits = 0
            )
        }

        val totalDurationSeconds = durationHours * 3600.0
        val bottleneckNode = nodes.maxByOrNull { it.cycleTimeSeconds }!!
        val bottleneckCycleTime = bottleneckNode.cycleTimeSeconds
        
        // Deterministic Simulation Logic
        val theoreticalThroughput = totalDurationSeconds / bottleneckCycleTime
        val throughputPerHour = 3600.0 / bottleneckCycleTime
        
        val utilization = nodes.associate { node ->
            node.stationId to (node.cycleTimeSeconds / bottleneckCycleTime).coerceIn(0.0, 1.0)
        }
        
        val idleTime = nodes.associate { node ->
            node.stationId to (bottleneckCycleTime - node.cycleTimeSeconds)
        }
        
        // Little's Law / Lead Time simplified
        val totalWorkContent = nodes.sumOf { it.cycleTimeSeconds }
        val leadTimeMinutes = (totalWorkContent + (nodes.size * bottleneckCycleTime)) / 60.0 // Simplified WIP influence
        
        return SimulationResult(
            id = "SIM-${UUID.randomUUID()}",
            projectId = projectId,
            scenarioName = scenarioName,
            throughputPerHour = throughputPerHour,
            cycleTimeSeconds = bottleneckCycleTime,
            bottleneckStationId = bottleneckNode.stationId,
            averageWip = nodes.size.toDouble() * 1.2, // Simplified WIP buffer
            operatorUtilisation = utilization,
            stationIdleTime = idleTime,
            leadTimeMinutes = leadTimeMinutes,
            capacityUtilization = utilization.values.average(),
            simulatedUnits = theoreticalThroughput.toInt(),
            simulationDurationHours = durationHours
        )
    }
}
