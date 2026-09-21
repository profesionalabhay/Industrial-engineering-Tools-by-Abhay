package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OpExDashboardScreen(
    viewModel: OpExDashboardViewModel,
    projectId: String,
    onNavigateToOee: () -> Unit,
    onNavigateToKaizen: () -> Unit,
    onNavigateToRca: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "OPERATIONAL EXCELLENCE",
                subtitle = "Integrated Manufacturing Performance",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Level KPIs
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IeKpiCard(
                            title = "Overall OEE",
                            value = "${(uiState.oeeSummary.firstOrNull()?.oee?.times(100))?.toInt() ?: 0}%",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.PrecisionManufacturing
                        )
                        IeKpiCard(
                            title = "Benefit Realised",
                            value = "${uiState.totalBenefits.toInt()}m",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TrendingUp,
                            trend = "+12%",
                            isPositiveTrend = true
                        )
                    }
                }

                // New Process Metrics Dashboard
                item {
                    ProcessMetricsDashboard(
                        cycleTimes = uiState.cycleTimeMetrics,
                        overallEfficiency = uiState.oeeSummary.firstOrNull()?.oee ?: 0.0
                    )
                }

                // Suite Status Cards
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IeKpiCard(
                            title = "Open RCAs",
                            value = uiState.openRcas.toString(),
                            modifier = Modifier.weight(1f),
                            subtitle = "Requires Analysis",
                            isAlert = uiState.openRcas > 0,
                            icon = Icons.Default.Search
                        )
                        IeKpiCard(
                            title = "Active Kaizens",
                            value = uiState.pendingKaizens.toString(),
                            modifier = Modifier.weight(1f),
                            subtitle = "In Pipeline",
                            icon = Icons.Default.Lightbulb
                        )
                    }
                }

                // OEE Quick View
                item {
                    IeSectionHeader(title = "LATEST OEE SNAPSHOT", action = {
                        TextButton(onClick = onNavigateToOee) { Text("View All") }
                    })
                }
                
                item {
                    uiState.oeeSummary.firstOrNull()?.let { metrics ->
                        OeeSummaryCard(metrics)
                    }
                }

                // Recent Losses
                item {
                    IeSectionHeader(title = "CRITICAL LOSSES", subtitle = "Recent high-impact disruptions")
                }

                items(uiState.recentLosses) { loss ->
                    LossEventItem(loss)
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
                item { IeReportBranding() }
            }
        }
    }
}
