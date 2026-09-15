package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*

@Composable
fun TimeStudyScreen(viewModel: TimeStudyViewModel, modifier: Modifier = Modifier) {
    val workElements by viewModel.workElements.collectAsStateWithLifecycle()
    val selectedElement by viewModel.selectedElement.collectAsStateWithLifecycle()

    if (workElements.isEmpty()) {
        EmptyState(
            title = "No Work Elements",
            message = "There are no work elements to analyze in this project."
        )
        return
    }

    Row(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Left Panel: List of Elements
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Work Elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { /* TODO: Add Element */ }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Element")
                        }
                        IconButton(onClick = { /* TODO: Export to CSV */ }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Study")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(workElements) { element ->
                        val isSelected = selectedElement?.id == element.id
                        ListItem(
                            headlineContent = { Text(element.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("Seq: ${element.sequence} | Source: ${element.timeSource.name}") },
                            trailingContent = {
                                IconButton(onClick = { /* TODO: Delete Element */ }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            },
                            modifier = Modifier
                                .clickable { viewModel.selectElement(element) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                ),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // Right Panel: Details and Analysis
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            selectedElement?.let { element ->
                val stats = viewModel.getStatsForElement(element.id)
                if (stats != null) {
                    ElementDetailHeader(element)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("Observations", "Statistics & Cycles", "Calculations", "VA/NVA Summary")
                    
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedTabIndex) {
                        0 -> ObservationTableContent(stats, viewModel)
                        1 -> StatisticsContent(stats)
                        2 -> CalculationsContent(element, viewModel)
                        3 -> VaNvaSummaryContent(element)
                    }
                }
            }
        }
    }
}

@Composable
fun ObservationTableContent(stats: ElementStats, viewModel: TimeStudyViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Observations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Header
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(8.dp)) {
                Text("Cycle", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Time (s)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Source", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Status", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Action", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            // Rows
            stats.observations.forEach { obs ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(obs.cycleId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(String.format("%.2f", obs.observedTime), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(obs.timeSource.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(if (obs.isRejected) "Rejected" else "Accepted", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (obs.isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.toggleObservationRejection(obs.id) }, modifier = Modifier.weight(1f)) {
                        Icon(if (obs.isRejected) Icons.Default.Check else Icons.Default.Close, contentDescription = "Toggle")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
fun StatisticsContent(stats: ElementStats) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Statistical Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Average", String.format("%.2f s", stats.average))
                    StatItem("Median", String.format("%.2f s", stats.median))
                    StatItem("Min", String.format("%.2f s", stats.min))
                    StatItem("Max", String.format("%.2f s", stats.max))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Std Deviation", String.format("%.3f s", stats.stdDev))
                    StatItem("Coeff of Variation", String.format("%.1f %%", stats.cv))
                    StatItem("Active Obs", "${stats.activeObservations.size}")
                    StatItem("Outliers", "${stats.observations.size - stats.activeObservations.size}")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text("Cycle Analysis (Times)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val maxTime = stats.observations.maxOfOrNull { it.observedTime }?.toFloat() ?: 1f
                val barColor = MaterialTheme.colorScheme.primary
                val rejectColor = MaterialTheme.colorScheme.error
                
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (stats.observations.size * 2).coerceAtLeast(1)
                    var xOffset = barWidth / 2
                    
                    stats.observations.forEach { obs ->
                        val barHeight = (obs.observedTime.toFloat() / maxTime) * size.height
                        val color = if (obs.isRejected) rejectColor else barColor
                        drawRect(
                            color = color,
                            topLeft = Offset(xOffset, size.height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                        xOffset += barWidth * 2
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun CalculationsContent(element: WorkElement, viewModel: TimeStudyViewModel) {
    var rating by remember(element.id) { mutableStateOf(element.performanceRating.toString()) }
    var allowance by remember(element.id) { mutableStateOf(element.allowance.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time Calculations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = rating,
                    onValueChange = { rating = it },
                    label = { Text("Performance Rating") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.updatePerformanceRating(element.id, rating.toDoubleOrNull() ?: 1.0) }) {
                    Text("Apply Rating")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = allowance,
                    onValueChange = { allowance = it },
                    label = { Text("Allowance (e.g., 0.1)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.updateAllowance(element.id, allowance.toDoubleOrNull() ?: 0.0) }) {
                    Text("Apply Allowance")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem("Observed Time", String.format("%.2f s", element.observedTime))
                StatItem("Normal Time", String.format("%.2f s", element.normalTime))
                StatItem("Standard Time", String.format("%.2f s", element.standardTime))
            }
        }
    }
}

@Composable
fun VaNvaSummaryContent(element: WorkElement) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Value Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatItem("Classification", element.valueClassification.name)
                    StatItem("Waste Category", element.wasteCategory.name)
                    StatItem("Automation Opp", if (element.automationOpportunity) "Yes" else "No")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Element Contribution (Cycle Time)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val standardTime = element.standardTime.toFloat()
                // Fake total cycle time for illustration (in a real app, sum all standard times for the project/station)
                val totalTime = maxOf(standardTime, 60f)
                val percentage = if (totalTime > 0) standardTime / totalTime else 0f
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Standard Time: ${String.format("%.2f", standardTime)} s", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(16.dp))
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.weight(1f).height(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(String.format("%.1f %%", percentage * 100), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ElementDetailHeader(element: WorkElement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(element.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(element.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Station: ${element.stationId}", color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(4.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Operator: ${element.operatorId}", color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(4.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("Source: ${element.timeSource.name}", color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}
