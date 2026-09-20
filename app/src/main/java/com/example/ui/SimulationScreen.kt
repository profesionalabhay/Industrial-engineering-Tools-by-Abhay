package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    projectId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "DIGITAL LINE SIMULATION",
                subtitle = "Deterministic modeling and capacity prediction",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StitchSlate50)
        ) {
            // Simulation Controls
            IeCard(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SIMULATION DURATION (HOURS)", style = IeTypography.badgeText, color = StitchSlate500)
                        Slider(
                            value = uiState.simulationHours.toFloat(),
                            onValueChange = { viewModel.updateDuration(it.toDouble()) },
                            valueRange = 1f..24f,
                            steps = 23,
                            colors = SliderDefaults.colors(thumbColor = StitchCobalt600, activeTrackColor = StitchCobalt500)
                        )
                        Text("${uiState.simulationHours.toInt()} Hours", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.runSimulation(projectId, "Standard Shift Run") },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt600),
                        shape = IeRadius.cardShape
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("RUN SIM")
                    }
                }
            }

            if (uiState.latestResult != null) {
                SimulationResultPanel(uiState.latestResult!!)
            } else {
                IeEmptyState(title = "No Simulation Run", message = "Configure duration and run simulation to see results.")
            }
        }
    }
}

@Composable
fun SimulationResultPanel(result: SimulationResult) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiBox("THROUGHPUT", "${String.format("%.1f", result.throughputPerHour)} u/h", StitchCobalt600, Modifier.weight(1f))
                KpiBox("TOTAL UNITS", "${result.simulatedUnits}", StitchSlate900, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiBox("LEAD TIME", "${String.format("%.1f", result.leadTimeMinutes)} min", StitchTaktLineIndigo, Modifier.weight(1f))
                KpiBox("UTILISATION", "${String.format("%.1f", result.capacityUtilization * 100)}%", StitchVaGreen, Modifier.weight(1f))
            }
        }

        item {
            IeSectionHeader(title = "BOTTLENECK ANALYSIS", subtitle = "Station loading and utilization")
            IeCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    result.operatorUtilisation.forEach { (stationId, util) ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stationId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            IeLinearProgress(
                                label = "Utilisation",
                                value = "${String.format("%.1f", util * 100)}%",
                                percentage = util.toFloat(),
                                color = if (stationId == result.bottleneckStationId) StitchNvaRed else StitchCobalt500
                            )
                        }
                    }
                }
            }
        }

        item {
            IeCard {
                Text("BOTTLENECK STATION", style = IeTypography.tableHeader, color = StitchSlate500)
                Text(result.bottleneckStationId, style = MaterialTheme.typography.titleLarge, color = StitchNvaRed, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Cycle Time: ${String.format("%.2f", result.cycleTimeSeconds)}s", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun KpiBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    IeCard(modifier = modifier, contentPadding = 12.dp) {
        Text(label, style = IeTypography.badgeText, color = StitchSlate500)
        Text(value, style = IeTypography.kpiLarge, color = color, fontSize = 24.sp)
    }
}
