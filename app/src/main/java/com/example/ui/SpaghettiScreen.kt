package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.components.*
import com.example.ui.theme.*
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Top Bar
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = "Spaghetti", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Spaghetti Motion & Layout Mapping", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Operator travel path tracking, excessive walking, and layout bottleneck reduction", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isFuture = activeState?.isFuture == true
                    FilterChip(
                        selected = !isFuture,
                        onClick = { viewModel.switchScenario(false) },
                        label = { Text("Current Layout", style = IeTypography.badgeText) },
                        shape = IeRadius.badgeShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt100,
                            selectedLabelColor = StitchCobalt700
                        )
                    )
                    FilterChip(
                        selected = isFuture,
                        onClick = { viewModel.switchScenario(true) },
                        label = { Text("Future Kaizen", style = IeTypography.badgeText) },
                        shape = IeRadius.badgeShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt100,
                            selectedLabelColor = StitchCobalt700
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.analyzeLayout() },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyze", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Layout Review")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Metrics Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IeKpiCard(
                title = "Distance / Cycle",
                value = String.format("%.1f", metrics.distancePerCycle),
                unit = " m",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Distance / Shift",
                value = String.format("%.1f", metrics.distancePerShift),
                unit = " m",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Total Trips",
                value = "${metrics.totalTrips}",
                unit = " trips",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Backtracking Incidents",
                value = "${metrics.backtrackingCount}",
                isAlert = metrics.backtrackingCount > 0,
                subtitle = "Waste indicator",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Cross Movements",
                value = "${metrics.crossMovementCount}",
                isAlert = metrics.crossMovementCount > 0,
                subtitle = "Collision hazard",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Toolbar
            SpaghettiToolbar(viewModel, modifier = Modifier.width(90.dp).fillMaxHeight().padding(end = 10.dp))

            // Canvas Workspace
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StitchSlate50)
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
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            color = StitchCobalt700,
                            shape = IeRadius.cardShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select destination node to connect travel path", color = StitchWhite, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(12.dp))
                                IconButton(
                                    onClick = { viewModel.cancelConnection() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Cancel", tint = StitchWhite)
                                }
                            }
                        }
                    }
                }
            }

            // Right Properties Panel
            if (selectedNodeId != null) {
                val node = activeState?.nodes?.find { it.id == selectedNodeId }
                if (node != null) {
                    SpaghettiNodeProperties(node, viewModel, Modifier.width(320.dp).fillMaxHeight().padding(start = 10.dp))
                }
            } else if (selectedPathId != null) {
                val path = activeState?.paths?.find { it.id == selectedPathId }
                if (path != null) {
                    SpaghettiPathProperties(path, viewModel, Modifier.width(320.dp).fillMaxHeight().padding(start = 10.dp))
                }
            }
        }
    }

    if (isAnalyzing || aiAnalysis.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAnalysis() },
            shape = IeRadius.dialogShape,
            containerColor = StitchWhite,
            icon = { Icon(Icons.Default.AutoAwesome, "AI Analysis", tint = StitchCobalt600) },
            title = {
                Text(
                    "Layout Flow Optimization Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate900
                )
            },
            text = {
                if (isAnalyzing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator(color = StitchCobalt600, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Gemini analyzing routing distances, backtracking, and work cell geometry...", style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("Engineered Kaizen Recommendations:", style = IeTypography.tableHeader, color = StitchSlate600)
                        }
                        items(aiAnalysis.size) { index ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = IeRadius.cardShape,
                                border = BorderStroke(1.dp, StitchSlate200),
                                colors = CardDefaults.cardColors(containerColor = StitchSlate50),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    aiAnalysis[index],
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StitchSlate800
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isAnalyzing) {
                    Button(
                        onClick = { viewModel.closeAnalysis() },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun SpaghettiToolbar(viewModel: SpaghettiViewModel, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("LAYOUT", style = IeTypography.tableHeader, color = StitchSlate500, modifier = Modifier.padding(top = 4.dp))
            ToolbarItem(Icons.Default.Workspaces, "Station") { viewModel.addNode(SpaghettiNodeType.STATION) }
            ToolbarItem(Icons.Default.PrecisionManufacturing, "Machine") { viewModel.addNode(SpaghettiNodeType.MACHINE) }
            ToolbarItem(Icons.Default.TableChart, "Rack") { viewModel.addNode(SpaghettiNodeType.MATERIAL_RACK) }
            ToolbarItem(Icons.Default.Inventory, "WIP") { viewModel.addNode(SpaghettiNodeType.WIP) }
            ToolbarItem(Icons.Default.Build, "Tool") { viewModel.addNode(SpaghettiNodeType.TOOL) }
            ToolbarItem(Icons.Default.Person, "Operator") { viewModel.addNode(SpaghettiNodeType.OPERATOR) }
        }
    }
}

@Composable
fun SpaghettiEdgesCanvas(nodes: List<SpaghettiNode>, paths: List<SpaghettiPath>, selectedPathId: String?, viewModel: SpaghettiViewModel) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    viewModel.selectPath(null)
                })
            }
    ) {
        paths.forEach { path ->
            val src = nodes.find { it.id == path.sourceId }
            val tgt = nodes.find { it.id == path.targetId }
            if (src != null && tgt != null) {
                val srcOffset = Offset(src.x + 30f, src.y + 30f)
                val tgtOffset = Offset(tgt.x + 30f, tgt.y + 30f)

                val isSelected = path.id == selectedPathId
                val strokeWidth = if (isSelected) 5f else 2.5f

                val color = if (path.type == SpaghettiPathType.OPERATOR) StitchNvaRed else StitchCobalt600
                val pathEffect = if (path.type == SpaghettiPathType.MATERIAL) PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f) else null

                drawLine(
                    color = color,
                    start = srcOffset,
                    end = tgtOffset,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect
                )

                val midX = (srcOffset.x + tgtOffset.x) / 2
                val midY = (srcOffset.y + tgtOffset.y) / 2
                drawCircle(color = if (isSelected) StitchSlate900 else color, radius = 7f, center = Offset(midX, midY))
            }
        }
    }

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
        val borderColor = if (isSelected) StitchCobalt600 else if (isConnectingTarget) StitchVaGreen else StitchSlate300
        val borderWidth = if (isSelected || isConnectingTarget) 2.dp else 1.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (node.type) {
                SpaghettiNodeType.STATION -> {
                    Card(
                        modifier = Modifier.size(76.dp),
                        shape = IeRadius.cardShape,
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = StitchWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(node.name, textAlign = TextAlign.Center, style = IeTypography.tableHeader, color = StitchSlate900)
                        }
                    }
                }
                SpaghettiNodeType.MACHINE -> {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = StitchSlate100)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = StitchSlate700)
                        }
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall, color = StitchSlate800)
                }
                SpaghettiNodeType.MATERIAL_RACK -> {
                    Card(
                        modifier = Modifier.width(76.dp).height(38.dp),
                        shape = IeRadius.cardShape,
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = StitchNnvaAmberLight)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = StitchNnvaAmberText)
                        }
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall, color = StitchSlate800)
                }
                SpaghettiNodeType.WIP -> {
                    Card(
                        modifier = Modifier.size(54.dp),
                        shape = IeRadius.cardShape,
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = StitchCobalt100)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("WIP", style = IeTypography.tableHeader, color = StitchCobalt700)
                        }
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall, color = StitchSlate800)
                }
                SpaghettiNodeType.TOOL -> {
                    Card(
                        modifier = Modifier.size(42.dp),
                        shape = IeRadius.cardShape,
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = StitchWhite)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = StitchSlate700, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall, color = StitchSlate800)
                }
                SpaghettiNodeType.OPERATOR -> {
                    Surface(
                        shape = CircleShape,
                        color = StitchSlate100,
                        border = BorderStroke(borderWidth, borderColor),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(8.dp), tint = StitchSlate800)
                    }
                    Text(node.name, style = MaterialTheme.typography.labelSmall, color = StitchSlate800)
                }
            }
        }
    }
}

