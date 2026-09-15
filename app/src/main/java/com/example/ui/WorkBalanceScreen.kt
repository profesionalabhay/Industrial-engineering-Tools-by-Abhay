package com.example.ui

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*

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

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        // Header & Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Work Balance Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        Button(onClick = { showSuggestDialog = true }, enabled = !isGenerating, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Suggest")
                            Spacer(Modifier.width(4.dp))
                            Text("Suggest Balance")
                        }
                        FilledTonalButton(onClick = { viewModel.saveDraftAsScenario("New Balance") }) {
                            Text("Save as Scenario")
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MetricComparisonBox("Efficiency", baseMetrics?.balanceEfficiency?.times(100), draftMetrics?.balanceEfficiency?.times(100), "%")
                    MetricComparisonBox("Max CT", baseMetrics?.maxCycleTime, draftMetrics?.maxCycleTime, "s")
                    MetricComparisonBox("Stations", baseMetrics?.numStations?.toDouble(), draftMetrics?.numStations?.toDouble(), "")
                    MetricComparisonBox("Takt", baseMetrics?.taktTime, draftMetrics?.taktTime, "s", showDiff = false)
                }
            }
        }

        // Warnings
        if (warnings.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Constraint Violations", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                    warnings.forEach { warning ->
                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Station Columns
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
            onApply = { proposalId -> 
                viewModel.applyProposal(proposalId)
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
        modifier = Modifier.width(320.dp).fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(draft.station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Badge(containerColor = if (isOverTakt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                    Text(String.format("%.1fs", draft.cycleTime), color = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
            
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (draft.cycleTime / taktTime).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (isOverTakt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(String.format("Utilization: %.1f%%", utilization), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            
            Spacer(Modifier.height(12.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragIndicator, contentDescription = "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(element.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Time: ${String.format("%.1fs", element.standardTime)}", style = MaterialTheme.typography.labelSmall)
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Move")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text("Move to ${station.name}") },
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
fun MetricComparisonBox(label: String, base: Double?, draft: Double?, unit: String, showDiff: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (draft != null) String.format("%.1f", draft) + unit else "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (showDiff && base != null && draft != null) {
                val diff = draft - base
                if (kotlin.math.abs(diff) > 0.1) {
                    val color = if (diff < 0) VaColor else NvaColor // Typically less CT/Stations is better
                    Text(
                        text = String.format(" (%+.1f)", diff),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                    )
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
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest Balance Solutions") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (proposals.isEmpty() && !isGenerating) {
                    Text("Run the balancing engine to generate feasible assignments based on heuristics.")
                } else if (isGenerating) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        items(proposals) { proposal ->
                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(proposal.methodName, fontWeight = FontWeight.Bold)
                                        Button(onClick = { onApply(proposal.id) }) {
                                            Text("Apply")
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Efficiency: ${String.format("%.1f%%", proposal.metrics.balanceEfficiency * 100)}", style = MaterialTheme.typography.bodySmall)
                                        Text("Stations: ${proposal.metrics.numStations}", style = MaterialTheme.typography.bodySmall)
                                        Text("Violations: ${proposal.violations}", style = MaterialTheme.typography.bodySmall, color = if (proposal.violations > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
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
                Button(onClick = onGenerate) {
                    Text("Generate Solutions")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
