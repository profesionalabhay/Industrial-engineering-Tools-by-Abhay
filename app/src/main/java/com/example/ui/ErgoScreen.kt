package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
fun ErgoScreen(
    viewModel: ErgoViewModel,
    projectId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "ERGONOMICS ASSESSMENT",
                subtitle = "Human factors and workplace safety analysis",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            IeLoadingState()
        } else if (uiState.selectedAssessment == null) {
            IeEmptyState(title = "No Assessments", message = "Conduct a new ergonomic review for this line.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ErgoScoreCard(uiState.selectedAssessment!!)
                }

                item {
                    IeSectionHeader(title = "RISK FACTORS", subtitle = "Critical posture and force findings")
                }

                items(uiState.selectedAssessment!!.findings) { finding ->
                    IeCard(contentPadding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StitchNnvaAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = finding, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
                        }
                    }
                }

                item {
                    IeCard {
                        Text(text = "RECOMMENDED ACTIONS", style = IeTypography.tableHeader, color = StitchSlate500)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.selectedAssessment!!.recommendation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun ErgoScoreCard(assessment: ErgoAssessment) {
    val scoreColor = when {
        assessment.totalScore <= 3 -> StitchVaGreen
        assessment.totalScore <= 7 -> StitchNnvaAmber
        else -> StitchNvaRed
    }

    IeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                IeBadge(text = assessment.method, variant = IeBadgeVariant.PRIMARY)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Station: ${assessment.stationId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Assessed by ${assessment.assessor}", style = IeTypography.badgeText, color = StitchSlate500)
            }
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(scoreColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = assessment.totalScore.toString(), style = IeTypography.kpiLarge, color = scoreColor)
                    Text(text = "SCORE", style = IeTypography.badgeText, color = scoreColor)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = IeRadius.pillShape,
                color = scoreColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = assessment.riskLevel.uppercase(),
                    style = IeTypography.badgeText,
                    color = StitchWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
