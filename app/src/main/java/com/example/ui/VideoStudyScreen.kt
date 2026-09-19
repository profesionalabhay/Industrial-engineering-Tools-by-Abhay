package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale

enum class VideoStudyTab(val label: String) {
    WORKSPACE("Video Workspace"),
    CYCLES_STATS("Cycle Statistics"),
    OBSERVATION_TABLE("Elemental Matrix"),
    AI_FINDINGS("AI Findings & IE Report"),
    WHAT_IF_TRANSFER("What-If & Master Sync")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStudyScreen(viewModel: VideoStudyViewModel, modifier: Modifier = Modifier) {
    val studies by viewModel.studies.collectAsStateWithLifecycle()
    val activeStudy by viewModel.activeStudy.collectAsStateWithLifecycle()
    val videoUri by viewModel.videoUri.collectAsStateWithLifecycle()
    val filteredCandidates by viewModel.filteredCandidates.collectAsStateWithLifecycle()
    val cycles by viewModel.cycles.collectAsStateWithLifecycle()
    val cycleStats by viewModel.cycleStats.collectAsStateWithLifecycle()
    val opportunities by viewModel.opportunities.collectAsStateWithLifecycle()
    val aiReport by viewModel.aiReport.collectAsStateWithLifecycle()
    val videoTime by viewModel.videoTime.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val startMarker by viewModel.startMarker.collectAsStateWithLifecycle()
    val endMarker by viewModel.endMarker.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedCycleNum by viewModel.selectedCycleNumber.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(VideoStudyTab.WORKSPACE) }
    var candidateToEdit by remember { mutableStateOf<AICandidateElement?>(null) }
    var candidateToSplit by remember { mutableStateOf<AICandidateElement?>(null) }
    var showCreateMarkerDialog by remember { mutableStateOf(false) }
    var showNewStudyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchSlate50)
            .padding(IeSpacing.screenPadding)
    ) {
        // Top Header Card
        IeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Study", tint = StitchCobalt600, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeStudy?.name ?: "Video Time Study",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate900
                                )
                                Spacer(Modifier.width(8.dp))
                                IeBadge(
                                    text = activeStudy?.status?.name ?: "AWAITING_VALIDATION",
                                    variant = when (activeStudy?.status) {
                                        VideoStudyStatus.VALIDATED -> IeBadgeVariant.SUCCESS
                                        VideoStudyStatus.FAILED -> IeBadgeVariant.ERROR
                                        else -> IeBadgeVariant.INFO
                                    }
                                )
                            }
                            Text(
                                text = "Sub-second temporal tracking, AI motion parsing, cycle normalisation, and human IE validation",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSlate500
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { viewModel.simulateAiAnalysis() },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                            shape = IeRadius.buttonShape,
                            enabled = !isAnalyzing
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Run AI Segmentation")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showNewStudyDialog = true },
                            shape = IeRadius.buttonShape,
                            border = BorderStroke(1.dp, StitchSlate300)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(16.dp), tint = StitchSlate700)
                            Spacer(Modifier.width(4.dp))
                            Text("New Study", color = StitchSlate800)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(Modifier.height(8.dp))

                // Metadata Details Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetaBadge(label = "Model", value = "${activeStudy?.modelId ?: "-"} (${activeStudy?.variant ?: "-"})")
                        MetaBadge(label = "Station", value = activeStudy?.stationId ?: "-")
                        MetaBadge(label = "Operator", value = activeStudy?.operatorId ?: "-")
                        MetaBadge(label = "Target Takt", value = "${activeStudy?.expectedTaktSeconds ?: 0.0}s")
                        MetaBadge(label = "Duration", value = "${String.format(Locale.US, "%.1f", duration)}s")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.acceptAllCandidates() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Accept All", modifier = Modifier.size(16.dp), tint = StitchVaGreenText)
                            Spacer(Modifier.width(4.dp))
                            Text("Accept All AI", color = StitchVaGreenText, style = IeTypography.badgeText)
                        }
                        TextButton(onClick = { viewModel.commitAllValidatedElements() }) {
                            Icon(Icons.Default.Save, contentDescription = "Sync", modifier = Modifier.size(16.dp), tint = StitchCobalt600)
                            Spacer(Modifier.width(4.dp))
                            Text("Commit Validated", color = StitchCobalt600, style = IeTypography.badgeText)
                        }
                    }
                }
            }
        }

        // Status feedback snack/banner
        if (statusMessage != null) {
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = IeRadius.cardShape,
                colors = CardDefaults.cardColors(containerColor = StitchCobalt50),
                border = BorderStroke(1.dp, StitchCobalt500.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusMessage ?: "", color = StitchCobalt700, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { viewModel.clearStatusMessage() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = StitchCobalt700, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Navigation Tabs Strip
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = StitchWhite,
            contentColor = StitchCobalt600,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            VideoStudyTab.values().forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (activeTab == tab) StitchCobalt600 else StitchSlate600
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Tab Content
        when (activeTab) {
            VideoStudyTab.WORKSPACE -> VideoWorkspaceView(
                viewModel = viewModel,
                videoTime = videoTime,
                duration = duration,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                isAnalyzing = isAnalyzing,
                startMarker = startMarker,
                endMarker = endMarker,
                filteredCandidates = filteredCandidates,
                filter = filter,
                cycles = cycles,
                selectedCycleNum = selectedCycleNum,
                onEdit = { candidateToEdit = it },
                onSplit = { candidateToSplit = it },
                onCreateFromMarker = { showCreateMarkerDialog = true }
            )
            VideoStudyTab.CYCLES_STATS -> CyclesAndStatisticsView(
                viewModel = viewModel,
                cycles = cycles,
                stats = cycleStats
            )
            VideoStudyTab.OBSERVATION_TABLE -> ElementalMatrixView(
                candidates = filteredCandidates,
                cycles = cycles,
                viewModel = viewModel
            )
            VideoStudyTab.AI_FINDINGS -> AiFindingsAndReportView(
                report = aiReport,
                opportunities = opportunities
            )
            VideoStudyTab.WHAT_IF_TRANSFER -> WhatIfAndMasterSyncView(
                candidates = filteredCandidates,
                viewModel = viewModel
            )
        }
    }

    // Dialogs
    if (candidateToEdit != null) {
        EditCandidateDialog(
            candidate = candidateToEdit!!,
            onDismiss = { candidateToEdit = null },
            onSave = { name, start, end, act, cls, tools, mats, notes ->
                viewModel.editCandidate(candidateToEdit!!.id, name, start, end, act, cls, tools, mats, notes)
                candidateToEdit = null
            }
        )
    }

    if (candidateToSplit != null) {
        SplitCandidateDialog(
            candidate = candidateToSplit!!,
            onDismiss = { candidateToSplit = null },
            onSplit = { splitTime, part1, part2 ->
                viewModel.splitCandidate(candidateToSplit!!.id, splitTime, part1, part2)
                candidateToSplit = null
            }
        )
    }

    if (showCreateMarkerDialog) {
        CreateElementFromMarkerDialog(
            startTime = startMarker ?: 0.0,
            endTime = endMarker ?: videoTime,
            onDismiss = { showCreateMarkerDialog = false },
            onCreate = { name, act, cls, tools, mats ->
                viewModel.createElementFromMarkers(name, act, cls, tools, mats)
                showCreateMarkerDialog = false
            }
        )
    }

    if (showNewStudyDialog) {
        CreateNewStudyDialog(
            onDismiss = { showNewStudyDialog = false },
            onCreate = { name, file, model, variant, station, op, dur, takt ->
                viewModel.createNewStudy(name, file, model, variant, station, op, dur, takt)
                showNewStudyDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 1: WORKSPACE VIEW
// -----------------------------------------------------------------------------

@Composable
private fun VideoWorkspaceView(
    viewModel: VideoStudyViewModel,
    videoTime: Double,
    duration: Double,
    isPlaying: Boolean,
    playbackSpeed: Double,
    isAnalyzing: Boolean,
    startMarker: Double?,
    endMarker: Double?,
    filteredCandidates: List<AICandidateElement>,
    filter: CandidateFilter,
    cycles: List<StudyCycle>,
    selectedCycleNum: Int?,
    onEdit: (AICandidateElement) -> Unit,
    onSplit: (AICandidateElement) -> Unit,
    onCreateFromMarker: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: Player & Video Controls & Timeline
        Column(
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight()
                .padding(end = 10.dp)
        ) {
            // Video Player Canvas / Frame Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate900),
                colors = CardDefaults.cardColors(containerColor = StitchSlate900),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isAnalyzing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StitchWhite, strokeWidth = 3.dp)
                            Spacer(Modifier.height(14.dp))
                            Text("Gemini Video Intelligence parsing elemental motions...", color = StitchWhite, style = MaterialTheme.typography.bodyMedium)
                            Text("Extracting temporal boundaries and tool grasp signatures", color = StitchSlate400, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        // Center Play Button & Graphic Overlay
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.togglePlay() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = StitchWhite.copy(alpha = 0.90f),
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                            Text(
                                text = "STATION 04 ASSEMBLY FOOTAGE",
                                color = StitchSlate400,
                                style = IeTypography.tableHeader,
                                fontSize = 10.sp
                            )
                        }

                        // Top Left: Current Cycle / Marker HUD
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeCycle = cycles.find { videoTime >= it.startTime && videoTime <= it.endTime }
                            if (activeCycle != null) {
                                Surface(
                                    color = StitchSlate800.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CYCLE ${activeCycle.cycleNumber}",
                                        color = StitchCobalt500,
                                        style = IeTypography.dataMonoBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (startMarker != null) {
                                Surface(
                                    color = StitchVaGreen.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "IN: ${String.format(Locale.US, "%.2fs", startMarker)}",
                                        color = StitchWhite,
                                        style = IeTypography.dataMonoBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (endMarker != null) {
                                Surface(
                                    color = StitchNvaRed.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "OUT: ${String.format(Locale.US, "%.2fs", endMarker)}",
                                        color = StitchWhite,
                                        style = IeTypography.dataMonoBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Right Timecode
                        Text(
                            text = String.format(Locale.US, "%02d:%05.2f / %02d:%05.2f", (videoTime / 60).toInt(), videoTime % 60, (duration / 60).toInt(), duration % 60),
                            color = StitchWhite,
                            style = IeTypography.dataMonoBold,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback and Marker Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frame step and play buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.stepBackward() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Step -1 Frame", tint = StitchSlate700)
                            }
                            IconButton(onClick = { viewModel.togglePlay() }, modifier = Modifier.size(34.dp)) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = StitchSlate900)
                            }
                            IconButton(onClick = { viewModel.stepForward() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Step +1 Frame", tint = StitchSlate700)
                            }
                        }

                        // Time Slider
                        Slider(
                            value = videoTime.toFloat(),
                            onValueChange = { viewModel.seek(it.toDouble()) },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = StitchCobalt600,
                                activeTrackColor = StitchCobalt600,
                                inactiveTrackColor = StitchSlate200
                            )
                        )

                        // Playback Speed Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${playbackSpeed}x", style = IeTypography.dataMonoBold, color = StitchSlate800)
                            IconButton(
                                onClick = {
                                    val nextSpeed = when (playbackSpeed) {
                                        0.25 -> 0.5
                                        0.5 -> 1.0
                                        1.0 -> 1.5
                                        1.5 -> 2.0
                                        else -> 0.25
                                    }
                                    viewModel.setSpeed(nextSpeed)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = StitchSlate700)
                            }
                        }
                    }

                    // Marker Actions Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.setStartMarker() },
                                shape = IeRadius.buttonShape,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("[ Set In-Marker", style = IeTypography.badgeText, color = StitchSlate700)
                            }
                            OutlinedButton(
                                onClick = { viewModel.setEndMarker() },
                                shape = IeRadius.buttonShape,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Set Out-Marker ]", style = IeTypography.badgeText, color = StitchSlate700)
                            }
                            if (startMarker != null || endMarker != null) {
                                TextButton(
                                    onClick = { viewModel.clearMarkers() },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Clear Markers", style = IeTypography.badgeText, color = StitchNvaRed)
                                }
                            }
                        }

                        if (startMarker != null && endMarker != null && endMarker > startMarker) {
                            Button(
                                onClick = onCreateFromMarker,
                                colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt600),
                                shape = IeRadius.buttonShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Element from Range", style = IeTypography.badgeText)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Synchronized Timeline Ruler
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp).fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SYNCHRONIZED TIMELINE RULER", style = IeTypography.tableHeader, color = StitchSlate500)
                            Spacer(Modifier.width(8.dp))
                            Text("Green=VA, Amber=NNVA, Red=NVA", style = MaterialTheme.typography.bodySmall, color = StitchSlate400, fontSize = 10.sp)
                        }
                        Text("Span: ${String.format(Locale.US, "%.1f", duration)}s", style = IeTypography.dataMono, color = StitchSlate600, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val rulerHeight = 22.dp.toPx()
                        drawRect(color = StitchSlate100, size = Size(size.width, rulerHeight))

                        // Draw Candidate Segments
                        filteredCandidates.forEach { el ->
                            val startX = (el.startTime / duration) * size.width
                            val endX = (el.endTime / duration) * size.width

                            val segColor = when (el.finalClassification) {
                                ValueClassification.VA -> StitchVaGreen
                                ValueClassification.NNVA -> StitchNnvaAmber
                                ValueClassification.NVA -> StitchNvaRed
                            }

                            drawRect(
                                color = segColor.copy(alpha = if (el.validationStatus == ValidationStatus.REJECTED) 0.25f else 0.85f),
                                topLeft = Offset(startX.toFloat(), 0f),
                                size = Size(maxOf(2f, (endX - startX).toFloat()), rulerHeight)
                            )
                        }

                        // Draw Marker Indicators
                        if (startMarker != null) {
                            val inX = (startMarker / duration) * size.width
                            drawLine(color = StitchVaGreenText, start = Offset(inX.toFloat(), 0f), end = Offset(inX.toFloat(), size.height), strokeWidth = 2.dp.toPx())
                        }
                        if (endMarker != null) {
                            val outX = (endMarker / duration) * size.width
                            drawLine(color = StitchNvaRed, start = Offset(outX.toFloat(), 0f), end = Offset(outX.toFloat(), size.height), strokeWidth = 2.dp.toPx())
                        }

                        // Playhead
                        val playheadX = (videoTime / duration) * size.width
                        drawLine(
                            color = StitchSlate900,
                            start = Offset(playheadX.toFloat(), 0f),
                            end = Offset(playheadX.toFloat(), size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }
        }

        // Right Column: AI Candidate Queue & Validation Panel
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = IeRadius.cardShape,
            border = BorderStroke(1.dp, StitchSlate200),
            colors = CardDefaults.cardColors(containerColor = StitchWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                // Filter Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AI CANDIDATE WORK ELEMENTS", style = IeTypography.tableHeader, color = StitchSlate500)
                        Text("${filteredCandidates.size} elements in scope", style = IeTypography.badgeText, color = StitchSlate600)
                    }

                    // Cycle Selector Filter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cycle:", style = IeTypography.badgeText, color = StitchSlate500)
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedCycleNum == null,
                            onClick = { viewModel.setCycleFilter(null) },
                            label = { Text("All", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        cycles.forEach { c ->
                            Spacer(Modifier.width(2.dp))
                            FilterChip(
                                selected = selectedCycleNum == c.cycleNumber,
                                onClick = { viewModel.setCycleFilter(c.cycleNumber) },
                                label = { Text("C${c.cycleNumber}", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CandidateFilter.ALL to "All",
                        CandidateFilter.AI_SUGGESTED to "AI Suggested",
                        CandidateFilter.USER_VALIDATED to "Validated",
                        CandidateFilter.USER_EDITED to "Edited",
                        CandidateFilter.REJECTED to "Rejected"
                    ).forEach { (f, label) ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = StitchSlate200)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredCandidates.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No candidate elements match filter.", color = StitchSlate500, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCandidates, key = { it.id }) { cand ->
                            CandidateElementCard(
                                candidate = cand,
                                onSeek = { viewModel.seek(cand.startTime) },
                                onAccept = { viewModel.acceptCandidate(cand.id) },
                                onReject = { viewModel.rejectCandidate(cand.id) },
                                onEdit = { onEdit(cand) },
                                onSplit = { onSplit(cand) },
                                onDelete = { viewModel.deleteCandidate(cand.id) },
                                onToggleAbnormal = {
                                    viewModel.toggleAbnormalStatus(cand.id, "Non-standard interruption")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CANDIDATE ELEMENT CARD
// -----------------------------------------------------------------------------

@Composable
fun CandidateElementCard(
    candidate: AICandidateElement,
    onSeek: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAbnormal: () -> Unit
) {
    val borderColor = when (candidate.validationStatus) {
        ValidationStatus.USER_VALIDATED -> StitchVaGreen.copy(alpha = 0.5f)
        ValidationStatus.USER_EDITED -> StitchCobalt500.copy(alpha = 0.5f)
        ValidationStatus.REJECTED -> StitchNvaRed.copy(alpha = 0.4f)
        ValidationStatus.AI_SUGGESTED -> StitchSlate300
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSeek() },
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = StitchSlate50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IeBadge(
                        text = when (candidate.validationStatus) {
                            ValidationStatus.USER_VALIDATED -> "USER-VALIDATED"
                            ValidationStatus.USER_EDITED -> "USER-EDITED"
                            ValidationStatus.REJECTED -> "REJECTED"
                            ValidationStatus.AI_SUGGESTED -> "AI-SUGGESTED"
                        },
                        variant = when (candidate.validationStatus) {
                            ValidationStatus.USER_VALIDATED -> IeBadgeVariant.SUCCESS
                            ValidationStatus.USER_EDITED -> IeBadgeVariant.INFO
                            ValidationStatus.REJECTED -> IeBadgeVariant.ERROR
                            ValidationStatus.AI_SUGGESTED -> IeBadgeVariant.WARNING
                        }
                    )
                    IeBadge(
                        text = candidate.finalClassification.name,
                        variant = when (candidate.finalClassification) {
                            ValueClassification.VA -> IeBadgeVariant.SUCCESS
                            ValueClassification.NNVA -> IeBadgeVariant.WARNING
                            ValueClassification.NVA -> IeBadgeVariant.ERROR
                        }
                    )
                    if (candidate.isAbnormal) {
                        IeBadge(text = "ABNORMAL", variant = IeBadgeVariant.ERROR)
                    }
                }

                Text(
                    text = "AI Conf: ${String.format(Locale.US, "%.0f%%", candidate.confidenceScore * 100)}",
                    style = IeTypography.dataMonoBold,
                    fontSize = 11.sp,
                    color = if (candidate.confidenceScore >= 0.85) StitchVaGreenText else StitchNnvaAmberText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Element Name and Description
            Text(
                text = "${candidate.sequence}. ${candidate.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchSlate900
            )

            if (candidate.description.isNotBlank()) {
                Text(
                    text = candidate.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate500,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp, Duration, and Tooling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2fs", candidate.startTime)} - ${String.format(Locale.US, "%.2fs", candidate.endTime)} (Δ ${String.format(Locale.US, "%.2fs", candidate.duration)}) [C${candidate.cycleNumber}]",
                    style = IeTypography.dataMono,
                    fontSize = 11.sp,
                    color = StitchSlate700
                )

                if (candidate.toolNames.isNotEmpty() || candidate.materialNames.isNotEmpty()) {
                    Text(
                        text = (candidate.toolNames + candidate.materialNames).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = StitchCobalt700,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Traceability footnote if edited/split
            if (candidate.originalAiSuggestion != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Orig AI: '${candidate.originalAiSuggestion.name}' (${String.format(Locale.US, "%.2fs", candidate.originalAiSuggestion.duration)})",
                    style = IeTypography.dataMono,
                    fontSize = 9.sp,
                    color = StitchSlate400
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onSeek, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play Segment", tint = StitchCobalt600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = StitchSlate600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onSplit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Split", tint = StitchSlate600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleAbnormal, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.WarningAmber, contentDescription = "Toggle Abnormal", tint = if (candidate.isAbnormal) StitchNvaRed else StitchSlate400, modifier = Modifier.size(16.dp))
                    }
                }

                Row {
                    if (candidate.validationStatus != ValidationStatus.USER_VALIDATED) {
                        IconButton(onClick = onAccept, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = StitchVaGreenText, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (candidate.validationStatus != ValidationStatus.REJECTED) {
                        IconButton(onClick = onReject, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", tint = StitchNvaRed, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StitchSlate400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: CYCLES & STATISTICAL ANALYSIS VIEW
// -----------------------------------------------------------------------------

@Composable
private fun CyclesAndStatisticsView(
    viewModel: VideoStudyViewModel,
    cycles: List<StudyCycle>,
    stats: CycleStatistics
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary KPI Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatCard(
                title = "Average Cycle Time",
                value = "${String.format(Locale.US, "%.2f", stats.averageCycleTime)}s",
                subtitle = "${stats.validCycleCount} valid cycles (${stats.excludedCycleCount} excluded)",
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Range (Min - Max)",
                value = "${String.format(Locale.US, "%.2f", stats.minCycleTime)}s - ${String.format(Locale.US, "%.2f", stats.maxCycleTime)}s",
                subtitle = "Δ ${String.format(Locale.US, "%.2f", stats.rangeCycleTime)}s spread",
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Variation (StdDev / CV%)",
                value = if (stats.stdDevCycleTime != null && stats.cvPercent != null) {
                    "${String.format(Locale.US, "%.2f", stats.stdDevCycleTime)}s (${String.format(Locale.US, "%.1f", stats.cvPercent)}%)"
                } else {
                    "N < 2 (Need 2+ valid)"
                },
                subtitle = if (stats.cvPercent != null && stats.cvPercent > 15.0) "High variation flagged" else "Within normal pacing",
                valueColor = if (stats.cvPercent != null && stats.cvPercent > 15.0) StitchNnvaAmberText else StitchSlate900,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Value-Stream Split",
                value = "${String.format(Locale.US, "%.0f", stats.vaPercent)}% VA",
                subtitle = "${String.format(Locale.US, "%.0f", stats.nnvaPercent)}% NNVA | ${String.format(Locale.US, "%.0f", stats.nvaPercent)}% NVA",
                valueColor = StitchVaGreenText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Cycles Normalisation Table
        IeCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CYCLE NORMALISATION & EXCLUSION REGISTER", style = IeTypography.tableHeader, color = StitchSlate600)
                    Text("IE can exclude abnormal cycles from standard time", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cycles) { cycle ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, if (cycle.isExcluded) StitchNvaRed.copy(alpha = 0.5f) else StitchSlate200),
                            colors = CardDefaults.cardColors(containerColor = if (cycle.isExcluded) StitchNvaRedLight else StitchWhite)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Cycle ${cycle.cycleNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = StitchSlate900)
                                    Text(
                                        "${String.format(Locale.US, "%.2fs", cycle.startTime)} - ${String.format(Locale.US, "%.2fs", cycle.endTime)}",
                                        style = IeTypography.dataMono,
                                        color = StitchSlate600,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Duration: ${String.format(Locale.US, "%.2fs", cycle.duration)}",
                                        style = IeTypography.dataMonoBold,
                                        color = StitchCobalt700,
                                        fontSize = 12.sp
                                    )
                                    if (cycle.isExcluded) {
                                        IeBadge(text = "EXCLUDED: ${cycle.exclusionReason}", variant = IeBadgeVariant.ERROR)
                                    } else {
                                        IeBadge(text = "NORMAL CYCLE", variant = IeBadgeVariant.SUCCESS)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleCycleExclusion(cycle.id, "Abnormal Operator Pacing") },
                                        shape = IeRadius.buttonShape,
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (cycle.isExcluded) "Re-Include Cycle" else "Exclude from Standard", style = IeTypography.badgeText, color = if (cycle.isExcluded) StitchVaGreenText else StitchNvaRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: ELEMENTAL OBSERVATION MATRIX
// -----------------------------------------------------------------------------

@Composable
private fun ElementalMatrixView(
    candidates: List<AICandidateElement>,
    cycles: List<StudyCycle>,
    viewModel: VideoStudyViewModel
) {
    IeCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ELEMENTAL TIME STUDY MATRIX (CYCLE-TO-CYCLE)", style = IeTypography.tableHeader, color = StitchSlate600)
                Text("Deterministic element durations across repeated cycles", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
            }

            Spacer(Modifier.height(10.dp))

            // Group candidates by sequence / name
            val groupedBySequence = candidates.groupBy { it.sequence }.toSortedMap()

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchSlate100, shape = RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Seq & Element Name", style = IeTypography.tableHeader, modifier = Modifier.weight(1.8f), color = StitchSlate600)
                        Text("Category", style = IeTypography.tableHeader, modifier = Modifier.weight(0.9f), color = StitchSlate600)
                        Text("Classification", style = IeTypography.tableHeader, modifier = Modifier.weight(0.9f), color = StitchSlate600)
                        cycles.forEach { c ->
                            Text("C${c.cycleNumber}", style = IeTypography.tableHeader, modifier = Modifier.weight(0.7f), color = StitchSlate600)
                        }
                        Text("Avg Dur", style = IeTypography.tableHeader, modifier = Modifier.weight(0.8f), color = StitchSlate600)
                        Text("Status", style = IeTypography.tableHeader, modifier = Modifier.weight(0.9f), color = StitchSlate600)
                    }
                }

                items(groupedBySequence.entries.toList()) { (seq, elements) ->
                    val sample = elements.first()
                    val avgDur = elements.map { it.duration }.average()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchWhite, shape = RoundedCornerShape(4.dp))
                            .border(BorderStroke(1.dp, StitchSlate200), shape = RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$seq. ${sample.name}",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.8f),
                            color = StitchSlate900
                        )
                        Text(
                            sample.activityCategory.name,
                            style = IeTypography.dataMono,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(0.9f),
                            color = StitchSlate700
                        )
                        Text(
                            sample.finalClassification.name,
                            style = IeTypography.dataMonoBold,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(0.9f),
                            color = when (sample.finalClassification) {
                                ValueClassification.VA -> StitchVaGreenText
                                ValueClassification.NNVA -> StitchNnvaAmberText
                                ValueClassification.NVA -> StitchNvaRed
                            }
                        )

                        // Durations for each cycle
                        cycles.forEach { c ->
                            val elForCycle = elements.find { it.cycleNumber == c.cycleNumber }
                            Text(
                                if (elForCycle != null) String.format(Locale.US, "%.2fs", elForCycle.duration) else "-",
                                style = IeTypography.dataMono,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.7f),
                                color = StitchSlate800
                            )
                        }

                        Text(
                            String.format(Locale.US, "%.2fs", avgDur),
                            style = IeTypography.dataMonoBold,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(0.8f),
                            color = StitchCobalt700
                        )

                        Text(
                            sample.validationStatus.name,
                            style = IeTypography.badgeText,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(0.9f),
                            color = if (sample.validationStatus == ValidationStatus.USER_VALIDATED) StitchVaGreenText else StitchSlate600
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 4: AI FINDINGS & 7-SECTION IE REPORT
// -----------------------------------------------------------------------------

@Composable
private fun AiFindingsAndReportView(
    report: VideoStudyAiReport?,
    opportunities: List<VideoImprovementOpportunity>
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("AUTOMATIC INDUSTRIAL ENGINEERING IMPROVEMENT OPPORTUNITIES", style = IeTypography.tableHeader, color = StitchSlate600)
        }

        if (opportunities.isEmpty()) {
            item {
                IeCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No critical waste anomalies or excessive variations identified.", color = StitchSlate500, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(opportunities) { opp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IeRadius.cardShape,
                    border = BorderStroke(1.dp, StitchNnvaAmber.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = StitchWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opp.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = StitchSlate900)
                            IeBadge(text = opp.patternType, variant = IeBadgeVariant.WARNING)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(opp.evidence, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = StitchSlate200)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("POTENTIAL CAUSE:", style = IeTypography.badgeText, color = StitchSlate500)
                                Text(opp.potentialCause, style = MaterialTheme.typography.bodySmall, color = StitchSlate700)
                            }
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("SUGGESTED KAIZEN ACTION:", style = IeTypography.badgeText, color = StitchVaGreenText)
                                Text(opp.suggestedAction, style = MaterialTheme.typography.bodySmall, color = StitchSlate900, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text("FORMAL 7-SECTION INDUSTRIAL ENGINEERING DIAGNOSTIC REPORT", style = IeTypography.tableHeader, color = StitchSlate600)
        }

        if (report != null) {
            item {
                IeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReportSectionBlock(title = "1. OBSERVED FACTS", items = report.observedFacts, color = StitchCobalt600)
                        Spacer(Modifier.height(12.dp))
                        ReportSectionBlock(title = "2. CALCULATED RESULTS", items = report.calculatedResults, color = StitchCobalt600)
                        Spacer(Modifier.height(12.dp))

                        Text("3. ENGINEERING INTERPRETATION", style = IeTypography.tableHeader, color = StitchCobalt700)
                        Spacer(Modifier.height(4.dp))
                        Text(report.engineeringInterpretation, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
                        Spacer(Modifier.height(12.dp))

                        ReportSectionBlock(title = "4. POTENTIAL IMPROVEMENTS", items = report.potentialImprovements, color = StitchVaGreenText)
                        Spacer(Modifier.height(12.dp))
                        ReportSectionBlock(title = "5. RISKS & CONSTRAINTS", items = report.risksAndConstraints, color = StitchNvaRed)
                        Spacer(Modifier.height(12.dp))
                        ReportSectionBlock(title = "6. ASSUMPTIONS", items = report.assumptions, color = StitchSlate600)
                        Spacer(Modifier.height(12.dp))
                        ReportSectionBlock(title = "7. VALIDATION REQUIRED", items = report.validationRequired, color = StitchNnvaAmberText)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 5: WHAT-IF TRANSFER & MASTER SYNC
// -----------------------------------------------------------------------------

@Composable
private fun WhatIfAndMasterSyncView(
    candidates: List<AICandidateElement>,
    viewModel: VideoStudyViewModel
) {
    IeCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MASTER DATABASE PROMOTION & WHAT-IF STAGING", style = IeTypography.tableHeader, color = StitchSlate600)
                    Text("Promote validated candidates into official Work Elements or branch to What-If", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                }
                Button(
                    onClick = { viewModel.commitAllValidatedElements() },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Commit", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Commit All Validated")
                }
            }

            Spacer(Modifier.height(12.dp))

            val validated = candidates.filter { it.validationStatus == ValidationStatus.USER_VALIDATED || it.validationStatus == ValidationStatus.USER_EDITED }

            if (validated.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No validated elements ready for promotion. Validate candidates in Workspace first.", color = StitchSlate500)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(validated) { cand ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = IeRadius.cardShape,
                            border = BorderStroke(1.dp, StitchSlate200),
                            colors = CardDefaults.cardColors(containerColor = StitchWhite)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(cand.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = StitchSlate900)
                                        IeBadge(text = cand.finalClassification.name, variant = when (cand.finalClassification) {
                                            ValueClassification.VA -> IeBadgeVariant.SUCCESS
                                            ValueClassification.NNVA -> IeBadgeVariant.WARNING
                                            ValueClassification.NVA -> IeBadgeVariant.ERROR
                                        })
                                        if (cand.elementId != null) {
                                            IeBadge(text = "COMMITTED (${cand.elementId})", variant = IeBadgeVariant.INFO)
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Duration: ${String.format(Locale.US, "%.2fs", cand.duration)} | Activity: ${cand.activityCategory.name} | Cycle ${cand.cycleNumber}",
                                        style = IeTypography.dataMono,
                                        fontSize = 11.sp,
                                        color = StitchSlate600
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.commitValidatedElementToMaster(cand.id) },
                                        shape = IeRadius.buttonShape,
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Promote to Master", style = IeTypography.badgeText, color = StitchCobalt600)
                                    }

                                    Button(
                                        onClick = { viewModel.sendToWhatIf(cand.id, "ST-05") },
                                        colors = ButtonDefaults.buttonColors(containerColor = StitchCobalt600),
                                        shape = IeRadius.buttonShape,
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Branch to What-If", style = IeTypography.badgeText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DIALOGS & HELPER COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun ReportSectionBlock(title: String, items: List<String>, color: Color) {
    Text(title, style = IeTypography.tableHeader, color = color)
    Spacer(Modifier.height(4.dp))
    items.forEach { line ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
            Text("• ", color = color, fontWeight = FontWeight.Bold)
            Text(line, style = MaterialTheme.typography.bodySmall, color = StitchSlate800)
        }
    }
}

@Composable
private fun MetaBadge(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = IeTypography.tableHeader, fontSize = 9.sp, color = StitchSlate400)
        Text(value, style = IeTypography.dataMonoBold, fontSize = 11.sp, color = StitchSlate800)
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color = StitchSlate900,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title.uppercase(), style = IeTypography.tableHeader, color = StitchSlate500, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StitchSlate500, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EditCandidateDialog(
    candidate: AICandidateElement,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, VideoActivityCategory, ValueClassification, List<String>, List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf(candidate.name) }
    var start by remember { mutableStateOf(candidate.startTime.toString()) }
    var end by remember { mutableStateOf(candidate.endTime.toString()) }
    var category by remember { mutableStateOf(candidate.activityCategory) }
    var classification by remember { mutableStateOf(candidate.finalClassification) }
    var tools by remember { mutableStateOf(candidate.toolNames.joinToString(", ")) }
    var materials by remember { mutableStateOf(candidate.materialNames.joinToString(", ")) }
    var notes by remember { mutableStateOf(candidate.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Candidate Work Element", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Element Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start (s)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End (s)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Value Classification:", style = IeTypography.badgeText)
                        Row {
                            ValueClassification.values().forEach { cls ->
                                FilterChip(
                                    selected = classification == cls,
                                    onClick = { classification = cls },
                                    label = { Text(cls.name, fontSize = 9.sp) },
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tools,
                    onValueChange = { tools = it },
                    label = { Text("Tools Used (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = materials,
                    onValueChange = { materials = it },
                    label = { Text("Materials Handled") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = start.toDoubleOrNull() ?: candidate.startTime
                    val e = end.toDoubleOrNull() ?: candidate.endTime
                    val toolList = tools.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val matList = materials.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(name, s, e, category, classification, toolList, matList, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SplitCandidateDialog(
    candidate: AICandidateElement,
    onDismiss: () -> Unit,
    onSplit: (Double, String, String) -> Unit
) {
    val mid = (candidate.startTime + candidate.endTime) / 2.0
    var splitTime by remember { mutableStateOf(String.format(Locale.US, "%.2f", mid)) }
    var part1Name by remember { mutableStateOf("${candidate.name} [Reach/Pick]") }
    var part2Name by remember { mutableStateOf("${candidate.name} [Fasten/Place]") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Work Element", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Original Element: '${candidate.name}' (${String.format(Locale.US, "%.2fs - %.2fs", candidate.startTime, candidate.endTime)})", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = splitTime,
                    onValueChange = { splitTime = it },
                    label = { Text("Split Timestamp (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = part1Name,
                    onValueChange = { part1Name = it },
                    label = { Text("Part 1 Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = part2Name,
                    onValueChange = { part2Name = it },
                    label = { Text("Part 2 Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = splitTime.toDoubleOrNull() ?: mid
                    onSplit(t, part1Name, part2Name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) {
                Text("Split Element")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateElementFromMarkerDialog(
    startTime: Double,
    endTime: Double,
    onDismiss: () -> Unit,
    onCreate: (String, VideoActivityCategory, ValueClassification, List<String>, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VideoActivityCategory.ASSEMBLE) }
    var classification by remember { mutableStateOf(ValueClassification.VA) }
    var tools by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Work Element from Marker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Range: ${String.format(Locale.US, "%.2fs - %.2fs (Δ %.2fs)", startTime, endTime, endTime - startTime)}", style = IeTypography.dataMonoBold, color = StitchCobalt700)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Element Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Classification:", style = IeTypography.badgeText)
                        Row {
                            ValueClassification.values().forEach { cls ->
                                FilterChip(
                                    selected = classification == cls,
                                    onClick = { classification = cls },
                                    label = { Text(cls.name, fontSize = 9.sp) },
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tools,
                    onValueChange = { tools = it },
                    label = { Text("Tools Used") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = materials,
                    onValueChange = { materials = it },
                    label = { Text("Materials Handled") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val toolList = tools.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val matList = materials.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreate(name, category, classification, toolList, matList)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) {
                Text("Create Element")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateNewStudyDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("Station 04 Fastening Video Study") }
    var file by remember { mutableStateOf("workstation_st04_capture.mp4") }
    var modelId by remember { mutableStateOf("MDL-1") }
    var variant by remember { mutableStateOf("Standard") }
    var stationId by remember { mutableStateOf("ST-04") }
    var operatorId by remember { mutableStateOf("OP-1") }
    var duration by remember { mutableStateOf("60.0") }
    var takt by remember { mutableStateOf("27.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Video Study", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Study Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = file, onValueChange = { file = it }, label = { Text("Video File / Stream") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = variant, onValueChange = { variant = it }, label = { Text("Variant") }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stationId, onValueChange = { stationId = it }, label = { Text("Station ID") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = operatorId, onValueChange = { operatorId = it }, label = { Text("Operator ID") }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (s)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = takt, onValueChange = { takt = it }, label = { Text("Target Takt (s)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = duration.toDoubleOrNull() ?: 60.0
                    val t = takt.toDoubleOrNull() ?: 27.0
                    onCreate(name, file, modelId, variant, stationId, operatorId, d, t)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) {
                Text("Create Study")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
