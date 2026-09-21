package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WorkBalanceScreen(viewModel: WorkBalanceViewModel, modifier: Modifier = Modifier) {
    val draftStations by remember { derivedStateOf { viewModel.getDraftStations() } }
    val baseMetrics by viewModel.baselineMetrics.collectAsStateWithLifecycle()
    val draftMetrics by viewModel.draftMetrics.collectAsStateWithLifecycle()
    val warnings by viewModel.warnings.collectAsStateWithLifecycle()
    val stationsList by viewModel.stations.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val proposals by viewModel.proposedBalances.collectAsStateWithLifecycle()

    var showSuggestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Header & Line Balance Metrics
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Work Line Balancing Canvas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Interactive element assignment & heuristic line optimization",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchSlate500
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showSuggestDialog = true },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt600),
                            shape = IeRadius.buttonShape,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Suggest Balance", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveDraftAsScenario("Balanced Line Scenario") },
                            shape = IeRadius.buttonShape,
                            border = BorderStroke(1.dp, StitchSlate300)
                        ) {
                            Text("Save Scenario", color = StitchSlate800)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricComparisonBox(
                        label = "Balance Efficiency",
                        base = baseMetrics?.balanceEfficiency?.times(100),
                        draft = draftMetrics?.balanceEfficiency?.times(100),
                        unit = "%",
                        higherIsBetter = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricComparisonBox(
                        label = "Max Cycle Time",
                        base = baseMetrics?.maxCycleTime,
                        draft = draftMetrics?.maxCycleTime,
                        unit = "s",
                        higherIsBetter = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricComparisonBox(
                        label = "Active Stations",
                        base = baseMetrics?.numStations?.toDouble(),
                        draft = draftMetrics?.numStations?.toDouble(),
                        unit = " stn",
                        higherIsBetter = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricComparisonBox(
                        label = "Target Takt Time",
                        base = baseMetrics?.taktTime,
                        draft = draftMetrics?.taktTime,
                        unit = "s",
                        showDiff = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Precedence & Constraint Warnings
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
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = StitchNvaRedText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Constraint Violations (${warnings.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = StitchNvaRedText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    warnings.forEach { warning ->
                        Text(
                            "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchNvaRedText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Station Balancing Columns
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(draftStations) { stationDraft ->
                StationColumn(
                    draft = stationDraft,
                    allStations = stationsList,
                    taktTime = draftMetrics?.taktTime ?: 60.0,
                    onMoveElement = { elementId, newStationId ->
                        viewModel.moveElement(elementId, newStationId)
                    }
                )
            }
        }
    }

    if (showSuggestDialog) {
        SuggestBalanceDialog(
            proposals = proposals,
            isGenerating = isGenerating,
            onGenerate = { viewModel.generateSuggestions() },
            onApply = { proposal ->
                viewModel.applyProposal(proposal)
                showSuggestDialog = false
            },
            onDismiss = { showSuggestDialog = false }
        )
    }
}

@Composable
fun StationColumn(
    draft: StationDraft,
    allStations: List<Station>,
    taktTime: Double,
    onMoveElement: (String, String) -> Unit
) {
    val utilization = if (taktTime > 0) (draft.cycleTime / taktTime) * 100 else 0.0
    val isOverTakt = draft.cycleTime > taktTime

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
            // Station Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        draft.station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate900
                    )
                    Text(
                        "${draft.elements.size} work elements",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchSlate500
                    )
                }

                Surface(
                    shape = IeRadius.badgeShape,
                    color = if (isOverTakt) StitchNvaRedLight else StitchVaGreenLight,
                    border = BorderStroke(1.dp, if (isOverTakt) StitchNvaRed.copy(alpha = 0.5f) else StitchVaGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        String.format("%.1fs", draft.cycleTime),
                        style = IeTypography.dataMonoBold,
                        color = if (isOverTakt) StitchNvaRedText else StitchVaGreenText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Utilization Bar
            LinearProgressIndicator(
                progress = { (draft.cycleTime / taktTime).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverTakt) StitchNvaRed else StitchCobalt600,
                trackColor = StitchSlate100
            )

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("Utilization: %.1f%%", utilization),
                    style = IeTypography.dataMono,
                    fontSize = 11.sp,
                    color = if (isOverTakt) StitchNvaRedText else StitchSlate600
                )
                Text(
                    text = if (isOverTakt) "OVER TAKT" else "BALANCED",
                    style = IeTypography.badgeText,
                    fontSize = 10.sp,
                    color = if (isOverTakt) StitchNvaRedText else StitchVaGreenText
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(draft.elements) { element ->
                    DraggableElementCard(element, allStations, onMoveElement)
                }
            }
        }
    }
}

@Composable
fun DraggableElementCard(
    element: WorkElement,
    stations: List<Station>,
    onMoveElement: (String, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchSlate50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = "Drag",
                tint = StitchSlate400,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    element.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchSlate900
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Time: ${String.format("%.1fs", element.standardTime)}",
                        style = IeTypography.dataMono,
                        color = StitchSlate600
                    )
                    Spacer(Modifier.width(6.dp))
                    IeClassificationBadge(element.valueClassification)
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Reassign",
                        tint = StitchCobalt600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text("Move to ${station.name}", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onMoveElement(element.id, station.id)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricComparisonBox(
    label: String,
    base: Double?,
    draft: Double?,
    unit: String,
    modifier: Modifier = Modifier,
    showDiff: Boolean = true,
    higherIsBetter: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                label.uppercase(),
                style = IeTypography.tableHeader,
                color = StitchSlate500
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                val formattedValue = if (draft != null) {
                    if (draft % 1.0 == 0.0 && (unit.contains("stn") || unit.isEmpty())) {
                        "${draft.toInt()}$unit"
                    } else {
                        String.format("%.1f", draft) + unit
                    }
                } else "-"

                Text(
                    text = formattedValue,
                    style = IeTypography.kpiMedium,
                    color = StitchSlate900
                )
                if (showDiff && base != null && draft != null) {
                    val diff = draft - base
                    if (kotlin.math.abs(diff) > 0.05) {
                        val isPositiveImpact = if (higherIsBetter) diff > 0 else diff < 0
                        val color = if (isPositiveImpact) StitchVaGreenText else StitchNvaRedText
                        val bg = if (isPositiveImpact) StitchVaGreenLight else StitchNvaRedLight
                        Surface(
                            shape = IeRadius.badgeShape,
                            color = bg,
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
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

@Composable
fun SuggestBalanceDialog(
    proposals: List<ProposedBalance>,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onApply: (ProposedBalance) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = IeRadius.dialogShape,
        containerColor = StitchWhite,
        title = {
            Text(
                "Suggest Balance Solutions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (proposals.isEmpty() && !isGenerating) {
                    Text(
                        "Execute the heuristic balancing engine (Ranked Positional Weight & Kilbridge-Wester) to re-allocate elements under Takt constraint.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchSlate600
                    )
                } else if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StitchCobalt600, strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Evaluating balancing iterations...", style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 380.dp)
                    ) {
                        items(proposals) { proposal ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = IeRadius.cardShape,
                                border = BorderStroke(1.dp, StitchSlate200),
                                colors = CardDefaults.cardColors(containerColor = StitchSlate50),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            proposal.methodName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchSlate900
                                        )
                                        Button(
                                            onClick = { onApply(proposal) },
                                            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                                            shape = IeRadius.buttonShape
                                        ) {
                                            Text("Apply", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Efficiency: ${String.format("%.1f%%", proposal.metrics.balanceEfficiency * 100)}",
                                            style = IeTypography.dataMono,
                                            color = StitchSlate700
                                        )
                                        Text(
                                            "Stations: ${proposal.metrics.numStations}",
                                            style = IeTypography.dataMono,
                                            color = StitchSlate700
                                        )
                                        Text(
                                            "Violations: ${proposal.violations}",
                                            style = IeTypography.dataMono,
                                            color = if (proposal.violations > 0) StitchNvaRedText else StitchVaGreenText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isGenerating && proposals.isEmpty()) {
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Text("Generate Solutions")
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = IeRadius.buttonShape,
                    border = BorderStroke(1.dp, StitchSlate300)
                ) {
                    Text("Close", color = StitchSlate700)
                }
            }
        }
    )
}
