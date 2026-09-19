package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun TimeStudyScreen(viewModel: TimeStudyViewModel, modifier: Modifier = Modifier) {
    val workElements by viewModel.workElements.collectAsStateWithLifecycle()
    val selectedElement by viewModel.selectedElement.collectAsStateWithLifecycle()

    if (workElements.isEmpty()) {
        IeEmptyState(
            title = "Observation Table & Time Study",
            message = "No work elements recorded for this study. Import video or add manual elements to begin time measurement."
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Left Panel: Work Elements Register
        IeCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Work Elements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Text(
                            "${workElements.size} operational steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchSlate500
                        )
                    }
                    Row {
                        IconButton(onClick = { /* Add Element */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add Step", tint = StitchCobalt600)
                        }
                        IconButton(onClick = { /* Export */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = StitchSlate600)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(workElements) { element ->
                        val isSelected = selectedElement?.id == element.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectElement(element) },
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, if (isSelected) StitchCobalt600 else StitchSlate200),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) StitchCobalt100 else StitchWhite
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${element.sequence}. ${element.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) StitchCobalt700 else StitchSlate900
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Std: ${String.format("%.2fs", element.standardTime)} • ${element.timeSource.name}",
                                        style = IeTypography.dataMono,
                                        fontSize = 11.sp,
                                        color = if (isSelected) StitchCobalt700 else StitchSlate600
                                    )
                                }
                                IeClassificationBadge(element.valueClassification)
                            }
                        }
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
        ) {
            selectedElement?.let { element ->
                val stats = viewModel.getStatsForElement(element.id)
                if (stats != null) {
                    ElementDetailHeader(element)
                    Spacer(modifier = Modifier.height(12.dp))

                    var selectedTabIndex by remember { mutableIntStateOf(0) }
                    val tabs = listOf("OBSERVATION CYCLES", "STATISTICS & CYCLES", "TIME CALCULATIONS", "VA/NVA SUMMARY")

                    Surface(
                        shape = IeRadius.cardShape,
                        border = IeBorders.cardBorder,
                        color = StitchWhite
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = StitchWhite,
                            contentColor = StitchCobalt700,
                            divider = { HorizontalDivider(color = StitchSlate200) }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            title,
                                            style = IeTypography.tableHeader,
                                            color = if (selectedTabIndex == index) StitchCobalt700 else StitchSlate600
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedTabIndex) {
                        0 -> ObservationTableContent(stats, viewModel)
                        1 -> StatisticsContent(stats)
                        2 -> CalculationsContent(element, viewModel)
                        3 -> VaNvaSummaryContent(element)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    IeReportBranding()
                }
            }
        }
    }
}

