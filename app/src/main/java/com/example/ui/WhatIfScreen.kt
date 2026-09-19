package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Scenario Header & Controls
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = "Simulator", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("What-If Simulation Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Evaluate operator reallocation and process kaizen without interrupting line", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }

                    Spacer(Modifier.width(20.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = IeRadius.buttonShape,
                            border = BorderStroke(1.dp, StitchSlate300)
                        ) {
                            Text(activeScenario?.name ?: "Select Scenario", color = StitchSlate900, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = StitchSlate600)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            scenarios.forEach { scn ->
                                DropdownMenuItem(
                                    text = { Text(scn.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        viewModel.selectScenario(scn.id)
                                        expanded = false
                                    },
                                    trailingIcon = if (scn.id == activeScenarioId) {
                                        { Icon(Icons.Default.Check, contentDescription = "Active", tint = StitchCobalt600) }
                                    } else null
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Create Scenario", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.createScenario("Scenario ${scenarios.size + 1}")
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add", tint = StitchCobalt600) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { activeScenarioId?.let { viewModel.duplicateScenario(it) } },
                        shape = IeRadius.buttonShape,
                        border = BorderStroke(1.dp, StitchSlate300)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp), tint = StitchSlate700)
                        Spacer(Modifier.width(4.dp))
                        Text("Duplicate", color = StitchSlate800)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetScenario() },
                        shape = IeRadius.buttonShape,
                        border = BorderStroke(1.dp, StitchSlate300)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp), tint = StitchSlate700)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Baseline", color = StitchSlate800)
                    }
                    Button(
                        onClick = {
                            viewModel.analyzeScenario()
                            showAiDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Analyze Scenario")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Metrics Comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ComparisonCard("Balance Efficiency", baseMetrics?.balanceEfficiency?.times(100), activeMetrics?.balanceEfficiency?.times(100), "%", Modifier.weight(1f))
            ComparisonCard("Max Cycle Time", baseMetrics?.maxCycleTime, activeMetrics?.maxCycleTime, "s", Modifier.weight(1f), invertColor = true)
            ComparisonCard("Throughput Capacity", baseMetrics?.capacityPerHr, activeMetrics?.capacityPerHr, " pcs/h", Modifier.weight(1f))
            ComparisonCard("VA Stream %", baseMetrics?.vaPercent, activeMetrics?.vaPercent, "%", Modifier.weight(1f))
        }

        // Warnings List
        if (warnings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchNvaRed.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = StitchNvaRedLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = StitchNvaRedText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Precedence Constraint Violations (${warnings.size})", style = MaterialTheme.typography.titleSmall, color = StitchNvaRedText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    warnings.forEach { warning ->
                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = StitchNvaRedText)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Scenario Editor workspace
        activeScenario?.let { scn ->
            val elementsByStation = scn.elements.groupBy { it.stationId }

            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
fun ComparisonCard(
    title: String,
    base: Double?,
    active: Double?,
    unit: String,
    modifier: Modifier = Modifier,
    invertColor: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Text(title.uppercase(), style = IeTypography.tableHeader, color = StitchSlate500)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val formatValue: (Double?) -> String = { v ->
                    if (v == null) "-"
                    else if (v % 1.0 == 0.0 && (unit.contains("pcs") || unit.contains("stn"))) "${v.toInt()}$unit"
                    else String.format("%.1f%s", v, unit)
                }

                Column {
                    Text("Baseline", style = MaterialTheme.typography.labelSmall, color = StitchSlate400)
                    Text(
                        formatValue(base),
                        style = IeTypography.dataMono,
                        color = StitchSlate600
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Scenario", style = MaterialTheme.typography.labelSmall, color = StitchSlate400)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatValue(active),
                            style = IeTypography.kpiMedium,
                            color = StitchSlate900
                        )
                        if (base != null && active != null) {
                            val diff = active - base
                            if (kotlin.math.abs(diff) > 0.05) {
                                val isPositiveImpact = if (invertColor) diff < 0 else diff > 0
                                val color = if (isPositiveImpact) StitchVaGreenText else StitchNvaRedText
                                val bg = if (isPositiveImpact) StitchVaGreenLight else StitchNvaRedLight
                                Surface(
                                    shape = IeRadius.badgeShape,
                                    color = bg,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = String.format("%+.1f", diff),
                                        style = IeTypography.dataMonoBold,
                                        fontSize = 10.sp,
                                        color = color,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
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
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        border = BorderStroke(1.dp, if (isOverTakt) StitchNvaRed else StitchSlate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
                Surface(
                    shape = IeRadius.badgeShape,
                    color = if (isOverTakt) StitchNvaRedLight else StitchVaGreenLight,
                    border = BorderStroke(1.dp, if (isOverTakt) StitchNvaRed.copy(alpha = 0.5f) else StitchVaGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        String.format("%.1fs", totalTime),
                        style = IeTypography.dataMonoBold,
                        color = if (isOverTakt) StitchNvaRedText else StitchVaGreenText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchSlate50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(element.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = StitchSlate900)
                    Text(
                        "Time: ${String.format("%.1fs", element.standardTime)}",
                        style = IeTypography.dataMono,
                        color = StitchSlate600
                    )
                }

                var showMoveMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showMoveMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Move", tint = StitchCobalt600, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                        stations.forEach { station ->
                            DropdownMenuItem(
                                text = { Text("Move to ${station.name}", style = MaterialTheme.typography.bodyMedium) },
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
                HorizontalDivider(color = StitchSlate200)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTimeText,
                        onValueChange = { editTimeText = it },
                        label = { Text("Cycle Time (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = IeTypography.dataMono,
                        shape = IeRadius.inputShape
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            editTimeText.toDoubleOrNull()?.let {
                                viewModel.updateElementTime(element.id, it)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape
                    ) {
                        Text("Apply")
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.eliminateElement(element.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = IeRadius.buttonShape,
                    border = BorderStroke(1.dp, StitchNvaRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminate", tint = StitchNvaRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminate Element (Kaizen)", color = StitchNvaRedText)
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
        shape = IeRadius.dialogShape,
        containerColor = StitchWhite,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = StitchCobalt600) },
        title = {
            Text(
                "AI Scenario Evaluation & Bottleneck Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
        },
        text = {
            if (isAnalyzing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = StitchCobalt600, strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Gemini AI evaluating line balance trade-offs...", style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
                }
            } else if (analysis != null) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalysisSection("Predicted Improvement", analysis.improvements)
                    AnalysisSection("Trade-Offs & Station Load", analysis.tradeOffs)
                    AnalysisSection("Remaining Bottleneck Station", analysis.remainingBottleneck)
                    AnalysisSection("Model Assumptions", analysis.assumptions)

                    Card(
                        shape = IeRadius.cardShape,
                        border = BorderStroke(1.dp, StitchNvaRed.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = StitchNvaRedLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Operational Risks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchNvaRedText)
                            Spacer(Modifier.height(4.dp))
                            Text(analysis.risks, style = MaterialTheme.typography.bodySmall, color = StitchNvaRedText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                shape = IeRadius.buttonShape
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AnalysisSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchCobalt700)
        Spacer(Modifier.height(2.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, color = StitchSlate700)
    }
}
