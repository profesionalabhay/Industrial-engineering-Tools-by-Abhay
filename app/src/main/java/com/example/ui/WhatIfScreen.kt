package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*

@Composable
fun WhatIfScreen(viewModel: WhatIfViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    
    val baseMetrics by viewModel.baselineMetrics.collectAsStateWithLifecycle()
    val activeMetrics by viewModel.activeMetrics.collectAsStateWithLifecycle()
    val warnings by viewModel.warnings.collectAsStateWithLifecycle()
    
    val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    val activeScenario = scenarios.find { it.id == activeScenarioId }
    var showAiDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Scenario Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = "Simulator", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("IE What-If Simulator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.width(24.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(activeScenario?.name ?: "Select Scenario")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            scenarios.forEach { scn ->
                                DropdownMenuItem(
                                    text = { Text(scn.name) },
                                    onClick = { 
                                        viewModel.selectScenario(scn.id)
                                        expanded = false 
                                    },
                                    trailingIcon = if (scn.id == activeScenarioId) {
                                        { Icon(Icons.Default.Check, contentDescription = "Active") }
                                    } else null
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("New Scenario") },
                                onClick = { 
                                    viewModel.createScenario("Scenario ${scenarios.size + 1}")
                                    expanded = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add") }
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { activeScenarioId?.let { viewModel.duplicateScenario(it) } }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Duplicate")
                    }
                    OutlinedButton(onClick = { viewModel.resetScenario() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Baseline")
                    }
                    Button(
                        onClick = { 
                            viewModel.analyzeScenario()
                            showAiDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze")
                        Spacer(Modifier.width(4.dp))
                        Text("Analyze Scenario")
                    }
                }
            }
        }

        // Metrics Comparison
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ComparisonCard("Balance Efficiency", baseMetrics?.balanceEfficiency?.times(100), activeMetrics?.balanceEfficiency?.times(100), "%", Modifier.weight(1f))
            ComparisonCard("Max Cycle Time", baseMetrics?.maxCycleTime, activeMetrics?.maxCycleTime, "s", Modifier.weight(1f), invertColor = true)
            ComparisonCard("Capacity", baseMetrics?.capacityPerHr, activeMetrics?.capacityPerHr, "/hr", Modifier.weight(1f))
            ComparisonCard("VA Time %", baseMetrics?.vaPercent, activeMetrics?.vaPercent, "%", Modifier.weight(1f))
        }

        // Warnings List
        if (warnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Constraint Violations (${warnings.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                    warnings.forEach { warning ->
                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Scenario Editor workspace
        activeScenario?.let { scn ->
            val elementsByStation = scn.elements.groupBy { it.stationId }
            
            LazyRow(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(scn.stations) { station ->
                    val stationElements = elementsByStation[station.id]?.sortedBy { it.sequence } ?: emptyList()
                    StationEditorColumn(
                        station = station,
                        elements = stationElements,
                        allStations = scn.stations,
                        taktTime = activeMetrics?.taktTime ?: 60.0,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAiDialog) {
        AiAnalysisDialog(
            isAnalyzing = isAnalyzing,
            analysis = aiAnalysis,
            onDismiss = { showAiDialog = false }
        )
    }
}

@Composable
fun ComparisonCard(title: String, base: Double?, active: Double?, unit: String, modifier: Modifier = Modifier, invertColor: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Baseline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (base != null) String.format("%.1f$unit", base) else "-", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Scenario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (active != null) String.format("%.1f$unit", active) else "-",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (base != null && active != null) {
                            val diff = active - base
                            if (kotlin.math.abs(diff) > 0.05) {
                                val isPositiveImpact = if (invertColor) diff < 0 else diff > 0
                                val color = if (isPositiveImpact) VaColor else NvaColor
                                Text(
                                    text = String.format(" (%+.1f)", diff),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationEditorColumn(
    station: Station,
    elements: List<WorkElement>,
    allStations: List<Station>,
    taktTime: Double,
    viewModel: WhatIfViewModel
) {
    val totalTime = elements.sumOf { it.standardTime }
    val isOverTakt = totalTime > taktTime

    Card(
        modifier = Modifier.width(320.dp).fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Badge(containerColor = if (isOverTakt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                    Text(String.format("%.1fs", totalTime), color = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(elements) { element ->
                    WhatIfElementCard(element, allStations, viewModel)
                }
            }
        }
    }
}

@Composable
fun WhatIfElementCard(
    element: WorkElement,
    stations: List<Station>,
    viewModel: WhatIfViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var editTimeText by remember(element.standardTime) { mutableStateOf(element.standardTime.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(element.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Time: ${String.format("%.1fs", element.standardTime)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                var showMoveMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMoveMenu = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Move")
                    }
                    DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                        stations.forEach { station ->
                            DropdownMenuItem(
                                text = { Text(station.name) },
                                onClick = {
                                    viewModel.moveElement(element.id, station.id)
                                    showMoveMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTimeText,
                        onValueChange = { editTimeText = it },
                        label = { Text("CT (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { 
                        editTimeText.toDoubleOrNull()?.let {
                            viewModel.updateElementTime(element.id, it)
                        }
                    }) {
                        Text("Apply")
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.eliminateElement(element.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminate")
                    Spacer(Modifier.width(4.dp))
                    Text("Eliminate Element")
                }
            }
        }
    }
}

@Composable
fun AiAnalysisDialog(
    isAnalyzing: Boolean,
    analysis: AiAnalysisResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
        title = { Text("AI Scenario Analysis") },
        text = {
            if (isAnalyzing) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Gemini is analyzing scenario trade-offs...")
                }
            } else if (analysis != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalysisSection("Predicted Improvement", analysis.improvements)
                    AnalysisSection("Trade-Offs", analysis.tradeOffs)
                    AnalysisSection("Remaining Bottleneck", analysis.remainingBottleneck)
                    AnalysisSection("Assumptions", analysis.assumptions)
                    
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Risks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(analysis.risks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AnalysisSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
