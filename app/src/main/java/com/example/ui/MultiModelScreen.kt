package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.engine.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun MultiModelScreen(
    viewModel: MultiModelViewModel,
    modifier: Modifier = Modifier
) {
    val plan by viewModel.productionPlan.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val mixItems by viewModel.modelMix.collectAsStateWithLifecycle()
    val sequence by viewModel.productionSequence.collectAsStateWithLifecycle()
    val summary by viewModel.lineSummary.collectAsStateWithLifecycle()
    val seqAnalysis by viewModel.sequenceAnalysis.collectAsStateWithLifecycle()
    val simulation by viewModel.redistributionSimulation.collectAsStateWithLifecycle()
    val aiDiagnosis by viewModel.aiDiagnosis.collectAsStateWithLifecycle()
    val activeSubTab by viewModel.activeSubTab.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val subTabTitles = listOf(
        "PLAN & MIX",
        "STATION × MODEL",
        "LINE BALANCE",
        "SEQUENCE",
        "REDISTRIBUTION",
        "AI DIAGNOSIS"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Status Banner
        if (statusMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(6.dp),
                color = StitchCobalt100,
                border = BorderStroke(1.dp, StitchCobalt500.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchSlate900
                    )
                    IconButton(
                        onClick = { viewModel.clearStatusMessage() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = StitchCobalt700)
                    }
                }
            }
        }

        // Header KPI Bar
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Multi-Model Line Balancing",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StitchSlate900
                            )
                            Spacer(Modifier.width(8.dp))
                            IeBadge(
                                text = "V2.1",
                                variant = IeBadgeVariant.PRIMARY
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Plan: ${plan?.name ?: "Loading..."} (${plan?.shiftName})  •  Target Volume: ${plan?.plannedTotalQuantity ?: 0} units",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchSlate600
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IeBadge(
                            text = "${models.size} Models Active",
                            variant = IeBadgeVariant.INFO
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // High-density KPI row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IeKpiCard(
                        title = "Line Takt",
                        value = String.format(Locale.US, "%.1f", summary?.lineTakt ?: 0.0),
                        unit = "s",
                        modifier = Modifier.weight(1f)
                    )
                    IeKpiCard(
                        title = "Weighted BE",
                        value = String.format(Locale.US, "%.1f", summary?.weightedBalanceEfficiency ?: 0.0),
                        unit = "%",
                        trend = "Loss: ${String.format(Locale.US, "%.1f%%", summary?.weightedLineBalanceLoss ?: 0.0)}",
                        isPositiveTrend = (summary?.weightedBalanceEfficiency ?: 0.0) >= 85.0,
                        modifier = Modifier.weight(1f)
                    )
                    IeKpiCard(
                        title = "Bottleneck",
                        value = String.format(Locale.US, "%.1f", summary?.weightedBottleneckCycleTime ?: 0.0),
                        unit = "s",
                        trend = summary?.weightedBottleneckStation?.name?.take(12) ?: "None",
                        isAlert = (summary?.weightedBottleneckCycleTime ?: 0.0) > (summary?.lineTakt ?: 0.0),
                        modifier = Modifier.weight(1.2f)
                    )
                    IeKpiCard(
                        title = "Max Peak CT",
                        value = String.format(Locale.US, "%.1f", summary?.maxOverallCycleTime ?: 0.0),
                        unit = "s",
                        trend = summary?.maxBottleneckStation?.name?.take(12) ?: "None",
                        isAlert = (summary?.maxOverallCycleTime ?: 0.0) > (summary?.lineTakt ?: 0.0),
                        modifier = Modifier.weight(1.2f)
                    )
                    IeKpiCard(
                        title = "Mixed Capacity",
                        value = String.format(Locale.US, "%.1f", summary?.effectiveMixedCapacityPerHour ?: 0.0),
                        unit = "u/h",
                        trend = "+${String.format(Locale.US, "%.1f%%", summary?.capacityHeadroomPercent ?: 0.0)} gap",
                        isPositiveTrend = (summary?.capacityHeadroomPercent ?: 0.0) >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Sub-Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = StitchWhite,
            contentColor = StitchCobalt700,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchWhite, RoundedCornerShape(8.dp))
        ) {
            subTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubTab == index,
                    onClick = { viewModel.selectSubTab(index) },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Content Area by Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                0 -> ProductionPlanAndMixTab(plan, mixItems, viewModel)
                1 -> StationModelMatrixTab(summary, mixItems)
                2 -> LineBalanceTab(summary, mixItems)
                3 -> SequenceImpactTab(sequence, seqAnalysis, summary)
                4 -> WorkRedistributionTab(summary, simulation, viewModel)
                5 -> AiDiagnosisTab(aiDiagnosis)
            }
        }
    }
}

