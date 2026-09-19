package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AboutScreen() {
    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "ABOUT IE COPILOT",
                subtitle = "System Information & Attribution",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StitchSlate50),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IeCard {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(StitchCobalt600, IeRadius.cardShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = StitchWhite, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "IE COPILOT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate900
                        )
                        Text(
                            text = "Manufacturing Excellence | Industrial Engineering",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchSlate500
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = StitchSlate100)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        AboutRow(label = "Developer", value = "Abhay Singh")
                        AboutRow(label = "Application", value = "IE COPILOT")
                        AboutRow(label = "Version", value = "2.6")
                        AboutRow(label = "Build", value = "26")
                        AboutRow(label = "Technology", value = "Kotlin | Compose | Gemini")
                        AboutRow(label = "AI Providers", value = "Gemini | NVIDIA NIM | OpenAI")
                        AboutRow(label = "Copyright", value = "© Abhay Singh")
                    }
                }
            }

            item {
                IeSectionHeader(title = "MISSION", subtitle = "Digitalizing Industrial Engineering")
            }

            item {
                IeCard {
                    Text(
                        text = "IE COPILOT is a professional industrial engineering workbench designed to bridge the gap between physical operations and digital intelligence. By combining deterministic engineering principles with advanced AI models, we empower Manufacturing Excellence teams to achieve unprecedented precision in time studies, line balancing, and capacity planning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchSlate700,
                        lineHeight = 22.sp
                    )
                }
            }
            
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed by Abhay Singh",
                        style = IeTypography.badgeText,
                        color = StitchSlate400
                    )
                    Text(
                        text = "IE COPILOT • V2.6 PRODUCTION",
                        style = IeTypography.dataMono,
                        fontSize = 10.sp,
                        color = StitchSlate300
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label.uppercase(), style = IeTypography.tableHeader, color = StitchSlate500)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = StitchSlate900)
    }
}
