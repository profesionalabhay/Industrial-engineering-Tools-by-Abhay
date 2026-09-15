package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CapacityScreen(viewModel: CapacityViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val params = scenarios.find { it.id == activeScenarioId }
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = "Capacity", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Capacity Planning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                
                // Scenario Toggle
                params?.let { scn ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = scn.isFuture,
                            onCheckedChange = { viewModel.switchScenario(it) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("Future", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Inputs Column
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Planning Inputs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    
                    params?.let { p ->
                        PlanningNumberField("Daily Demand", p.dailyDemand.toDouble()) { v -> viewModel.updateActiveScenario { it.copy(dailyDemand = v.toInt()) } }
                        PlanningNumberField("Shift Duration (Hours)", p.shiftLengthHours) { v -> viewModel.updateActiveScenario { it.copy(shiftLengthHours = v) } }
                        PlanningNumberField("Breaks (Minutes)", p.breaksMinutes) { v -> viewModel.updateActiveScenario { it.copy(breaksMinutes = v) } }
                        PlanningNumberField("Planned Downtime (Minutes)", p.plannedDowntimeMinutes) { v -> viewModel.updateActiveScenario { it.copy(plannedDowntimeMinutes = v) } }
                        PlanningNumberField("Performance Efficiency (0-1)", p.performanceEfficiency) { v -> viewModel.updateActiveScenario { it.copy(performanceEfficiency = v) } }
                        PlanningNumberField("Quality Yield (0-1)", p.qualityYield) { v -> viewModel.updateActiveScenario { it.copy(qualityYield = v) } }
                        PlanningNumberField("Bottleneck CT (sec)", p.bottleneckCtSec) { v -> viewModel.updateActiveScenario { it.copy(bottleneckCtSec = v) } }
                    }
                }
            }

            // Outputs Column
            Card(modifier = Modifier.weight(2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                metrics?.let { m ->
                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Capacity Calculations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CapacityMetricCard("Takt Time", String.format("%.1f s", m.taktTimeSec), Modifier.weight(1f))
                            CapacityMetricCard("UPH", String.format("%.1f /hr", m.uph), Modifier.weight(1f))
                            CapacityMetricCard("Daily Capacity", String.format("%d units", m.dailyCapacity), Modifier.weight(1f))
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CapacityMetricCard("Available Time", String.format("%.1f hrs", m.availableTimeSec / 3600), Modifier.weight(1f))
                            CapacityMetricCard("Net Operating Time", String.format("%.1f hrs", m.netOperatingTimeSec / 3600), Modifier.weight(1f))
                            CapacityMetricCard("Utilization", String.format("%.1f %%", m.utilization), Modifier.weight(1f))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        val isGap = m.capacityGap < 0 // Negative means shortage in this view model
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isGap) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Capacity Gap Analysis", fontWeight = FontWeight.Bold, color = if (isGap) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20))
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Calculated Gap:", style = MaterialTheme.typography.bodyMedium)
                                    Text(String.format("%+d units", m.capacityGap), fontWeight = FontWeight.Bold)
                                }
                                if (isGap) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Overtime Required:", style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format("%.1f hrs", m.requiredOvertimeHours), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningNumberField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { 
            text = it
            it.toDoubleOrNull()?.let { v -> onValueChange(v) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun CapacityMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
