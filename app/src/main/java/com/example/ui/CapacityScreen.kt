package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CapacityScreen(viewModel: CapacityViewModel, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val params = scenarios.find { it.id == activeScenarioId }
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()

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
                    Icon(Icons.Default.Assessment, contentDescription = "Capacity", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Capacity & Throughput Planning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Takt time calculation, net operating time and capacity gap analysis", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                }

                params?.let { scn ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current State", style = MaterialTheme.typography.bodySmall, color = if (!scn.isFuture) StitchSlate900 else StitchSlate500, fontWeight = if (!scn.isFuture) FontWeight.Bold else FontWeight.Normal)
                        Switch(
                            checked = scn.isFuture,
                            onCheckedChange = { viewModel.switchScenario(it) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StitchWhite,
                                checkedTrackColor = StitchCobalt600,
                                uncheckedThumbColor = StitchSlate400,
                                uncheckedTrackColor = StitchSlate200
                            )
                        )
                        Text("Future State", style = MaterialTheme.typography.bodySmall, color = if (scn.isFuture) StitchSlate900 else StitchSlate500, fontWeight = if (scn.isFuture) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inputs Column
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("PLANNING PARAMETERS", style = IeTypography.tableHeader, color = StitchSlate500)
                    HorizontalDivider(color = StitchSlate200)

                    params?.let { p ->
                        PlanningNumberField("Customer Daily Demand", p.dailyDemand.toDouble(), " units") { v -> viewModel.updateActiveScenario { it.copy(dailyDemand = v.toInt()) } }
                        PlanningNumberField("Gross Shift Duration", p.shiftLengthHours, " hrs") { v -> viewModel.updateActiveScenario { it.copy(shiftLengthHours = v) } }
                        PlanningNumberField("Scheduled Breaks", p.breaksMinutes, " min") { v -> viewModel.updateActiveScenario { it.copy(breaksMinutes = v) } }
                        PlanningNumberField("Planned Downtime", p.plannedDowntimeMinutes, " min") { v -> viewModel.updateActiveScenario { it.copy(plannedDowntimeMinutes = v) } }
                        PlanningNumberField("Performance Efficiency", p.performanceEfficiency, "") { v -> viewModel.updateActiveScenario { it.copy(performanceEfficiency = v) } }
                        PlanningNumberField("First Pass Yield (Quality)", p.qualityYield, "") { v -> viewModel.updateActiveScenario { it.copy(qualityYield = v) } }
                        PlanningNumberField("Line Bottleneck Cycle Time", p.bottleneckCtSec, " s") { v -> viewModel.updateActiveScenario { it.copy(bottleneckCtSec = v) } }
                    }
                }
            }

            // Outputs & Gap Analysis Column
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                metrics?.let { m ->
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("PRODUCTION RATE & LINE METRICS", style = IeTypography.tableHeader, color = StitchSlate500)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IeKpiCard(
                                title = "Required Takt Time",
                                value = String.format("%.1f", m.taktTimeSec),
                                unit = " s",
                                subtitle = "Pace needed to meet demand",
                                modifier = Modifier.weight(1f)
                            )
                            IeKpiCard(
                                title = "Throughput (UPH)",
                                value = String.format("%.1f", m.uph),
                                unit = " units/hr",
                                subtitle = "Bottleneck capacity rate",
                                modifier = Modifier.weight(1f)
                            )
                            IeKpiCard(
                                title = "Daily Capacity",
                                value = "${m.dailyCapacity}",
                                unit = " units",
                                trend = "Output",
                                isPositiveTrend = m.dailyCapacity >= (params?.dailyDemand ?: 0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IeKpiCard(
                                title = "Gross Operating Time",
                                value = String.format("%.1f", m.availableTimeSec / 3600),
                                unit = " hrs",
                                modifier = Modifier.weight(1f)
                            )
                            IeKpiCard(
                                title = "Net Operating Time",
                                value = String.format("%.1f", m.netOperatingTimeSec / 3600),
                                unit = " hrs",
                                modifier = Modifier.weight(1f)
                            )
                            IeKpiCard(
                                title = "Line Utilization",
                                value = String.format("%.1f", m.utilization),
                                unit = "%",
                                isAlert = m.utilization > 100.0,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = StitchSlate200)

                        val isGap = m.capacityGap < 0
                        Card(
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, if (isGap) StitchNvaRed.copy(alpha = 0.5f) else StitchVaGreen.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = if (isGap) StitchNvaRedLight else StitchVaGreenLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (isGap) "CAPACITY DEFICIT DETECTED" else "CAPACITY SUFFICIENT",
                                        style = IeTypography.tableHeader,
                                        color = if (isGap) StitchNvaRedText else StitchVaGreenText
                                    )
                                    IeBadge(
                                        text = if (isGap) "OVERTIME NEEDED" else "MEETS TARGET",
                                        variant = if (isGap) IeBadgeVariant.ERROR else IeBadgeVariant.SUCCESS
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Net Output Balance Gap:", style = MaterialTheme.typography.bodyMedium, color = StitchSlate700)
                                    Text(
                                        String.format("%+d units / day", m.capacityGap),
                                        style = IeTypography.dataMonoBold,
                                        color = if (isGap) StitchNvaRedText else StitchVaGreenText
                                    )
                                }
                                if (isGap) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Compensating Overtime Required:", style = MaterialTheme.typography.bodyMedium, color = StitchSlate700)
                                        Text(
                                            String.format("%.1f hrs / shift", m.requiredOvertimeHours),
                                            style = IeTypography.dataMonoBold,
                                            color = StitchNvaRedText
                                        )
                                    }
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
fun PlanningNumberField(label: String, value: Double, unitSuffix: String = "", onValueChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let { v -> onValueChange(v) }
        },
        label = { Text(label) },
        trailingIcon = if (unitSuffix.isNotEmpty()) {
            { Text(unitSuffix, style = IeTypography.dataMono, color = StitchSlate500, modifier = Modifier.padding(end = 8.dp)) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = IeTypography.dataMono,
        shape = IeRadius.inputShape
    )
}
