package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun EnterpriseAnalyticsScreen(
    viewModel: EnterpriseViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "PLANT-WIDE IE ANALYTICS",
                subtitle = "Enterprise benchmarking and performance control",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StitchSlate50)
        ) {
            // Metric Selection
            LazyRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BenchmarkMetric.values()) { metric ->
                    FilterChip(
                        selected = uiState.benchmarkMetric == metric,
                        onClick = { viewModel.setBenchmarkMetric(metric) },
                        label = { Text(metric.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchCobalt100,
                            selectedLabelColor = StitchCobalt700
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    IeSectionHeader(title = "KPI BENCHMARKING", subtitle = "Comparative analysis across entities")
                }

                val filteredKpis = uiState.kpis.filter { it.metric == uiState.benchmarkMetric }
                
                items(filteredKpis) { kpi ->
                    KpiBenchmarkRow(kpi)
                }

                item {
                    IeSectionHeader(title = "PRODUCTIVITY TRENDS", subtitle = "Output per labour hour by project")
                }

                items(uiState.productivityRecords) { record ->
                    IeCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(record.projectId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Date: ${record.date} | Shift: ${record.shift}", style = IeTypography.badgeText, color = StitchSlate500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%.2f", record.productivityIndex)}",
                                    style = IeTypography.kpiLarge,
                                    color = StitchVaGreen
                                )
                                Text("UNITS/LH", style = IeTypography.badgeText, color = StitchVaGreen)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item { IeReportBranding() }
            }
        }
    }
}

@Composable
fun KpiBenchmarkRow(kpi: EnterpriseKpi) {
    IeCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    IeBadge(text = kpi.entityType, variant = IeBadgeVariant.PRIMARY)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(kpi.entityId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val color = if ((kpi.trend ?: 0.0) >= 0) StitchVaGreen else StitchNvaRed
                    Text(
                        "${String.format("%.1f", kpi.value * 100)}%",
                        style = IeTypography.kpiLarge,
                        color = StitchSlate900
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if ((kpi.trend ?: 0.0) >= 0) "▲" else "▼",
                            color = color,
                            style = IeTypography.badgeText
                        )
                        Text(
                            text = " ${String.format("%.1f", Math.abs(kpi.trend ?: 0.0) * 100)}%",
                            color = color,
                            style = IeTypography.badgeText
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Comparison against target
            kpi.target?.let { target ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TARGET: ${String.format("%.1f", target * 100)}%", style = IeTypography.badgeText, color = StitchSlate500)
                        val gap = kpi.value - target
                        Text(
                            text = if (gap >= 0) "ON TARGET" else "GAP: ${String.format("%.1f", Math.abs(gap) * 100)}%",
                            style = IeTypography.badgeText,
                            color = if (gap >= 0) StitchVaGreen else StitchNvaRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    IeLinearProgress(
                        label = "Performance",
                        value = "${String.format("%.1f", kpi.value * 100)}%",
                        percentage = kpi.value.toFloat(),
                        color = if (kpi.value >= target) StitchVaGreen else StitchNnvaAmber
                    )
                }
            }
        }
    }
}
