package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun SpaghettiScreen(viewModel: SpaghettiViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    
    val selectedNodeId by viewModel.selectedNodeId.collectAsStateWithLifecycle()
    val selectedPathId by viewModel.selectedPathId.collectAsStateWithLifecycle()
    
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    
    val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    val activeState = scenarios.find { it.id == activeScenarioId }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = "Spaghetti", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Spaghetti Diagram", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isFuture = activeState?.isFuture == true
                    FilterChip(
                        selected = !isFuture,
                        onClick = { viewModel.switchScenario(false) },
                        label = { Text("Current Layout") }
                    )
                    FilterChip(
                        selected = isFuture,
                        onClick = { viewModel.switchScenario(true) },
                        label = { Text("Future Layout") }
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Button(onClick = { viewModel.analyzeLayout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyze")
                        Spacer(Modifier.width(4.dp))
                        Text("Analyze Layout")
                    }
                }
            }
        }

        // Metrics Bar
        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricItem("Distance / Cycle", String.format("%.1f m", metrics.distancePerCycle))
                MetricItem("Distance / Shift", String.format("%.1f m", metrics.distancePerShift))
                MetricItem("Total Trips", "${metrics.totalTrips}")
                MetricItem("Backtracking", "${metrics.backtrackingCount}")
                MetricItem("Cross Movements", "${metrics.crossMovementCount}")
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Toolbar
            SpaghettiToolbar(viewModel, modifier = Modifier.width(80.dp).fillMaxHeight())
            
            // Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFEBEBEB)) // Factory floor color
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { viewModel.selectNode(null) })
                    }
            ) {
                activeState?.let { state ->
                    SpaghettiEdgesCanvas(state.nodes, state.paths, selectedPathId, viewModel)
                    
                    state.nodes.forEach { node ->
                        SpaghettiNodeView(
                            node = node,
                            isSelected = node.id == selectedNodeId,
                            isConnectingTarget = isConnecting && node.id != viewModel.connectingSourceId.value,
                            onNodeClick = { viewModel.selectNode(node.id) },
                            onDrag = { dx, dy -> viewModel.moveNode(node.id, dx, dy) }
                        )
                    }
                }

                if (isConnecting) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Select target node to connect path", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(16.dp))
                            IconButton(onClick = { viewModel.cancelConnection() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        }
                    }
                }
            }

            // Right Properties Panel
            if (selectedNodeId != null) {
                val node = activeState?.nodes?.find { it.id == selectedNodeId }
                if (node != null) {
                    SpaghettiNodeProperties(node, viewModel, Modifier.width(300.dp).fillMaxHeight())
                }
            } else if (selectedPathId != null) {
                val path = activeState?.paths?.find { it.id == selectedPathId }
                if (path != null) {
                    SpaghettiPathProperties(path, viewModel, Modifier.width(300.dp).fillMaxHeight())
                }
            }
        }
    }

    if (isAnalyzing || aiAnalysis.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAnalysis() },
            icon = { Icon(Icons.Default.AutoAwesome, "AI Analysis") },
            title = { Text("Gemini Layout Optimization") },
            text = {
                if (isAnalyzing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Analyzing layout paths, backtracking, and distances...")
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            Text("Recommendations (Requires IE Validation):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                        }
                        items(aiAnalysis.size) { index ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(aiAnalysis[index], modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isAnalyzing) {
                    TextButton(onClick = { viewModel.closeAnalysis() }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
fun SpaghettiToolbar(viewModel: SpaghettiViewModel, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Layout", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            ToolbarItem(Icons.Default.Workspaces, "Station") { viewModel.addNode(SpaghettiNodeType.STATION) }
            ToolbarItem(Icons.Default.PrecisionManufacturing, "Machine") { viewModel.addNode(SpaghettiNodeType.MACHINE) }
            ToolbarItem(Icons.Default.TableChart, "Mat. Rack") { viewModel.addNode(SpaghettiNodeType.MATERIAL_RACK) }
            ToolbarItem(Icons.Default.Inventory, "WIP") { viewModel.addNode(SpaghettiNodeType.WIP) }
            ToolbarItem(Icons.Default.Build, "Tools") { viewModel.addNode(SpaghettiNodeType.TOOL) }
            ToolbarItem(Icons.Default.Person, "Operator") { viewModel.addNode(SpaghettiNodeType.OPERATOR) }
        }
    }
}

@Composable
fun SpaghettiEdgesCanvas(nodes: List<SpaghettiNode>, paths: List<SpaghettiPath>, selectedPathId: String?, viewModel: SpaghettiViewModel) {
    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(onTap = { offset ->
            // Simple hit detection for paths could go here, for now it just deselects
            viewModel.selectPath(null)
        })
    }) {
        paths.forEach { path ->
            val src = nodes.find { it.id == path.sourceId }
            val tgt = nodes.find { it.id == path.targetId }
            if (src != null && tgt != null) {
                // Determine center of node based on type roughly
                val srcOffset = Offset(src.x + 30f, src.y + 30f)
                val tgtOffset = Offset(tgt.x + 30f, tgt.y + 30f)
                
                val isSelected = path.id == selectedPathId
                val strokeWidth = if (isSelected) 6f else 3f
                
                val color = if (path.type == SpaghettiPathType.OPERATOR) Color(0xFFE53935) else Color(0xFF1E88E5)
                val pathEffect = if (path.type == SpaghettiPathType.MATERIAL) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null

                drawLine(
                    color = color,
                    start = srcOffset,
                    end = tgtOffset,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect
                )
                
                // Draw small circle in middle for selection target / visualization
                val midX = (srcOffset.x + tgtOffset.x) / 2
                val midY = (srcOffset.y + tgtOffset.y) / 2
                drawCircle(color = if (isSelected) Color.Black else color, radius = 8f, center = Offset(midX, midY))
            }
        }
    }
    
    // Transparent overlay to click paths. (In a real app, math is used to hit test lines. For simplicity, we can render clickable boxes at midpoints)
    Box(modifier = Modifier.fillMaxSize()) {
        paths.forEach { path ->
            val src = nodes.find { it.id == path.sourceId }
            val tgt = nodes.find { it.id == path.targetId }
            if (src != null && tgt != null) {
                val midX = (src.x + tgt.x) / 2 + 30f
                val midY = (src.y + tgt.y) / 2 + 30f
                Box(
                    modifier = Modifier
                        .offset { IntOffset(midX.roundToInt() - 15, midY.roundToInt() - 15) }
                        .size(30.dp)
                        .clickable { viewModel.selectPath(path.id) }
                )
            }
        }
    }
}

@Composable
fun SpaghettiNodeView(
    node: SpaghettiNode,
    isSelected: Boolean,
    isConnectingTarget: Boolean,
    onNodeClick: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onNodeClick() })
            }
    ) {
        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else if (isConnectingTarget) Color.Green else Color.Black
        val borderWidth = if (isSelected || isConnectingTarget) 3.dp else 1.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (node.type) {
                SpaghettiNodeType.STATION -> {
                    Box(modifier = Modifier.size(80.dp).background(Color.White).border(borderWidth, borderColor)) {
                        Text(node.name, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                    }
                }
                SpaghettiNodeType.MACHINE -> {
                    Box(modifier = Modifier.size(60.dp).background(Color.LightGray, CircleShape).border(borderWidth, borderColor, CircleShape)) {
                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall)
                }
                SpaghettiNodeType.MATERIAL_RACK -> {
                    Box(modifier = Modifier.width(80.dp).height(40.dp).background(Color(0xFFFFCC80)).border(borderWidth, borderColor)) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall)
                }
                SpaghettiNodeType.WIP -> {
                    Box(modifier = Modifier.width(60.dp).height(60.dp).background(Color(0xFF90CAF9)).border(borderWidth, borderColor))
                    Text(node.name, style = MaterialTheme.typography.labelSmall)
                }
                SpaghettiNodeType.TOOL -> {
                    Box(modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(8.dp)).border(borderWidth, borderColor, RoundedCornerShape(8.dp))) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.align(Alignment.Center), tint = Color.DarkGray)
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall)
                }
                SpaghettiNodeType.OPERATOR -> {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = borderColor)
                    Text(node.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SpaghettiNodeProperties(node: SpaghettiNode, viewModel: SpaghettiViewModel, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Node Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.deleteSelectedNode() }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = node.name,
                onValueChange = { viewModel.updateNodeName(node.id, it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            Text("Draw Path", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { viewModel.startConnection(SpaghettiPathType.OPERATOR) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Operator") }
                Button(onClick = { viewModel.startConnection(SpaghettiPathType.MATERIAL) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) { Text("Material") }
            }
        }
    }
}

@Composable
fun SpaghettiPathProperties(path: SpaghettiPath, viewModel: SpaghettiViewModel, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Path Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.deleteSelectedPath() }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Text("Type: ${path.type.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            var distText by remember(path.distanceMeters) { mutableStateOf(path.distanceMeters.toString()) }
            var tripText by remember(path.tripsPerCycle) { mutableStateOf(path.tripsPerCycle.toString()) }

            OutlinedTextField(
                value = distText,
                onValueChange = { distText = it },
                label = { Text("Distance (Meters)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tripText,
                onValueChange = { tripText = it },
                label = { Text("Trips per Cycle") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val d = distText.toDoubleOrNull() ?: path.distanceMeters
                    val t = tripText.toIntOrNull() ?: path.tripsPerCycle
                    viewModel.updatePath(path.id, d, t)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Path")
            }
        }
    }
}
