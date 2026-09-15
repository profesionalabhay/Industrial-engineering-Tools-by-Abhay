package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*

val VaColor = Color(0xFF4CAF50) // Green
val NnvaColor = Color(0xFFFF9800) // Orange
val NvaColor = Color(0xFFF44336) // Red
val TaktColor = Color(0xFF2196F3) // Blue

@Composable
fun YamazumiScreen(viewModel: YamazumiViewModel, modifier: Modifier = Modifier) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val taktTime by viewModel.taktTime.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()

    if (metrics.isEmpty()) {
        EmptyState(
            title = "Yamazumi Chart",
            message = "No stations or work elements available to balance."
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Yamazumi Line Balance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (taktTime == null) {
                        Text("Takt time required for balance comparison.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Takt Time: ${String.format("%.2f s", taktTime)}", style = MaterialTheme.typography.bodyMedium, color = TaktColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row {
                    Button(onClick = { /* TODO: Scenario Compare */ }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Scenarios")
                    }
                    IconButton(onClick = { /* TODO: Export */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Area
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                // Legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    LegendItem("VA", VaColor)
                    Spacer(Modifier.width(16.dp))
                    LegendItem("NNVA", NnvaColor)
                    Spacer(Modifier.width(16.dp))
                    LegendItem("NVA", NvaColor)
                    Spacer(Modifier.width(16.dp))
                    LegendItem("Takt", TaktColor, isLine = true)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                YamazumiChart(metrics, taktTime) { stationId ->
                    viewModel.selectStation(stationId)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Area: Station Detail
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (selectedStation == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a station segment from the chart to view details.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                StationDetailContent(selectedStation!!, viewModel)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, isLine: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(12.dp).background(color, if (isLine) RoundedCornerShape(0.dp) else RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun YamazumiChart(metrics: List<StationYamazumiMetrics>, taktTime: Double?, onStationClick: (String) -> Unit) {
    val maxTime = maxOf(
        taktTime ?: 0.0,
        metrics.maxOfOrNull { it.totalTime } ?: 1.0
    ) * 1.1 // Add 10% headroom
    
    val stationCount = metrics.size
    
    Canvas(modifier = Modifier.fillMaxSize().pointerInput(metrics) {
        detectTapGestures { offset ->
            val canvasWidth = size.width
            val barWidth = canvasWidth / (stationCount * 2)
            val padding = barWidth / 2
            var currentX = padding
            
            for (metric in metrics) {
                if (offset.x >= currentX && offset.x <= currentX + barWidth) {
                    onStationClick(metric.station.id)
                    return@detectTapGestures
                }
                currentX += barWidth * 2
            }
        }
    }) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val barWidth = canvasWidth / (stationCount * 2)
        val padding = barWidth / 2
        var currentX = padding
        
        // Draw Takt Line
        if (taktTime != null) {
            val taktY = canvasHeight - ((taktTime / maxTime) * canvasHeight).toFloat()
            drawLine(
                color = TaktColor,
                start = Offset(0f, taktY),
                end = Offset(canvasWidth, taktY),
                strokeWidth = 4f
            )
        }
        
        // Draw Bars
        metrics.forEach { metric ->
            val vaHeight = ((metric.vaTime / maxTime) * canvasHeight).toFloat()
            val nnvaHeight = ((metric.nnvaTime / maxTime) * canvasHeight).toFloat()
            val nvaHeight = ((metric.nvaTime / maxTime) * canvasHeight).toFloat()
            
            var currentY = canvasHeight
            
            // Draw VA
            currentY -= vaHeight
            drawRect(
                color = VaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, vaHeight)
            )
            
            // Draw NNVA
            currentY -= nnvaHeight
            drawRect(
                color = NnvaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, nnvaHeight)
            )
            
            // Draw NVA
            currentY -= nvaHeight
            drawRect(
                color = NvaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, nvaHeight)
            )
            
            currentX += barWidth * 2
        }
    }
}

@Composable
fun StationDetailContent(stationMetrics: StationYamazumiMetrics, viewModel: YamazumiViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${stationMetrics.station.name} Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.selectStation(null) }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Metrics Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            MetricBox("Station CT", String.format("%.2fs", stationMetrics.totalTime))
            MetricBox("VA %", String.format("%.1f%%", stationMetrics.vaPercent), VaColor)
            MetricBox("NVA %", String.format("%.1f%%", stationMetrics.nvaPercent), NvaColor)
            MetricBox("Idle Time", String.format("%.2fs", stationMetrics.idleTime))
            MetricBox("Utilization", String.format("%.1f%%", stationMetrics.utilization))
        }
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        
        Text("Work Elements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(stationMetrics.elements) { element ->
                ElementClassificationCard(element, viewModel)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ElementClassificationCard(element: WorkElement, viewModel: YamazumiViewModel) {
    var expanded by remember { mutableStateOf(false) }
    
    val color = when (element.valueClassification) {
        ValueClassification.VA -> VaColor
        ValueClassification.NNVA -> NnvaColor
        ValueClassification.NVA -> NvaColor
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${element.sequence}. ${element.name}", fontWeight = FontWeight.Bold)
                    Text("Time: ${String.format("%.2fs", element.standardTime)}", style = MaterialTheme.typography.labelMedium)
                }
                Badge(containerColor = color) { Text(element.valueClassification.name, color = Color.White, modifier = Modifier.padding(4.dp)) }
            }
            
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                
                var selectedClassification by remember { mutableStateOf(element.valueClassification) }
                var selectedWaste by remember { mutableStateOf(element.wasteCategory) }
                var reason by remember { mutableStateOf(element.classificationReason) }
                
                Text("Classification", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ValueClassification.entries.forEach { cls ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedClassification == cls, onClick = { selectedClassification = cls })
                            Text(cls.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                if (selectedClassification != ValueClassification.VA) {
                    Spacer(Modifier.height(8.dp))
                    Text("Waste Category", style = MaterialTheme.typography.labelSmall)
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WasteCategory.entries.filter { it != WasteCategory.NONE }.forEach { cat ->
                            FilterChip(
                                selected = selectedWaste == cat,
                                onClick = { selectedWaste = cat },
                                label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                } else {
                    selectedWaste = WasteCategory.NONE
                }
                
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Classification Reason (IE Override)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateElementClassification(element.id, selectedClassification, selectedWaste, reason) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Classification")
                }
            }
        }
    }
}
