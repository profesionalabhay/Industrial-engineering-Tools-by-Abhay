package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

// Deprecated local aliases pointing to central design tokens for safety
val VaColor = IeLeanTokens.vaColor
val NnvaColor = IeLeanTokens.nnvaColor
val NvaColor = IeLeanTokens.nvaColor
val TaktColor = IeLeanTokens.taktColor

@Composable
fun YamazumiScreen(viewModel: YamazumiViewModel, modifier: Modifier = Modifier) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val taktTime by viewModel.taktTime.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val activePlan by viewModel.activePlan.collectAsStateWithLifecycle()

    if (metrics.isEmpty()) {
        IeEmptyState(
            title = "Yamazumi Chart",
            message = "No stations or work elements available to balance for this line."
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Engineering Header
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Yamazumi Station Load Balance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Spacer(Modifier.height(4.dp))
                        if (taktTime == null) {
                            Text(
                                "Takt time reference is required for load balance compliance.",
                                color = StitchNvaRedText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Target Takt: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StitchSlate600
                                )
                                Text(
                                    String.format("%.2fs", taktTime),
                                    style = IeTypography.dataMonoBold,
                                    color = StitchCobalt700
                                )
                                if (activePlan != null) {
                                    Text(
                                        "  •  Plan: ${activePlan?.name} (${activePlan?.plannedTotalQuantity} pcs)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StitchSlate500
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { /* Compare Scenarios */ },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                            shape = IeRadius.buttonShape,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scenarios", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = { /* Export */ }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Yamazumi", tint = StitchSlate600)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(Modifier.height(10.dp))

                // Multi-Model Mode Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ANALYSIS MODE:",
                        style = IeTypography.tableHeader,
                        color = StitchSlate500
                    )

                    FilterChip(
                        selected = selectedMode == YamazumiMode.MIXED_WEIGHTED,
                        onClick = { viewModel.setMode(YamazumiMode.MIXED_WEIGHTED) },
                        label = { Text("Mixed Weighted Average", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt700,
                            selectedLabelColor = StitchWhite
                        )
                    )

                    FilterChip(
                        selected = selectedMode == YamazumiMode.MAXIMUM_LOAD,
                        onClick = { viewModel.setMode(YamazumiMode.MAXIMUM_LOAD) },
                        label = { Text("Maximum Workload Peak", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt700,
                            selectedLabelColor = StitchWhite
                        )
                    )

                    FilterChip(
                        selected = selectedMode == YamazumiMode.MODEL_SPECIFIC,
                        onClick = { viewModel.setMode(YamazumiMode.MODEL_SPECIFIC) },
                        label = { Text("Model Specific", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt700,
                            selectedLabelColor = StitchWhite
                        )
                    )
                }

                // If Model Specific mode is selected, show models
                if (selectedMode == YamazumiMode.MODEL_SPECIFIC && availableModels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("SELECT MODEL:", style = IeTypography.tableHeader, color = StitchSlate500)
                        availableModels.forEach { model ->
                            val isSelected = selectedModelId == model.id || (selectedModelId == null && model == availableModels.first())
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectModel(model.id) },
                                label = { Text("${model.name} (${model.variant})", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StitchSlate900,
                                    selectedLabelColor = StitchWhite
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Yamazumi Stacked Chart Container
        IeChartContainer(
            title = "Station Work Load vs. Takt Time",
            subtitle = "Tap a station bar to inspect element breakdown and classification",
            taktTime = taktTime,
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            legendContent = {
                IeLegendItem("VA", IeLeanTokens.vaColor)
                IeLegendItem("NNVA", IeLeanTokens.nnvaColor)
                IeLegendItem("NVA", IeLeanTokens.nvaColor)
                IeLegendItem("Takt Line", IeLeanTokens.taktColor, isDashed = true)
            }
        ) {
            YamazumiChart(
                metrics = metrics,
                taktTime = taktTime,
                selectedStationId = selectedStation?.station?.id
            ) { stationId ->
                viewModel.selectStation(stationId)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Station Details & Classification Editor
        IeCard(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
        ) {
            if (selectedStation == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = StitchSlate400,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select a station column from the chart above to analyze and classify elements.",
                            color = StitchSlate500,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                StationDetailContent(selectedStation!!, viewModel)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        IeReportBranding()
    }
}

@Composable
fun YamazumiChart(
    metrics: List<StationYamazumiMetrics>,
    taktTime: Double?,
    selectedStationId: String?,
    onStationClick: (String) -> Unit
) {
    val maxTime = maxOf(
        taktTime ?: 0.0,
        metrics.maxOfOrNull { it.totalTime } ?: 1.0
    ) * 1.25 // 25% headroom for clean visual framing and top value labels

    val stationCount = metrics.size

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(metrics) {
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
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val bottomLabelHeight = 24.dp.toPx()
        val chartHeight = canvasHeight - bottomLabelHeight

        val barWidth = canvasWidth / (stationCount * 2)
        val padding = barWidth / 2
        var currentX = padding

        // Grid lines at 25%, 50%, 75%
        val gridColor = StitchSlate200
        val gridTextPaint = Paint().apply {
            color = android.graphics.Color.rgb(148, 163, 184) // Slate 400
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
        val taktPaint = Paint().apply {
            color = android.graphics.Color.rgb(99, 102, 241) // Indigo 500
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
            typeface = Typeface.MONOSPACE
        }
        val stationLabelPaint = Paint().apply {
            color = android.graphics.Color.rgb(51, 65, 85) // Slate 700
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val timeLabelPaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42) // Slate 900
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        val overTaktTimePaint = Paint().apply {
            color = android.graphics.Color.rgb(225, 29, 72) // Rose / NVA Red
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        // Draw Baseline Axis
        drawLine(
            color = StitchSlate300,
            start = Offset(0f, chartHeight),
            end = Offset(canvasWidth, chartHeight),
            strokeWidth = 2f
        )

        for (i in 1..3) {
            val gridY = chartHeight * (i / 4f)
            val timeValue = maxTime * (1f - i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(canvasWidth, gridY),
                strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0fs", timeValue),
                8f,
                gridY - 4f,
                gridTextPaint
            )
        }

        // Takt Reference Line (Dashed Indigo)
        if (taktTime != null) {
            val taktY = chartHeight - ((taktTime / maxTime) * chartHeight).toFloat()
            drawLine(
                color = IeLeanTokens.taktColor,
                start = Offset(0f, taktY),
                end = Offset(canvasWidth, taktY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "TAKT: ${String.format("%.1fs", taktTime)}",
                canvasWidth - 170f,
                taktY - 6f,
                taktPaint
            )
        }

        // Render Bars
        metrics.forEach { metric ->
            val isSelected = metric.station.id == selectedStationId
            val isOverTakt = taktTime != null && metric.totalTime > taktTime

            val vaHeight = ((metric.vaTime / maxTime) * chartHeight).toFloat()
            val nnvaHeight = ((metric.nnvaTime / maxTime) * chartHeight).toFloat()
            val nvaHeight = ((metric.nvaTime / maxTime) * chartHeight).toFloat()

            var currentY = chartHeight
            val barCenterX = currentX + barWidth / 2

            // VA Segment (Bottom)
            currentY -= vaHeight
            drawRect(
                color = IeLeanTokens.vaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, vaHeight)
            )

            // NNVA Segment (Middle)
            currentY -= nnvaHeight
            drawRect(
                color = IeLeanTokens.nnvaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, nnvaHeight)
            )

            // NVA Segment (Top)
            currentY -= nvaHeight
            drawRect(
                color = IeLeanTokens.nvaColor,
                topLeft = Offset(currentX, currentY),
                size = Size(barWidth, nvaHeight)
            )

            // Highlight border if selected or over takt
            if (isSelected) {
                val totalBarHeight = vaHeight + nnvaHeight + nvaHeight
                drawRect(
                    color = StitchCobalt700,
                    topLeft = Offset(currentX - 2f, currentY - 2f),
                    size = Size(barWidth + 4f, totalBarHeight + 2f),
                    style = Stroke(width = 3f)
                )
            } else if (isOverTakt) {
                val totalBarHeight = vaHeight + nnvaHeight + nvaHeight
                drawRect(
                    color = StitchNvaRed,
                    topLeft = Offset(currentX, currentY),
                    size = Size(barWidth, totalBarHeight),
                    style = Stroke(width = 1.5f)
                )
            }

            // Draw Total Cycle Time on top of bar
            val totalTimeText = String.format("%.1fs", metric.totalTime)
            val textPaint = if (isOverTakt) overTaktTimePaint else timeLabelPaint
            drawContext.canvas.nativeCanvas.drawText(
                totalTimeText,
                barCenterX,
                currentY - 8f,
                textPaint
            )

            // Draw Station Name below baseline
            val stationName = if (metric.station.name.length > 8) metric.station.name.take(7) + "…" else metric.station.name
            drawContext.canvas.nativeCanvas.drawText(
                stationName,
                barCenterX,
                chartHeight + 20.dp.toPx(),
                stationLabelPaint
            )

            currentX += barWidth * 2
        }
    }
}

@Composable
fun StationDetailContent(stationMetrics: StationYamazumiMetrics, viewModel: YamazumiViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stationMetrics.station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate900
                )
                Spacer(Modifier.width(8.dp))
                IeBadge(
                    text = "${stationMetrics.elements.size} Elements",
                    variant = IeBadgeVariant.PRIMARY
                )
            }
            IconButton(
                onClick = { viewModel.selectStation(null) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = StitchSlate500)
            }
        }

        Spacer(Modifier.height(10.dp))

        // High-density KPI summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IeKpiCard(
                title = "Weighted CT",
                value = String.format("%.2f", stationMetrics.weightedCt),
                unit = "s",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Peak CT",
                value = String.format("%.2f", stationMetrics.maxCt),
                unit = "s",
                trend = stationMetrics.maxModelName.take(8),
                isPositiveTrend = false,
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Idle Time",
                value = String.format("%.2f", stationMetrics.idleTime),
                unit = "s",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Utilization",
                value = String.format("%.1f", stationMetrics.utilization),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "VA / NNVA / NVA",
                value = String.format("%.0f/%.0f/%.0f", stationMetrics.vaPercent, stationMetrics.nnvaPercent, stationMetrics.nvaPercent),
                unit = "%",
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Model Breakdown Pills
        if (stationMetrics.modelBreakdowns.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "MODEL BREAKDOWN:",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                stationMetrics.modelBreakdowns.values.forEach { mb ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StitchSlate100,
                        border = BorderStroke(1.dp, StitchSlate200)
                    ) {
                        Text(
                            text = "${mb.modelName}: ${String.format("%.1fs", mb.totalTime)} (${mb.applicableElements.size} els)",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = IeTypography.dataMono,
                            fontSize = 11.sp,
                            color = StitchSlate800
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(color = StitchSlate200)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "WORK ELEMENTS BREAKDOWN",
            style = IeTypography.tableHeader,
            color = StitchSlate500
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stationMetrics.elements) { element ->
                ElementClassificationCard(element, viewModel)
            }
        }
    }
}

@Composable
fun ElementClassificationCard(element: WorkElement, viewModel: YamazumiViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${element.sequence}. ${element.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchSlate900
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Standard Time: ${String.format("%.2fs", element.standardTime)}",
                        style = IeTypography.dataMono,
                        color = StitchSlate600
                    )
                }
                IeClassificationBadge(
                    valueClassification = element.valueClassification,
                    timeText = String.format("%.1fs", element.standardTime)
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(Modifier.height(10.dp))

                var selectedClassification by remember { mutableStateOf(element.valueClassification) }
                var selectedWaste by remember { mutableStateOf(element.wasteCategory) }
                var reason by remember { mutableStateOf(element.classificationReason) }

                Text("Classification", style = IeTypography.tableHeader, color = StitchSlate500)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ValueClassification.entries.forEach { cls ->
                        val isSelected = selectedClassification == cls
                        val color = IeLeanTokens.getColorForValueClassification(cls)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedClassification = cls },
                            label = { Text(cls.name, style = IeTypography.badgeText) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            ),
                            shape = IeRadius.badgeShape
                        )
                    }
                }

                if (selectedClassification != ValueClassification.VA) {
                    Spacer(Modifier.height(8.dp))
                    Text("Waste Category (Muda)", style = IeTypography.tableHeader, color = StitchSlate500)

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WasteCategory.entries.filter { it != WasteCategory.NONE }.forEach { cat ->
                            FilterChip(
                                selected = selectedWaste == cat,
                                onClick = { selectedWaste = cat },
                                label = {
                                    Text(
                                        cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                shape = IeRadius.badgeShape
                            )
                        }
                    }
                } else {
                    selectedWaste = WasteCategory.NONE
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Classification Reason / Engineering Justification") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = IeRadius.inputShape
                )

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.updateElementClassification(element, selectedClassification, selectedWaste, reason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Classification", color = StitchWhite)
                }
            }
        }
    }
}
