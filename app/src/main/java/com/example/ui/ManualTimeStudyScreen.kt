package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun ManualTimeStudyScreen(
    viewModel: ManualTimeStudyViewModel,
    studyId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studyId) {
        viewModel.loadStudy(studyId)
    }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "MANUAL VIDEO TIME STUDY",
                subtitle = uiState.study?.name ?: "Professional Analysis Workspace",
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
            // 1. Video Player Area (Mocked Controls)
            VideoPlayerMock(uiState, viewModel)

            // 2. Rapid Marking Controls
            RapidMarkingControl(uiState, viewModel)

            // 3. Bottom Panels (Elements & Cycles)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left: Observation List
                Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(16.dp)) {
                    Text(
                        text = "ELEMENTAL OBSERVATIONS",
                        style = IeTypography.tableHeader,
                        color = StitchSlate500,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ObservationList(uiState, viewModel)
                }

                // Right: Templates & Tools
                Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(16.dp)) {
                    TemplateSelector(uiState, viewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                    CycleControl(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerMock(state: ManualTimeStudyUiState, viewModel: ManualTimeStudyViewModel) {
    IeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        contentPadding = 0.dp,
        backgroundColor = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Center (Placeholder)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Text("VIDEO FEED: ${state.study?.videoFileName}", color = Color.White.copy(alpha = 0.7f))
            }

            // Overlay Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = (state.currentTimestamp / (state.study?.videoDurationSeconds ?: 1.0)).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = StitchCobalt500,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.togglePlayback() }) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Text(
                            text = formatTimestamp(state.currentTimestamp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${formatTimestamp(state.study?.videoDurationSeconds ?: 0.0)}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SPEED: ${state.playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { /* Previous Frame */ }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Frame", tint = Color.White)
                        }
                        IconButton(onClick = { /* Next Frame */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Frame", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RapidMarkingControl(state: ManualTimeStudyUiState, viewModel: ManualTimeStudyViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(100.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GIANT MARK BUTTON
        Button(
            onClick = { viewModel.markElement() },
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt600),
            shape = IeRadius.cardShape
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("MARK ELEMENT", fontWeight = FontWeight.Black, fontSize = 18.sp)
                state.lastMarkedTimestamp?.let { 
                    Text("Last: ${formatTimestamp(it)}", style = IeTypography.badgeText, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Cycle Display
        IeCard(
            modifier = Modifier.fillMaxHeight(0.8f).width(120.dp),
            contentPadding = 8.dp,
            backgroundColor = StitchWhite
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("CYCLE", style = IeTypography.badgeText, color = StitchSlate500)
                Text(state.currentCycle.toString(), style = IeTypography.kpiLarge, color = StitchSlate900)
            }
        }
    }
}

@Composable
fun TemplateSelector(state: ManualTimeStudyUiState, viewModel: ManualTimeStudyViewModel) {
    IeCard(modifier = Modifier.fillMaxWidth()) {
        Text("SELECT ELEMENT TEMPLATE", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.templates) { template ->
                FilterChip(
                    selected = state.selectedTemplate?.id == template.id,
                    onClick = { viewModel.selectTemplate(template) },
                    label = { Text(template.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StitchCobalt100,
                        selectedLabelColor = StitchCobalt700
                    )
                )
            }
        }
    }
}

@Composable
fun CycleControl(state: ManualTimeStudyUiState, viewModel: ManualTimeStudyViewModel) {
    IeCard(modifier = Modifier.fillMaxWidth()) {
        Text("CYCLE CONTROLS", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.startNewCycle() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StitchWhite, contentColor = StitchCobalt600),
            border = IeBorders.cardBorder
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START NEXT CYCLE")
        }
    }
}

@Composable
fun ObservationList(state: ManualTimeStudyUiState, viewModel: ManualTimeStudyViewModel) {
    IeCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = 0.dp
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.observations.reversed()) { obs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                IeLeanTokens.getColorForValueClassification(obs.finalClassification).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = obs.cycleNumber.toString(),
                            style = IeTypography.badgeText,
                            color = IeLeanTokens.getColorForValueClassification(obs.finalClassification)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(obs.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${formatTimestamp(obs.startTime)} → ${formatTimestamp(obs.endTime)}",
                            style = IeTypography.badgeText,
                            color = StitchSlate500
                        )
                    }
                    
                    Text(
                        "${String.format("%.2f", obs.duration)}s",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = StitchSlate900
                    )
                    
                    IconButton(onClick = { viewModel.deleteObservation(obs.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StitchSlate400, modifier = Modifier.size(20.dp))
                    }
                }
                Divider(color = StitchSlate100)
            }
        }
    }
}

private fun formatTimestamp(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    val ms = ((seconds % 1) * 1000).toInt()
    return String.format("%02d:%02d.%03d", mins, secs, ms)
}
