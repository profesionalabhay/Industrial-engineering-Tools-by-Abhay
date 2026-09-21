package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
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
fun RcaScreen(
    viewModel: RcaViewModel,
    projectId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "ROOT CAUSE ANALYSIS",
                subtitle = "Problem solving with 5-Why & Fishbone",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else if (uiState.selectedRca == null) {
            IeEmptyState(title = "No RCA Records", message = "Select a loss event to start an analysis.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RcaHeaderCard(uiState.selectedRca!!)
                }

                item {
                    IeSectionHeader(title = "5-WHY ANALYSIS", subtitle = "Drilling down to the systematic root cause")
                }

                items(uiState.selectedRca!!.fiveWhys.indices.toList()) { index ->
                    FiveWhyItem(
                        step = uiState.selectedRca!!.fiveWhys[index],
                        onUpdate = { viewModel.updateFiveWhys(index, it) }
                    )
                }
                
                if (uiState.selectedRca!!.fiveWhys.size < 5) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.updateFiveWhys(uiState.selectedRca!!.fiveWhys.size, "") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = IeRadius.buttonShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Why")
                        }
                    }
                }

                item {
                    FishboneCard(uiState.selectedRca!!.fishboneData)
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun RcaHeaderCard(rca: RcaRecord) {
    IeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IeBadge(text = rca.status.name, variant = IeBadgeVariant.PRIMARY)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "ID: ${rca.id}", style = IeTypography.dataMono, color = StitchSlate500)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = rca.problemStatement,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StitchSlate900
        )
        if (rca.evidenceReference != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "EVIDENCE: ${rca.evidenceReference}",
                style = IeTypography.badgeText,
                color = StitchCobalt600
            )
        }
    }
}

@Composable
fun FiveWhyItem(step: FiveWhyStep, onUpdate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = StitchCobalt600,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = step.whyNumber.toString(), color = StitchWhite, style = IeTypography.dataMonoBold)
                }
            }
            if (step.whyNumber < 5) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(StitchCobalt100))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        OutlinedTextField(
            value = step.whyText,
            onValueChange = onUpdate,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Why ${step.whyNumber}?") },
            placeholder = { Text("Describe the underlying reason...") },
            shape = IeRadius.cardShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StitchCobalt500,
                unfocusedBorderColor = StitchSlate200
            )
        )
    }
}

@Composable
fun FishboneCard(data: Map<String, List<String>>) {
    val categories = listOf("MAN", "MACHINE", "MATERIAL", "METHOD")
    
    IeCard {
        Text(text = "FISHBONE (4M) ANALYSIS", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.forEach { category ->
                val causes = data[category] ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StitchSlate50, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(text = category, style = IeTypography.dataMonoBold, color = StitchSlate700)
                    if (causes.isEmpty()) {
                        Text(text = "No causes identified", style = MaterialTheme.typography.bodySmall, color = StitchSlate400)
                    } else {
                        causes.forEach { cause ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StitchSlate400, modifier = Modifier.size(14.dp))
                                Text(text = cause, style = MaterialTheme.typography.bodySmall, color = StitchSlate800)
                            }
                        }
                    }
                }
            }
        }
    }
}