@Composable
fun ProductionPlanAndMixTab(
    plan: ProductionPlan?,
    mixItems: List<ModelMixItem>,
    viewModel: MultiModelViewModel
) {
    IeCard(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "PRODUCTION PLAN PARAMETERS",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IeKpiCard("Calendar", plan?.workingCalendar ?: "", modifier = Modifier.weight(1f))
                    IeKpiCard("Operating Seconds", String.format(Locale.US, "%.0f", plan?.plannedOperatingSeconds ?: 0.0), unit = "s", modifier = Modifier.weight(1f))
                    IeKpiCard("Line Takt Req.", String.format(Locale.US, "%.1f", plan?.requiredTaktSeconds ?: 0.0), unit = "s", modifier = Modifier.weight(1f))
                    IeKpiCard("Operators", "${plan?.operatorCount ?: 0}", modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "MODEL MIX & DEMAND ALLOCATION",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))
            }

            items(mixItems) { item ->
                ModelMixCard(item, onQuantityChange = { newQty ->
                    viewModel.updateModelQuantity(item.modelId, newQty)
                })
            }
        }
    }
}

@Composable
fun ModelMixCard(
    item: ModelMixItem,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Spacer(Modifier.width(6.dp))
                        IeBadge(item.variant, variant = IeBadgeVariant.INFO)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Model ID: ${item.modelId}  •  Work Content: ${String.format(Locale.US, "%.1fs", item.standardWorkContent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchSlate500
                    )
                }

                // Quantity Adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (item.plannedQuantity >= 50) onQuantityChange(item.plannedQuantity - 50) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = StitchSlate600)
                    }
                    Text(
                        "${item.plannedQuantity} pcs",
                        style = IeTypography.dataMonoBold,
                        color = StitchCobalt700,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onQuantityChange(item.plannedQuantity + 50) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = StitchSlate600)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Mix bar and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mix Ratio: ${String.format(Locale.US, "%.1f%%", item.effectiveMixPercentage)}",
                    style = IeTypography.dataMono,
                    fontSize = 12.sp,
                    color = StitchSlate700
                )
                Text(
                    "Required Model Takt: ${String.format(Locale.US, "%.1fs", item.requiredTakt)}",
                    style = IeTypography.dataMono,
                    fontSize = 12.sp,
                    color = StitchCobalt700
                )
            }

            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (item.effectiveMixPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = StitchCobalt600,
                trackColor = StitchSlate200
            )
        }
    }
}

