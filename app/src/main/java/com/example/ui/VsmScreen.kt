package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Station
import kotlin.math.roundToInt

@Composable
fun VsmScreen(viewModel: VsmViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val selectedNodeId by viewModel.selectedNodeId.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val availableStations = viewModel.availableStations

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
                    Icon(Icons.Default.AccountTree, contentDescription = "VSM", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Value Stream Mapping", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isFuture = activeState?.isFutureState == true
                    FilterChip(
                        selected = !isFuture,
                        onClick = { viewModel.switchScenario(false) },
                        label = { Text("Current State") }
                    )
                    FilterChip(
                        selected = isFuture,
                        onClick = { viewModel.switchScenario(true) },
                        label = { Text("Future State") }
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    OutlinedButton(onClick = { /* Export */ }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        Spacer(Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
        }

        // Metrics Bar
        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                VsmMetricItem("Lead Time", String.format("%.1f Days", metrics.totalLeadTimeDays))
                VsmMetricItem("Process (VA) Time", String.format("%.1f Sec", metrics.totalVaTimeSec))
                VsmMetricItem("PCE", String.format("%.2f %%", metrics.pce))
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Toolbar
            VsmToolbar(viewModel, modifier = Modifier.width(80.dp).fillMaxHeight())
            
            // Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF8F9FA))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { viewModel.selectNode(null) })
                    }
            ) {
                activeState?.let { state ->
                    VsmEdgesCanvas(state.nodes, state.edges)
                    
                    state.nodes.forEach { node ->
                        VsmNodeView(
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
                            Text("Select target node to connect", color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                val selectedNode = activeState?.nodes?.find { it.id == selectedNodeId }
                if (selectedNode != null) {
                    VsmPropertiesPanel(
                        node = selectedNode,
                        availableStations = availableStations,
                        viewModel = viewModel,
                        modifier = Modifier.width(300.dp).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun VsmMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun VsmToolbar(viewModel: VsmViewModel, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nodes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            ToolbarItem(Icons.Default.Factory, "Process") { viewModel.addNode(VsmNodeType.PROCESS) }
            ToolbarItem(Icons.Default.ChangeHistory, "Inventory") { viewModel.addNode(VsmNodeType.INVENTORY) }
            ToolbarItem(Icons.Default.Business, "Supplier/Customer") { viewModel.addNode(VsmNodeType.SUPPLIER) }
            ToolbarItem(Icons.Default.Dns, "Supermarket") { viewModel.addNode(VsmNodeType.SUPERMARKET) }
            ToolbarItem(Icons.Default.CropPortrait, "Kanban") { viewModel.addNode(VsmNodeType.KANBAN) }
            ToolbarItem(Icons.Default.Computer, "Control") { viewModel.addNode(VsmNodeType.PROD_CONTROL) }
        }
    }
}

@Composable
fun ToolbarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun VsmEdgesCanvas(nodes: List<VsmNode>, edges: List<VsmEdge>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        edges.forEach { edge ->
            val src = nodes.find { it.id == edge.sourceId }
            val tgt = nodes.find { it.id == edge.targetId }
            if (src != null && tgt != null) {
                val start = Offset(src.x + 60f, src.y + 40f) // Approximate centers
                val end = Offset(tgt.x + 60f, tgt.y + 40f)
                
                when (edge.type) {
                    VsmEdgeType.MATERIAL -> {
                        drawLine(color = Color.Black, start = start, end = end, strokeWidth = 3f)
                        drawArrowHead(start, end, Color.Black)
                    }
                    VsmEdgeType.INFORMATION -> {
                        drawLine(
                            color = Color.Blue, 
                            start = start, 
                            end = end, 
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawArrowHead(start, end, Color.Blue)
                    }
                    VsmEdgeType.FIFO -> {
                        drawLine(color = Color.DarkGray, start = start, end = end, strokeWidth = 4f)
                        // Simple cross hatch simulation for FIFO
                    }
                    VsmEdgeType.SHIPMENT -> {
                        drawLine(color = Color.Black, start = start, end = end, strokeWidth = 5f)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color) {
    // Simplified arrowhead
    val path = Path()
    val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowSize = 15f
    val arrowAngle = kotlin.math.PI / 6

    val x1 = end.x - arrowSize * kotlin.math.cos(angle - arrowAngle).toFloat()
    val y1 = end.y - arrowSize * kotlin.math.sin(angle - arrowAngle).toFloat()
    val x2 = end.x - arrowSize * kotlin.math.cos(angle + arrowAngle).toFloat()
    val y2 = end.y - arrowSize * kotlin.math.sin(angle + arrowAngle).toFloat()

    path.moveTo(end.x, end.y)
    path.lineTo(x1, y1)
    path.lineTo(x2, y2)
    path.close()
    drawPath(path, color)
}

@Composable
fun VsmNodeView(
    node: VsmNode,
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

        when (node.type) {
            VsmNodeType.PROCESS -> {
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .background(Color.White)
                        .border(borderWidth, borderColor)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(4.dp)) {
                        Text(node.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider(color = Color.Black)
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text("CT: ${node.cycleTime}s", fontSize = 10.sp)
                        Text("Up: ${node.uptime}%", fontSize = 10.sp)
                        Text("Op: ${node.operators}", fontSize = 10.sp)
                    }
                }
            }
            VsmNodeType.INVENTORY -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChangeHistory, contentDescription = "Inventory", modifier = Modifier.size(48.dp), tint = borderColor)
                    Text("${node.wip} / ${node.leadTimeDays}d", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            VsmNodeType.SUPPLIER, VsmNodeType.CUSTOMER -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.background(Color.White).border(borderWidth, borderColor).padding(8.dp)
                ) {
                    Icon(Icons.Default.Business, contentDescription = "Factory", tint = Color.DarkGray)
                    Text(node.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            VsmNodeType.PROD_CONTROL -> {
                Box(
                    modifier = Modifier.background(Color.White).border(borderWidth, borderColor).padding(16.dp)
                ) {
                    Text(node.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            VsmNodeType.SUPERMARKET -> {
                Box(
                    modifier = Modifier.width(60.dp).height(40.dp).border(borderWidth, borderColor).background(Color.White)
                ) {
                    Text("Super\nMarket", fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                }
            }
            VsmNodeType.KANBAN -> {
                Box(
                    modifier = Modifier.width(30.dp).height(40.dp).background(Color.Yellow).border(borderWidth, borderColor)
                ) {
                    Text("K", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun VsmPropertiesPanel(
    node: VsmNode,
    availableStations: List<Station>,
    viewModel: VsmViewModel,
    modifier: Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { viewModel.duplicateSelectedNode() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = { viewModel.deleteSelectedNode() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Connections
            Text("Connect Node", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { viewModel.startConnection(VsmEdgeType.MATERIAL) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Material") }
                Button(onClick = { viewModel.startConnection(VsmEdgeType.INFORMATION) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Info") }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            OutlinedTextField(
                value = node.name,
                onValueChange = { n -> viewModel.updateSelectedNode { copy(name = n) } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            if (node.type == VsmNodeType.PROCESS) {
                Spacer(Modifier.height(12.dp))
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (node.linkedStationId != null) "Linked to Station" else "Link to Station data...")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.linkStationToSelectedNode(null); expanded = false })
                        availableStations.forEach { st ->
                            DropdownMenuItem(text = { Text(st.name) }, onClick = { viewModel.linkStationToSelectedNode(st.id); expanded = false })
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                NumberField("Cycle Time (s)", node.cycleTime) { v -> viewModel.updateSelectedNode { copy(cycleTime = v) } }
                NumberField("VA Time (s)", node.vaTimeSec) { v -> viewModel.updateSelectedNode { copy(vaTimeSec = v) } }
                NumberField("NNVA Time (s)", node.nnvaTimeSec) { v -> viewModel.updateSelectedNode { copy(nnvaTimeSec = v) } }
                NumberField("NVA Time (s)", node.nvaTimeSec) { v -> viewModel.updateSelectedNode { copy(nvaTimeSec = v) } }
                NumberField("Uptime (%)", node.uptime) { v -> viewModel.updateSelectedNode { copy(uptime = v) } }
                NumberField("Operators", node.operators.toDouble()) { v -> viewModel.updateSelectedNode { copy(operators = v.toInt()) } }
            }

            if (node.type == VsmNodeType.INVENTORY) {
                Spacer(Modifier.height(12.dp))
                NumberField("WIP (Units)", node.wip) { v -> viewModel.updateSelectedNode { copy(wip = v) } }
                NumberField("Lead Time (Days)", node.leadTimeDays) { v -> viewModel.updateSelectedNode { copy(leadTimeDays = v) } }
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = { onValueChange(it.toDoubleOrNull() ?: 0.0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}
