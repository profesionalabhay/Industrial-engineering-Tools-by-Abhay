package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun VsmScreen(viewModel: VsmViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val selectedNodeId by viewModel.selectedNodeId.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val availableStations by viewModel.availableStations.collectAsStateWithLifecycle()

    val activeState = scenarios.find { it.id == activeScenarioId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Top Bar & State Selector
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = "VSM", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Value Stream Mapping Canvas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Material and information flow modeling (Current vs. Future State)", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isFuture = activeState?.isFutureState == true
                    FilterChip(
                        selected = !isFuture,
                        onClick = { viewModel.switchScenario(false) },
                        label = { Text("Current State", style = IeTypography.badgeText) },
                        shape = IeRadius.badgeShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt100,
                            selectedLabelColor = StitchCobalt700
                        )
                    )
                    FilterChip(
                        selected = isFuture,
                        onClick = { viewModel.switchScenario(true) },
                        label = { Text("Future State", style = IeTypography.badgeText) },
                        shape = IeRadius.badgeShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt100,
                            selectedLabelColor = StitchCobalt700
                        )
                    )

                    Spacer(Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { /* Export */ },
                        shape = IeRadius.buttonShape,
                        border = BorderStroke(1.dp, StitchSlate300)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", modifier = Modifier.size(16.dp), tint = StitchSlate700)
                        Spacer(Modifier.width(4.dp))
                        Text("Export VSM", color = StitchSlate800)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // VSM Metrics Banner (Production Lead Time, Processing Time, PCE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IeKpiCard(
                title = "Total Production Lead Time",
                value = String.format("%.1f", metrics.totalLeadTimeDays),
                unit = " days",
                subtitle = "Sum of inventory queue times",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Total Processing (VA) Time",
                value = String.format("%.1f", metrics.totalVaTimeSec),
                unit = " s",
                subtitle = "Value-added work duration",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Process Cycle Efficiency (PCE)",
                value = String.format("%.2f", metrics.pce),
                unit = "%",
                trend = "VA / PLT",
                isPositiveTrend = metrics.pce > 5.0,
                subtitle = "Industrial benchmark: 5-15%",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Main Diagram Canvas & Toolbars
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Toolbar (Palette)
            VsmToolbar(viewModel, modifier = Modifier.width(90.dp).fillMaxHeight().padding(end = 10.dp))

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
                                Text("Select destination node to connect flow", color = StitchWhite, style = MaterialTheme.typography.bodyMedium)
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
                val selectedNode = activeState?.nodes?.find { it.id == selectedNodeId }
                if (selectedNode != null) {
                    VsmPropertiesPanel(
                        node = selectedNode,
                        availableStations = availableStations,
                        viewModel = viewModel,
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VsmToolbar(viewModel: VsmViewModel, modifier: Modifier) {
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
            Text("PALETTE", style = IeTypography.tableHeader, color = StitchSlate500, modifier = Modifier.padding(top = 4.dp))
            ToolbarItem(Icons.Default.Factory, "Process") { viewModel.addNode(VsmNodeType.PROCESS) }
            ToolbarItem(Icons.Default.ChangeHistory, "Inventory") { viewModel.addNode(VsmNodeType.INVENTORY) }
            ToolbarItem(Icons.Default.Business, "Supplier") { viewModel.addNode(VsmNodeType.SUPPLIER) }
            ToolbarItem(Icons.Default.Dns, "Supermkt") { viewModel.addNode(VsmNodeType.SUPERMARKET) }
            ToolbarItem(Icons.Default.CropPortrait, "Kanban") { viewModel.addNode(VsmNodeType.KANBAN) }
            ToolbarItem(Icons.Default.Computer, "Control") { viewModel.addNode(VsmNodeType.PROD_CONTROL) }
        }
    }
}

@Composable
fun ToolbarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = IeRadius.cardShape,
            color = StitchSlate100,
            border = BorderStroke(1.dp, StitchSlate200),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.padding(10.dp), tint = StitchSlate700)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = StitchSlate700
        )
    }
}

