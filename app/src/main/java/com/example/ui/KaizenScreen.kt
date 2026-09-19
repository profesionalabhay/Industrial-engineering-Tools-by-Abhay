package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
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
fun KaizenScreen(
    viewModel: KaizenViewModel,
    projectId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadKaizens(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "KAIZEN MANAGEMENT",
                subtitle = "Continuous improvement lifecycle",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else if (uiState.selectedKaizen == null) {
            IeEmptyState(title = "No Kaizen Actions", message = "Create a Kaizen from an RCA finding.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Info
                item {
                    KaizenDetailCard(uiState.selectedKaizen!!)
                }

                // Benefits
                item {
                    IeSectionHeader(title = "EXPECTED BENEFITS", subtitle = "Validated impact of improvement")
                }

                items(uiState.benefits) { benefit ->
                    BenefitItem(benefit)
                }

                // Status Timeline
                item {
                    KaizenStatusCard(uiState.selectedKaizen!!.status) { viewModel.updateKaizenStatus(it) }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun KaizenDetailCard(kaizen: KaizenRecord) {
    IeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = kaizen.id, style = IeTypography.dataMono, color = StitchSlate500)
            IeBadge(text = kaizen.status.name, variant = when(kaizen.status) {
                KaizenStatus.CLOSED -> IeBadgeVariant.SUCCESS
                KaizenStatus.IMPLEMENTED -> IeBadgeVariant.INFO
                else -> IeBadgeVariant.DEFAULT
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = kaizen.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchSlate900)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBlock(label = "PROBLEM", content = kaizen.problem)
            InfoBlock(label = "COUNTERMEASURE", content = kaizen.countermeasure)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IeKpiSmall(label = "OWNER", value = kaizen.owner)
            IeKpiSmall(label = "TARGET DATE", value = kaizen.targetDate)
            if (kaizen.stationId != null) {
                IeKpiSmall(label = "STATION", value = kaizen.stationId)
            }
        }
    }
}

@Composable
fun InfoBlock(label: String, content: String) {
    Column {
        Text(text = label, style = IeTypography.tableHeader, color = StitchSlate500)
        Text(text = content, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
    }
}

@Composable
fun BenefitItem(benefit: ImprovementBenefit) {
    IeCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = StitchVaGreenLight,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StitchVaGreenText, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = benefit.type.name.replace("_", " "), style = IeTypography.badgeText, color = StitchSlate500)
                Text(text = "${benefit.value} ${benefit.unit}", style = IeTypography.dataMonoBold, color = StitchVaGreenText)
            }
        }
    }
}

@Composable
fun KaizenStatusCard(currentStatus: KaizenStatus, onStatusChange: (KaizenStatus) -> Unit) {
    IeCard {
        Text(text = "PROGRESS TRACKING", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(modifier = Modifier.height(16.dp))
        
        val statuses = listOf(KaizenStatus.IDEA, KaizenStatus.IN_PROGRESS, KaizenStatus.IMPLEMENTED, KaizenStatus.CLOSED)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            statuses.forEach { status ->
                val isSelected = status == currentStatus
                val isPast = status.ordinal <= currentStatus.ordinal
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { onStatusChange(status) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isPast) StitchCobalt600 else StitchSlate100, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPast) Icons.Default.AssignmentTurnedIn else Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (isPast) StitchWhite else StitchSlate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.name.split("_").first(),
                        style = IeTypography.badgeText,
                        color = if (isSelected) StitchCobalt600 else StitchSlate500
                    )
                }
            }
        }
    }
}
