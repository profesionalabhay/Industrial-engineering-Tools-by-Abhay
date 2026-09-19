package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ManpowerScreen(viewModel: CapacityViewModel, onNavigateToWhatIf: () -> Unit = {}, modifier: Modifier = Modifier) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    val activeScenarioId by viewModel.activeScenarioId.collectAsStateWithLifecycle()
    val params = scenarios.find { it.id == activeScenarioId }
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val opps by viewModel.manpowerOpportunities.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Top Header
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = "Manpower", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Manpower Planning & Allocation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Theoretical headcount calculation and labor optimization opportunities", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Column
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
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("ALLOCATION BASELINE", style = IeTypography.tableHeader, color = StitchSlate500)
                    HorizontalDivider(color = StitchSlate200)

                    params?.let { p ->
                        PlanningNumberField("Actual Operators on Line", p.actualManpower.toDouble(), " headcount") { v ->
                            viewModel.updateActiveScenario { it.copy(actualManpower = v.toInt()) }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    IeKpiCard(
                        title = "Calculated Theoretical Headcount",
                        value = "${metrics?.calculatedManpowerRequirement ?: 0}",
                        unit = " operators",
                        subtitle = "Total Work Content / Takt Time",
                        trend = "IE Formula",
                        isPositiveTrend = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    opps?.let { o ->
                        val diff = (params?.actualManpower ?: 0) - (metrics?.calculatedManpowerRequirement ?: 0)
                        val isSurplus = diff > 0
                        val isDeficit = diff < 0

                        Card(
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, if (isSurplus) StitchVaGreen.copy(alpha = 0.5f) else if (isDeficit) StitchNvaRed.copy(alpha = 0.5f) else StitchSlate200),
                            colors = CardDefaults.cardColors(containerColor = if (isSurplus) StitchVaGreenLight else if (isDeficit) StitchNvaRedLight else StitchSlate50),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    if (isSurplus) "POTENTIAL EXCESS HEADCOUNT" else if (isDeficit) "HEADCOUNT SHORTAGE" else "HEADCOUNT BALANCED",
                                    style = IeTypography.tableHeader,
                                    color = if (isSurplus) StitchVaGreenText else if (isDeficit) StitchNvaRedText else StitchSlate600
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${kotlin.math.abs(diff)} Operators",
                                    style = IeTypography.kpiLarge,
                                    color = if (isSurplus) StitchVaGreenText else if (isDeficit) StitchNvaRedText else StitchSlate900
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (isSurplus) "Opportunity for operator redeployment or rebalancing" else if (isDeficit) "Line likely cannot meet takt time with current staffing" else "Line staffing matches theoretical requirements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StitchSlate600
                                )
                            }
                        }
                    }
                }
            }

            // Opportunities Column
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("OPTIMIZATION & REDEPLOYMENT OPPORTUNITIES", style = IeTypography.tableHeader, color = StitchSlate500)
                    HorizontalDivider(color = StitchSlate200)

                    opps?.let { o ->
                        Card(
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, StitchSlate200),
                            colors = CardDefaults.cardColors(containerColor = StitchSlate50),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = "Note", tint = StitchNnvaAmberText, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Status: ${o.status}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchSlate900)
                                    }
                                    IeBadge(
                                        text = "${o.theoreticalPotential} HC Potential",
                                        variant = IeBadgeVariant.INFO
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Theoretical potential: ${o.theoreticalPotential} headcount. Validated saving: ${o.validatedSaving} (Requires line rebalancing validation).",
                                    color = StitchSlate600,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        o.suggestions.forEach { suggestion ->
                            val parts = suggestion.split(":")
                            if (parts.size >= 2) {
                                OpportunityCard(parts[0].trim(), parts.subList(1, parts.size).joinToString(":").trim())
                            } else {
                                OpportunityCard("Kaizen Target", suggestion)
                            }
                        }

                        if (o.suggestions.isEmpty()) {
                            Text(
                                "No immediate line balancing opportunities identified with current parameters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StitchSlate500
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigateToWhatIf() },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                            shape = IeRadius.buttonShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Validate in What-If Simulator", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpportunityCard(title: String, description: String) {
    Card(
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchCobalt700)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = StitchSlate700)
        }
    }
}
