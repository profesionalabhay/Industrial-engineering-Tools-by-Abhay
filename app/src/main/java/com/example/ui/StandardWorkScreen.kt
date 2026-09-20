package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
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
fun StandardWorkScreen(
    viewModel: StandardWorkViewModel,
    stationId: String,
    modelId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stationId, modelId) {
        // Initialized in AppShell with default project
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "STANDARD WORK",
                subtitle = "Versioned manufacturing instructions",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else if (uiState.currentRevision == null) {
            IeEmptyState(title = "No Standard Work", message = "No instructions released for this station.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                item {
                    SwHeaderCard(uiState.currentRevision!!, uiState.relatedKaizen)
                }

                // Element List
                item {
                    IeSectionHeader(title = "WORK ELEMENTS", subtitle = "Validated sequence and key points")
                }

                items(uiState.currentRevision!!.elements) { element ->
                    SwElementItem(element)
                }

                // History
                item {
                    SwHistoryCard(uiState.history)
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SwHeaderCard(revision: StandardWorkRevision, kaizen: KaizenRecord?) {
    IeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IeBadge(text = "VERSION ${revision.version}", variant = IeBadgeVariant.PRIMARY)
                Spacer(modifier = Modifier.width(12.dp))
                IeBadge(text = revision.status.name, variant = IeBadgeVariant.SUCCESS)
            }
            Text(text = revision.id, style = IeTypography.dataMono, color = StitchSlate500)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IeKpiSmall(label = "STATION", value = revision.stationId)
            IeKpiSmall(label = "MODEL", value = revision.modelId)
            IeKpiSmall(label = "TAKT", value = "${revision.taktTime}s")
        }
        
        if (kaizen != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = StitchSlate200)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StitchVaGreenText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Improved via Kaizen: ${kaizen.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchVaGreenText
                )
            }
        }
    }
}

@Composable
fun SwElementItem(element: StandardWorkElement) {
    IeCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StitchSlate100,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = element.sequence.toString(), style = IeTypography.dataMonoBold, color = StitchSlate700)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = element.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (element.keyPoints.isNotBlank()) {
                    Text(text = "Key: ${element.keyPoints}", style = MaterialTheme.typography.bodySmall, color = StitchSlate600)
                }
                if (element.isQualityCheck) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StitchCobalt600, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Quality Check Point", style = IeTypography.badgeText, color = StitchCobalt600)
                    }
                }
            }
            Text(text = "${element.durationSeconds}s", style = IeTypography.dataMonoBold, color = StitchSlate900)
        }
    }
}

@Composable
fun SwHistoryCard(history: List<StandardWorkRevision>) {
    IeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = StitchSlate500)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "REVISION HISTORY", style = IeTypography.tableHeader, color = StitchSlate500)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.take(3).forEach { rev ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "V${rev.version} - ${rev.status}", style = MaterialTheme.typography.bodySmall, color = StitchSlate700)
                    Text(text = rev.author, style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                }
            }
        }
    }
}
