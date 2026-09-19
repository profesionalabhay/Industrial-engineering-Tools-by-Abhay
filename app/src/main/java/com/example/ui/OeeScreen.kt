package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OeeScreen(
    viewModel: OeeViewModel,
    projectId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadOeeData(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "OEE & LOSS ANALYSIS",
                subtitle = "Deterministic equipment efficiency tracking",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else if (uiState.selectedRecord == null) {
            IeEmptyState(title = "No OEE Data", message = "No production records found for this project.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main OEE Summary
                item {
                    OeeSummaryCard(uiState.metrics!!)
                }

                // Loss Pareto
                item {
                    LossParetoCard(uiState.paretoData)
                }

                // Detailed Loss Log
                item {
                    IeSectionHeader(title = "Detailed Loss Events", subtitle = "Recent downtime and performance disruptions")
                }

                items(uiState.losses) { loss ->
                    LossEventItem(loss)
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun OeeSummaryCard(metrics: OeeMetrics) {
    IeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IeOeeGauge(label = "OEE", percentage = metrics.oee, size = 140.dp, color = StitchCobalt600)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRow("Availability", metrics.availability, StitchVaGreen)
                MetricRow("Performance", metrics.performance, StitchCobalt500)
                MetricRow("Quality", metrics.quality, StitchNnvaAmber)
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = StitchSlate200)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IeKpiSmall(label = "Planned Time", value = "${metrics.plannedProductionTimeMinutes.toInt()}m")
            IeKpiSmall(label = "Avail. Loss", value = "${metrics.availabilityLossMinutes.toInt()}m", isAlert = metrics.availabilityLossMinutes > 30)
            IeKpiSmall(label = "Perf. Loss", value = "${metrics.performanceLossMinutes.toInt()}m")
            IeKpiSmall(label = "Reject Count", value = "${metrics.qualityLossCount}u", isAlert = metrics.qualityLossCount > 20)
        }
    }
}

@Composable
fun MetricRow(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = StitchSlate600, modifier = Modifier.width(80.dp))
        Text(text = "${(value * 100).toInt()}%", style = IeTypography.dataMonoBold, color = StitchSlate900)
    }
}

@Composable
fun IeKpiSmall(label: String, value: String, isAlert: Boolean = false) {
    Column {
        Text(text = label.uppercase(), style = IeTypography.badgeText, color = StitchSlate500)
        Text(
            text = value, 
            style = IeTypography.dataMonoBold, 
            color = if (isAlert) StitchNvaRed else StitchSlate900
        )
    }
}

@Composable
fun LossParetoCard(paretoData: List<Pair<String, Double>>) {
    val totalLoss = paretoData.sumOf { it.second }
    
    IeCard {
        Text(text = "LOSS PARETO (MINUTES)", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            paretoData.forEach { (reason, duration) ->
                val percentage = if (totalLoss > 0) (duration / totalLoss).toFloat() else 0f
                IeLinearProgress(
                    label = reason,
                    value = "${duration.toInt()}m",
                    percentage = percentage,
                    color = if (percentage > 0.4f) StitchNvaRed else StitchCobalt500
                )
            }
        }
    }
}

@Composable
fun LossEventItem(loss: LossEvent) {
    IeCard(contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = loss.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IeBadge(text = loss.category.name.replace("_", " "), variant = when(loss.category) {
                        OeeLossCategory.QUALITY_LOSS -> IeBadgeVariant.ERROR
                        OeeLossCategory.AVAILABILITY_LOSS -> IeBadgeVariant.WARNING
                        else -> IeBadgeVariant.DEFAULT
                    })
                    if (loss.stationId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = loss.stationId, style = IeTypography.badgeText, color = StitchSlate500)
                    }
                }
            }
            Text(text = "${loss.durationMinutes.toInt()} min", style = IeTypography.dataMonoBold, color = StitchNvaRed)
        }
    }
}
