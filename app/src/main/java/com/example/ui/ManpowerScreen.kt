package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ManpowerScreen(viewModel: CapacityViewModel, onNavigateToWhatIf: () -> Unit = {}, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val params = scenarios.find { it.id == activeScenarioId }
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val opps by viewModel.manpowerOpportunities.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = "Manpower", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Manpower Planning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Summary Column
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Baseline Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    params?.let { p ->
                        PlanningNumberField("Actual Operators Allocated", p.actualManpower.toDouble()) { v -> viewModel.updateActiveScenario { it.copy(actualManpower = v.toInt()) } }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Calculated Requirement", style = MaterialTheme.typography.labelMedium)
                            Text("${metrics?.calculatedManpowerRequirement ?: 0} Operators", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Based on Takt time and total work content.", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    opps?.let { o ->
                        val diff = (params?.actualManpower ?: 0) - (metrics?.calculatedManpowerRequirement ?: 0)
                        val color = if (diff > 0) Color(0xFF388E3C) else if (diff < 0) MaterialTheme.colorScheme.error else Color.Gray
                        Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text(if (diff > 0) "Potential Excess Manpower" else if (diff < 0) "Potential Shortage" else "Balanced", style = MaterialTheme.typography.labelMedium)
                                Text("${kotlin.math.abs(diff)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
                            }
                        }
                    }
                }
            }

            // Opportunities Column
            Card(modifier = Modifier.weight(2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Optimization Opportunities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    opps?.let { o ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Note", tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Status: ${o.status}", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Theoretical potential: ${o.theoreticalPotential} headcount.\nValidated saving: ${o.validatedSaving} (Confirm via Work Balance / What-If validation)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        o.suggestions.forEach { suggestion ->
                            val parts = suggestion.split(":")
                            if (parts.size >= 2) {
                                OpportunityCard(parts[0], parts.subList(1, parts.size).joinToString(":").trim())
                            } else {
                                OpportunityCard("Opportunity", suggestion)
                            }
                        }
                        
                        if (o.suggestions.isEmpty()) {
                            Text("No immediate optimization opportunities identified based on current parameters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(onClick = { onNavigateToWhatIf() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Validate in What-If Simulator")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpportunityCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