@Composable
fun SpaghettiNodeProperties(node: SpaghettiNode, viewModel: SpaghettiViewModel, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Layout Element", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
                IconButton(onClick = { viewModel.deleteSelectedNode() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = StitchNvaRed, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = node.name,
                onValueChange = { viewModel.updateNodeName(node.id, it) },
                label = { Text("Element Label") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = IeRadius.inputShape
            )

            Spacer(Modifier.height(14.dp))
            Text("CONNECT TRAVEL PATH", style = IeTypography.tableHeader, color = StitchSlate500)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.startConnection(SpaghettiPathType.OPERATOR) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Text("Operator", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { viewModel.startConnection(SpaghettiPathType.MATERIAL) },
                    modifier = Modifier.weight(1f),
                    shape = IeRadius.buttonShape,
                    border = BorderStroke(1.dp, StitchCobalt600)
                ) {
                    Text("Material", color = StitchCobalt700, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SpaghettiPathProperties(path: SpaghettiPath, viewModel: SpaghettiViewModel, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Path Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
                IconButton(onClick = { viewModel.deleteSelectedPath() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = StitchNvaRed, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Path Classification: ", style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
                IeBadge(
                    text = path.type.name,
                    variant = if (path.type == SpaghettiPathType.OPERATOR) IeBadgeVariant.ERROR else IeBadgeVariant.PRIMARY
                )
            }
            Spacer(Modifier.height(14.dp))

            var distText by remember(path.distanceMeters) { mutableStateOf(path.distanceMeters.toString()) }
            var tripText by remember(path.tripsPerCycle) { mutableStateOf(path.tripsPerCycle.toString()) }

            OutlinedTextField(
                value = distText,
                onValueChange = { distText = it },
                label = { Text("Physical Distance (Meters)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                textStyle = IeTypography.dataMono,
                shape = IeRadius.inputShape
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = tripText,
                onValueChange = { tripText = it },
                label = { Text("Frequency (Trips per Cycle)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                textStyle = IeTypography.dataMono,
                shape = IeRadius.inputShape
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val d = distText.toDoubleOrNull() ?: path.distanceMeters
                    val t = tripText.toIntOrNull() ?: path.tripsPerCycle
                    viewModel.updatePath(path.id, d, t)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                shape = IeRadius.buttonShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Path Metrics")
            }
        }
    }
}