@Composable
fun VsmEdgesCanvas(nodes: List<VsmNode>, edges: List<VsmEdge>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        edges.forEach { edge ->
            val src = nodes.find { it.id == edge.sourceId }
            val tgt = nodes.find { it.id == edge.targetId }
            if (src != null && tgt != null) {
                val start = Offset(src.x + 60f, src.y + 40f)
                val end = Offset(tgt.x + 60f, tgt.y + 40f)

                when (edge.type) {
                    VsmEdgeType.MATERIAL -> {
                        drawLine(color = StitchSlate800, start = start, end = end, strokeWidth = 3f)
                        drawArrowHead(start, end, StitchSlate800)
                    }
                    VsmEdgeType.INFORMATION -> {
                        drawLine(
                            color = StitchCobalt600,
                            start = start,
                            end = end,
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                        drawArrowHead(start, end, StitchCobalt600)
                    }
                    VsmEdgeType.FIFO -> {
                        drawLine(color = StitchSlate600, start = start, end = end, strokeWidth = 4f)
                    }
                    VsmEdgeType.SHIPMENT -> {
                        drawLine(color = StitchSlate900, start = start, end = end, strokeWidth = 5f)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color) {
    val path = Path()
    val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowSize = 14f
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
        val borderColor = if (isSelected) StitchCobalt600 else if (isConnectingTarget) StitchVaGreen else StitchSlate300
        val borderWidth = if (isSelected || isConnectingTarget) 2.dp else 1.dp

        when (node.type) {
            VsmNodeType.PROCESS -> {
                Card(
                    modifier = Modifier.width(130.dp),
                    shape = IeRadius.cardShape,
                    border = BorderStroke(borderWidth, borderColor),
                    colors = CardDefaults.cardColors(containerColor = StitchWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StitchSlate100)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                node.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = StitchSlate900,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider(color = StitchSlate200)
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("CT: ${node.cycleTime}s", style = IeTypography.dataMono, fontSize = 10.sp, color = StitchSlate800)
                            Text("VA: ${node.vaTimeSec}s", style = IeTypography.dataMono, fontSize = 10.sp, color = StitchVaGreenText)
                            Text("Up: ${node.uptime}%", style = IeTypography.dataMono, fontSize = 10.sp, color = StitchSlate600)
                            Text("Operators: ${node.operators}", style = IeTypography.dataMono, fontSize = 10.sp, color = StitchSlate600)
                        }
                    }
                }
            }
            VsmNodeType.INVENTORY -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ChangeHistory,
                        contentDescription = "Inventory",
                        modifier = Modifier.size(44.dp),
                        tint = if (isSelected) StitchCobalt600 else StitchSlate700
                    )
                    Text(
                        "${node.wip.toInt()} pcs | ${node.leadTimeDays}d",
                        style = IeTypography.dataMonoBold,
                        fontSize = 10.sp,
                        color = StitchSlate900
                    )
                }
            }
            VsmNodeType.SUPPLIER, VsmNodeType.CUSTOMER -> {
                Card(
                    shape = IeRadius.cardShape,
                    border = BorderStroke(borderWidth, borderColor),
                    colors = CardDefaults.cardColors(containerColor = StitchWhite)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.Business, contentDescription = "Entity", tint = StitchSlate700, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(node.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StitchSlate900)
                    }
                }
            }
            VsmNodeType.PROD_CONTROL -> {
                Card(
                    shape = IeRadius.cardShape,
                    border = BorderStroke(borderWidth, borderColor),
                    colors = CardDefaults.cardColors(containerColor = StitchSlate50)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(node.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = StitchSlate900)
                    }
                }
            }
            VsmNodeType.SUPERMARKET -> {
                Card(
                    shape = IeRadius.cardShape,
                    border = BorderStroke(borderWidth, borderColor),
                    colors = CardDefaults.cardColors(containerColor = StitchWhite)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SUPER\nMARKET", style = IeTypography.tableHeader, fontSize = 9.sp, textAlign = TextAlign.Center, color = StitchSlate800)
                    }
                }
            }
            VsmNodeType.KANBAN -> {
                Card(
                    shape = IeRadius.badgeShape,
                    border = BorderStroke(borderWidth, borderColor),
                    colors = CardDefaults.cardColors(containerColor = StitchNnvaAmberLight)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StitchNnvaAmberText)
                    }
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
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Element Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
                Row {
                    IconButton(onClick = { viewModel.duplicateSelectedNode() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = StitchSlate600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.deleteSelectedNode() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StitchNvaRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(10.dp))

            Text("CONNECT FLOW", style = IeTypography.tableHeader, color = StitchSlate500)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.startConnection(VsmEdgeType.MATERIAL) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Text("Material", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { viewModel.startConnection(VsmEdgeType.INFORMATION) },
                    modifier = Modifier.weight(1f),
                    shape = IeRadius.buttonShape,
                    border = BorderStroke(1.dp, StitchCobalt600)
                ) {
                    Text("Info", color = StitchCobalt700, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = StitchSlate200)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = node.name,
                onValueChange = { n -> viewModel.updateSelectedNode { copy(name = n) } },
                label = { Text("Node Label") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = IeRadius.inputShape
            )

            if (node.type == VsmNodeType.PROCESS) {
                Spacer(Modifier.height(10.dp))
                var expanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = IeRadius.buttonShape,
                        border = BorderStroke(1.dp, StitchSlate300)
                    ) {
                        Text(
                            if (node.linkedStationId != null) "Linked Station Selected" else "Link to Physical Station...",
                            color = StitchSlate800
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("None (Manual Parameters)") },
                            onClick = { viewModel.linkStationToSelectedNode(null); expanded = false }
                        )
                        availableStations.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.name) },
                                onClick = { viewModel.linkStationToSelectedNode(st.id); expanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                NumberField("Cycle Time (s)", node.cycleTime) { v -> viewModel.updateSelectedNode { copy(cycleTime = v) } }
                NumberField("VA Time (s)", node.vaTimeSec) { v -> viewModel.updateSelectedNode { copy(vaTimeSec = v) } }
                NumberField("NNVA Time (s)", node.nnvaTimeSec) { v -> viewModel.updateSelectedNode { copy(nnvaTimeSec = v) } }
                NumberField("NVA Time (s)", node.nvaTimeSec) { v -> viewModel.updateSelectedNode { copy(nvaTimeSec = v) } }
                NumberField("Uptime (%)", node.uptime) { v -> viewModel.updateSelectedNode { copy(uptime = v) } }
                NumberField("Assigned Operators", node.operators.toDouble()) { v -> viewModel.updateSelectedNode { copy(operators = v.toInt()) } }
            }

            if (node.type == VsmNodeType.INVENTORY) {
                Spacer(Modifier.height(10.dp))
                NumberField("WIP Queue (Units)", node.wip) { v -> viewModel.updateSelectedNode { copy(wip = v) } }
                NumberField("Lead Time (Days)", node.leadTimeDays) { v -> viewModel.updateSelectedNode { copy(leadTimeDays = v) } }
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    var text by remember(value) {
        mutableStateOf(if (value == 0.0) "" else if (value % 1.0 == 0.0) value.toInt().toString() else value.toString())
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let { v -> onValueChange(v) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        textStyle = IeTypography.dataMono,
        shape = IeRadius.inputShape
    )
}