@Composable
fun StationModelMatrixTab(
    summary: MixedModelLineSummary?,
    mixItems: List<ModelMixItem>
) {
    if (summary == null) return

    val horizontalScrollState = rememberScrollState()

    IeCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "STATION × MODEL WORKLOAD MATRIX",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Line Takt: ${String.format(Locale.US, "%.1fs", summary.lineTakt)}",
                        style = IeTypography.dataMonoBold,
                        color = StitchCobalt700
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable Matrix Table
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(modifier = Modifier.widthIn(min = 720.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchSlate100, RoundedCornerShape(4.dp))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Station", style = IeTypography.tableHeader, modifier = Modifier.width(130.dp))
                        mixItems.forEach { m ->
                            Text(
                                "${m.modelName}\n(${String.format(Locale.US, "%.0f%%", m.effectiveMixPercentage)})",
                                style = IeTypography.tableHeader,
                                modifier = Modifier.width(90.dp)
                            )
                        }
                        Text("Weighted CT", style = IeTypography.tableHeader, modifier = Modifier.width(90.dp))
                        Text("Max CT", style = IeTypography.tableHeader, modifier = Modifier.width(80.dp))
                        Text("Util %", style = IeTypography.tableHeader, modifier = Modifier.width(70.dp))
                        Text("Idle (s)", style = IeTypography.tableHeader, modifier = Modifier.width(70.dp))
                        Text("Status", style = IeTypography.tableHeader, modifier = Modifier.width(90.dp))
                    }

                    HorizontalDivider(color = StitchSlate200)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(summary.stationMetrics) { st ->
                            val isOverTakt = st.weightedCycleTime > summary.lineTakt
                            val rowBackground = if (st.isBottleneckWeighted) StitchCobalt100.copy(alpha = 0.4f) else StitchWhite

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBackground, RoundedCornerShape(4.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    st.station.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchSlate900,
                                    modifier = Modifier.width(130.dp)
                                )

                                mixItems.forEach { m ->
                                    val ct = st.modelCycleTimes[m.modelId] ?: 0.0
                                    val isModelOver = ct > summary.lineTakt
                                    Text(
                                        String.format(Locale.US, "%.1fs", ct),
                                        style = IeTypography.dataMono,
                                        color = if (isModelOver) StitchNvaRedText else StitchSlate800,
                                        modifier = Modifier.width(90.dp)
                                    )
                                }

                                Text(
                                    String.format(Locale.US, "%.1fs", st.weightedCycleTime),
                                    style = IeTypography.dataMonoBold,
                                    color = if (isOverTakt) StitchNvaRedText else StitchCobalt700,
                                    modifier = Modifier.width(90.dp)
                                )

                                Text(
                                    String.format(Locale.US, "%.1fs", st.maxCycleTime),
                                    style = IeTypography.dataMono,
                                    color = if (st.maxCycleTime > summary.lineTakt) StitchNvaRedText else StitchSlate700,
                                    modifier = Modifier.width(80.dp)
                                )

                                Text(
                                    String.format(Locale.US, "%.0f%%", st.weightedUtilization),
                                    style = IeTypography.dataMono,
                                    color = StitchSlate800,
                                    modifier = Modifier.width(70.dp)
                                )

                                Text(
                                    String.format(Locale.US, "%.1fs", st.weightedIdleTime),
                                    style = IeTypography.dataMono,
                                    color = StitchSlate500,
                                    modifier = Modifier.width(70.dp)
                                )

                                Box(modifier = Modifier.width(90.dp)) {
                                    if (st.isBottleneckWeighted) {
                                        IeBadge("BOTTLENECK", variant = IeBadgeVariant.ERROR)
                                    } else if (isOverTakt) {
                                        IeBadge("OVER TAKT", variant = IeBadgeVariant.ERROR)
                                    } else {
                                        IeBadge("NORMAL", variant = IeBadgeVariant.SUCCESS)
                                    }
                                }
                            }
                            HorizontalDivider(color = StitchSlate100)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineBalanceTab(
    summary: MixedModelLineSummary?,
    mixItems: List<ModelMixItem>
) {
    if (summary == null) return

    IeCard(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "BALANCE EFFICIENCY METRICS (IE FORMULAS)",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IeRadius.cardShape,
                    colors = CardDefaults.cardColors(containerColor = StitchSlate100),
                    border = BorderStroke(1.dp, StitchSlate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Formula A: Weighted Balance Efficiency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Text(
                            "Formula: Total Weighted Workload / (Stations × Takt) × 100",
                            style = IeTypography.dataMono,
                            fontSize = 12.sp,
                            color = StitchSlate600
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Result = ${String.format(Locale.US, "%.2f", summary.totalWeightedWorkContent)}s / (${summary.stationCount} × ${String.format(Locale.US, "%.1f", summary.lineTakt)}s) = ${String.format(Locale.US, "%.1f%%", summary.weightedBalanceEfficiency)}",
                            style = IeTypography.dataMonoBold,
                            color = StitchCobalt700
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Formula B: Model-Specific Balance Efficiency",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))

                summary.modelBalanceEfficiencies.forEach { (mId, eff) ->
                    val modelName = mixItems.find { it.modelId == mId }?.modelName ?: mId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modelName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchSlate800
                        )
                        Text(
                            String.format(Locale.US, "%.1f%%", eff),
                            style = IeTypography.dataMonoBold,
                            color = StitchCobalt700
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (eff / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = StitchCobalt700,
                        trackColor = StitchSlate200
                    )
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "STATION UTILIZATION SUMMARY",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))
            }

            items(summary.stationMetrics) { st ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(st.station.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Weighted CT: ${String.format(Locale.US, "%.1fs", st.weightedCycleTime)}  •  Max: ${String.format(Locale.US, "%.1fs", st.maxCycleTime)}", style = IeTypography.dataMono, fontSize = 11.sp, color = StitchSlate600)
                    }
                    Text("${String.format(Locale.US, "%.0f%%", st.weightedUtilization)} util", style = IeTypography.dataMonoBold, color = StitchCobalt700)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (st.weightedUtilization / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (st.weightedCycleTime > summary.lineTakt) StitchNvaRed else StitchCobalt600,
                    trackColor = StitchSlate200
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SequenceImpactTab(
    sequence: ProductionSequence?,
    seqAnalysis: SequenceAnalysisResult?,
    summary: MixedModelLineSummary?
) {
    IeCard(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "PRODUCTION HEIJUNKA SEQUENCE",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))
                if (sequence != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = IeRadius.cardShape,
                        colors = CardDefaults.cardColors(containerColor = StitchSlate100),
                        border = BorderStroke(1.dp, StitchSlate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                sequence.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StitchSlate900
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                sequence.modelPattern.forEachIndexed { idx, mId ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StitchCobalt700
                                    ) {
                                        Text(
                                            mId,
                                            style = IeTypography.dataMonoBold,
                                            color = StitchWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (idx < sequence.modelPattern.size - 1) {
                                        Text("→", style = IeTypography.dataMono, color = StitchSlate400)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "SEQUENCE RISK EVALUATION",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IeKpiCard(
                        title = "Pattern Length",
                        value = "${seqAnalysis?.patternLength ?: 0}",
                        unit = "units",
                        modifier = Modifier.weight(1f)
                    )
                    IeKpiCard(
                        title = "Smoothness Score",
                        value = String.format(Locale.US, "%.0f", seqAnalysis?.smoothnessScore ?: 100.0),
                        unit = "/100",
                        isPositiveTrend = (seqAnalysis?.smoothnessScore ?: 0.0) >= 80,
                        modifier = Modifier.weight(1f)
                    )
                    IeKpiCard(
                        title = "Max Over-Takt Run",
                        value = "${seqAnalysis?.maxConsecutiveOverTaktCount ?: 0}",
                        unit = "seq",
                        isAlert = (seqAnalysis?.maxConsecutiveOverTaktCount ?: 0) > 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                if ((seqAnalysis?.bottleneckRiskStations ?: emptyList()).isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = StitchNvaRedLight,
                        border = BorderStroke(1.dp, StitchNvaRedText.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StitchNvaRedText)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CONSECUTIVE OVER-TAKT RISK DETECTED",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchNvaRedText
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Running the pattern without buffer decoupling will cause line stoppages at: ${seqAnalysis?.bottleneckRiskStations?.joinToString(", ")}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSlate800
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = StitchCobalt100,
                        border = BorderStroke(1.dp, StitchCobalt500.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchCobalt700)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Leveled pattern is smoothed: No consecutive over-takt spikes exceed buffer capacity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSlate900
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkRedistributionTab(
    summary: MixedModelLineSummary?,
    simulation: RedistributionSimulationResult?,
    viewModel: MultiModelViewModel
) {
    val repository = ManufacturingRepository.getInstance()
    val elements = repository.workElements.filter { it.projectId == "P-001" }
    val stations = repository.stations

    var selectedElementId by remember { mutableStateOf(elements.firstOrNull()?.id ?: "") }
    var selectedTargetStationId by remember { mutableStateOf(stations.lastOrNull()?.id ?: "") }

    IeCard(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "WHAT-IF WORK ELEMENT REDISTRIBUTION SIMULATOR",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                Spacer(Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IeRadius.cardShape,
                    colors = CardDefaults.cardColors(containerColor = StitchWhite),
                    border = BorderStroke(1.dp, StitchSlate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Select Work Element:", style = MaterialTheme.typography.labelSmall, color = StitchSlate600)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            elements.take(4).forEach { el ->
                                FilterChip(
                                    selected = selectedElementId == el.id,
                                    onClick = { selectedElementId = el.id },
                                    label = { Text(el.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StitchCobalt700,
                                        selectedLabelColor = StitchWhite
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Select Destination Station:", style = MaterialTheme.typography.labelSmall, color = StitchSlate600)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stations.forEach { st ->
                                FilterChip(
                                    selected = selectedTargetStationId == st.id,
                                    onClick = { selectedTargetStationId = st.id },
                                    label = { Text(st.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StitchSlate900,
                                        selectedLabelColor = StitchWhite
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.simulateMoveElement(selectedElementId, selectedTargetStationId) },
                                colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt700),
                                shape = IeRadius.buttonShape
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simulate Rebalance")
                            }

                            if (simulation != null) {
                                OutlinedButton(
                                    onClick = { viewModel.clearSimulation() },
                                    shape = IeRadius.buttonShape
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            if (simulation != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SIMULATION RESULTS & CONSTRAINT ENGINE",
                        style = IeTypography.tableHeader,
                        color = StitchSlate500
                    )
                    Spacer(Modifier.height(6.dp))

                    if (simulation.violations.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = StitchNvaRedLight,
                            border = BorderStroke(1.dp, StitchNvaRedText.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = StitchNvaRedText)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "CONSTRAINT VIOLATION — MOVE NOT PERMITTED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchNvaRedText
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                simulation.violations.forEach { v ->
                                    Text("• ${v.description}", style = MaterialTheme.typography.bodySmall, color = StitchSlate900)
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = StitchCobalt100,
                            border = BorderStroke(1.dp, StitchCobalt500.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchCobalt700)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "FEASIBLE: All precedence & station constraints satisfied",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchSlate900
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.commitRedistribution(selectedElementId, selectedTargetStationId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                                        shape = IeRadius.buttonShape
                                    ) {
                                        Text("Commit Change")
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IeKpiCard("Current BE", "${String.format(Locale.US, "%.1f%%", simulation.baseSummary.weightedBalanceEfficiency)}", modifier = Modifier.weight(1f))
                                    IeKpiCard("Simulated BE", "${String.format(Locale.US, "%.1f%%", simulation.draftSummary.weightedBalanceEfficiency)}", modifier = Modifier.weight(1f))
                                    IeKpiCard("Delta BE", "${if (simulation.deltaWeightedBalanceEfficiency >= 0) "+" else ""}${String.format(Locale.US, "%.1f%%", simulation.deltaWeightedBalanceEfficiency)}", isPositiveTrend = simulation.deltaWeightedBalanceEfficiency >= 0, modifier = Modifier.weight(1f))
                                    IeKpiCard("Delta Cap.", "${if (simulation.deltaEffectiveCapacityPerHour >= 0) "+" else ""}${String.format(Locale.US, "%.1f u/h", simulation.deltaEffectiveCapacityPerHour)}", isPositiveTrend = simulation.deltaEffectiveCapacityPerHour >= 0, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiDiagnosisTab(diagnosis: MultiModelAiDiagnosis?) {
    if (diagnosis == null) return

    IeCard(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = StitchCobalt700)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI INDUSTRIAL ENGINEERING ADVISOR REPORT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate900
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deterministic multi-model balance interpretation adhering to strict IE standard format.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate500
                )
            }

            item {
                AiSectionCard("1. OBSERVED FACTS", diagnosis.observedFacts, StitchSlate100, StitchSlate800)
            }
            item {
                AiSectionCard("2. CALCULATED RESULTS", diagnosis.calculatedResults, StitchSlate100, StitchCobalt700)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IeRadius.cardShape,
                    colors = CardDefaults.cardColors(containerColor = StitchSlate100),
                    border = BorderStroke(1.dp, StitchSlate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "3. ENGINEERING INTERPRETATION",
                            style = IeTypography.tableHeader,
                            color = StitchSlate600
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            diagnosis.engineeringInterpretation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchSlate900
                        )
                    }
                }
            }
            item {
                AiSectionCard("4. RECOMMENDATIONS", diagnosis.recommendations, StitchCobalt100, StitchCobalt700)
            }
            item {
                AiSectionCard("5. RISKS & CONSTRAINTS", diagnosis.risksAndConstraints, StitchNvaRedLight, StitchNvaRedText)
            }
            item {
                AiSectionCard("6. ASSUMPTIONS", diagnosis.assumptions, StitchSlate100, StitchSlate700)
            }
            item {
                AiSectionCard("7. VALIDATION REQUIRED", diagnosis.validationRequired, StitchSlate100, StitchSlate800)
            }
        }
    }
}

@Composable
fun AiSectionCard(
    title: String,
    items: List<String>,
    containerColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, StitchSlate200)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = IeTypography.tableHeader,
                color = textColor
            )
            Spacer(Modifier.height(6.dp))
            items.forEach { line ->
                Text(
                    "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate900,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
