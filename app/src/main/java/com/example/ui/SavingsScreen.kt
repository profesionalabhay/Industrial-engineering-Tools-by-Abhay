package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
fun SavingsScreen(
    viewModel: SavingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // Initialized in AppShell
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "SAVINGS MANAGEMENT",
                subtitle = "Benefit realization and improvement tracking",
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    IeSectionHeader(title = "IDENTIFIED BENEFITS", subtitle = "Potential and validated savings")
                }

                items(uiState.benefits) { benefit ->
                    BenefitCard(benefit, uiState.validations.find { it.benefitId == benefit.id })
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item { IeReportBranding() }
            }
        }
    }
}

@Composable
fun BenefitCard(benefit: ImprovementBenefit, validation: SavingsValidation?) {
    IeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                IeBadge(text = benefit.type.name.replace("_", " "), variant = IeBadgeVariant.PRIMARY)
                Spacer(modifier = Modifier.height(4.dp))
                Text(benefit.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Source: ${benefit.source.name}", style = IeTypography.badgeText, color = StitchSlate500)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${benefit.value} ${benefit.unit}",
                    style = IeTypography.kpiLarge,
                    color = StitchVaGreen,
                    fontSize = 24.sp
                )
                
                if (validation != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchVaGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VALIDATED", style = IeTypography.badgeText, color = StitchVaGreen, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("PENDING", style = IeTypography.badgeText, color = StitchNnvaAmber)
                }
            }
        }
        
        if (validation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = StitchSlate100)
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text("VALIDATION NOTES", style = IeTypography.tableHeader, fontSize = 10.sp, color = StitchSlate500)
                Text(validation.notes, style = MaterialTheme.typography.bodySmall)
                Text("Validated by ${validation.validatedBy} at ${formatDate(validation.validatedAt)}", style = IeTypography.badgeText, color = StitchSlate400)
            }
        }
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