@Composable
fun ObservationTableContent(stats: ElementStats, viewModel: TimeStudyViewModel) {
    IeCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Recorded Cycle Observations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = StitchSlate900
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Toggle outlier cycles to exclude rejected measurements from normal time calculation",
            style = MaterialTheme.typography.bodySmall,
            color = StitchSlate500
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Industrial Data Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchSlate100, RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, StitchSlate200), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CYCLE", modifier = Modifier.weight(1f), style = IeTypography.tableHeader, color = StitchSlate600)
            Text("OBSERVED (S)", modifier = Modifier.weight(1.2f), style = IeTypography.tableHeader, color = StitchSlate600)
            Text("SOURCE", modifier = Modifier.weight(1.2f), style = IeTypography.tableHeader, color = StitchSlate600)
            Text("STATUS", modifier = Modifier.weight(1.2f), style = IeTypography.tableHeader, color = StitchSlate600)
            Text("ACTION", modifier = Modifier.weight(1f), style = IeTypography.tableHeader, color = StitchSlate600)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Table Rows
        stats.observations.forEachIndexed { index, obs ->
            val rowBg = if (index % 2 == 1) StitchSlate50 else StitchWhite
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(obs.cycleId, modifier = Modifier.weight(1f), style = IeTypography.dataMono, color = StitchSlate800)
                Text(
                    String.format("%.2f s", obs.observedTime),
                    modifier = Modifier.weight(1.2f),
                    style = IeTypography.dataMonoBold,
                    color = if (obs.isRejected) StitchNvaRedText else StitchSlate900
                )
                Text(obs.timeSource.name, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = StitchSlate700)
                Box(modifier = Modifier.weight(1.2f)) {
                    IeBadge(
                        text = if (obs.isRejected) "REJECTED" else "ACCEPTED",
                        variant = if (obs.isRejected) IeBadgeVariant.ERROR else IeBadgeVariant.SUCCESS
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { viewModel.toggleObservationRejection(obs.id) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (obs.isRejected) "Include" else "Reject",
                            style = IeTypography.dataMonoBold,
                            color = if (obs.isRejected) StitchCobalt600 else StitchNvaRed
                        )
                    }
                }
            }
            if (index < stats.observations.size - 1) {
                HorizontalDivider(color = StitchSlate200, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun StatisticsContent(stats: ElementStats) {
    val sampleSize = stats.activeObservations.size
    val stdDev = stats.stdDev
    val mean = stats.average
    val confidenceInterval95 = if (sampleSize > 1) 1.96 * stdDev / kotlin.math.sqrt(sampleSize.toDouble()) else 0.0
    val requiredSampleSize = if (mean > 0 && stdDev > 0) {
        val n = ((40.0 * stdDev) / mean)
        (n * n).toInt().coerceAtLeast(5)
    } else 5

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Statistical KPIs
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Statistical Distribution & Sample Sufficiency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IeKpiCard(
                    title = "Sample Mean",
                    value = String.format("%.2f", mean),
                    unit = "s",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Std Dev (σ)",
                    value = String.format("%.2f", stdDev),
                    unit = "s",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Range (Max-Min)",
                    value = String.format("%.2f", stats.max - stats.min),
                    unit = "s",
                    subtitle = "Min: ${String.format("%.1f", stats.min)} | Max: ${String.format("%.1f", stats.max)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IeKpiCard(
                    title = "95% Conf. Interval",
                    value = String.format("±%.2fs", confidenceInterval95),
                    subtitle = "Precision margin",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Accepted Samples",
                    value = "$sampleSize",
                    unit = "cycles",
                    subtitle = "Required: $requiredSampleSize",
                    isAlert = sampleSize < requiredSampleSize,
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Sufficiency Status",
                    value = if (sampleSize >= requiredSampleSize) "SUFFICIENT" else "NEED MORE",
                    isAlert = sampleSize < requiredSampleSize,
                    subtitle = "Based on ±5% precision",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Cycle Histogram Chart
        IeChartContainer(
            title = "Cycle Time Distribution Histogram",
            subtitle = "Observation variations and outlier rejection flags"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val maxTime = stats.observations.maxOfOrNull { it.observedTime }?.toFloat() ?: 1f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (stats.observations.size * 2).coerceAtLeast(1)
                    var xOffset = barWidth / 2

                    stats.observations.forEach { obs ->
                        val barHeight = (obs.observedTime.toFloat() / maxTime) * size.height
                        val color = if (obs.isRejected) StitchNvaRed else StitchCobalt600
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
fun CalculationsContent(element: WorkElement, viewModel: TimeStudyViewModel) {
    var rating by remember(element.id) { mutableStateOf(element.performanceRating.toString()) }
    var allowance by remember(element.id) { mutableStateOf(element.allowance.toString()) }

    IeCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Time Standards Derivation (IE Method)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = StitchSlate900
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Formula: Normal Time = Observed × Rating | Standard Time = Normal × (1 + Allowance)",
            style = MaterialTheme.typography.bodySmall,
            color = StitchSlate500
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = rating,
                onValueChange = { rating = it },
                label = { Text("Performance Rating (Westinghouse / Leveling)") },
                modifier = Modifier.weight(1f),
                textStyle = IeTypography.dataMono,
                shape = IeRadius.inputShape
            )
            Button(
                onClick = { viewModel.updatePerformanceRating(element.id, rating.toDoubleOrNull() ?: 1.0) },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                shape = IeRadius.buttonShape
            ) {
                Text("Apply")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = allowance,
                onValueChange = { allowance = it },
                label = { Text("Allowance Factor (Fatigue, Personal, Delay - e.g. 0.12)") },
                modifier = Modifier.weight(1f),
                textStyle = IeTypography.dataMono,
                shape = IeRadius.inputShape
            )
            Button(
                onClick = { viewModel.updateAllowance(element.id, allowance.toDoubleOrNull() ?: 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                shape = IeRadius.buttonShape
            ) {
                Text("Apply")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = StitchSlate200)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IeKpiCard(
                title = "Observed Time (OT)",
                value = String.format("%.2f", element.observedTime),
                unit = "s",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Normal Time (NT)",
                value = String.format("%.2f", element.normalTime),
                unit = "s",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Standard Time (ST)",
                value = String.format("%.2f", element.standardTime),
                unit = "s",
                trend = "Final Target",
                isPositiveTrend = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VaNvaSummaryContent(element: WorkElement) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Value Stream Classification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IeKpiCard(
                    title = "Classification",
                    value = element.valueClassification.name,
                    subtitle = "Lean Stream",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Waste Category",
                    value = element.wasteCategory.name.replace("_", " "),
                    subtitle = "Muda category",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Automation Potential",
                    value = if (element.automationOpportunity) "FEASIBLE" else "MANUAL",
                    subtitle = "Engineering assessment",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Element Workload Contribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
            Spacer(modifier = Modifier.height(12.dp))

            val standardTime = element.standardTime.toFloat()
            val totalTime = maxOf(standardTime, 60f)
            val percentage = if (totalTime > 0) standardTime / totalTime else 0f

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Standard Time: ${String.format("%.2fs", standardTime)}",
                    style = IeTypography.dataMonoBold,
                    color = StitchSlate900
                )
                Spacer(modifier = Modifier.width(16.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = StitchCobalt600,
                    trackColor = StitchSlate100
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    String.format("%.1f%% of Cycle", percentage * 100),
                    style = IeTypography.dataMonoBold,
                    color = StitchCobalt700
                )
            }
        }
    }
}

@Composable
fun ElementDetailHeader(element: WorkElement) {
    IeCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = element.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate900
                )
                IeClassificationBadge(element.valueClassification)
            }
            if (element.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = element.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchSlate500
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IeBadge(text = "STATION: ${element.stationId}", variant = IeBadgeVariant.PRIMARY)
                IeBadge(text = "OPERATOR: ${element.operatorId}", variant = IeBadgeVariant.DEFAULT)
                IeBadge(text = "SOURCE: ${element.timeSource.name}", variant = IeBadgeVariant.INFO)
            }
        }
    }
}
