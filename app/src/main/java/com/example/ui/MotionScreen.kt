package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MotionScreen(viewModel: MotionViewModel, modifier: Modifier = Modifier) {
    val activeStudy by viewModel.activeStudy.collectAsStateWithLifecycle()
    val motions = activeStudy?.events ?: emptyList()
    val isAnalyzingVideo by viewModel.isAnalyzingVideo.collectAsStateWithLifecycle()
    val aiSuggestions by viewModel.aiSuggestions.collectAsStateWithLifecycle()

    var showAiDialog by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.DirectionsRun, contentDescription = "Motion", tint = StitchCobalt600)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Therblig Micro-Motion Study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StitchSlate900)
                        Text("Granular operator action breakdown, Therblig categorization and waste detection", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                }

                Button(
                    onClick = { showAiDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Video", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Video Motion Extract")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Metrics Banner
        val totalTime = motions.sumOf { it.timeSec }
        val vaTime = motions.filter { it.category.isValueAdding }.sumOf { it.timeSec }
        val nvaTime = totalTime - vaTime
        val vaPercent = if (totalTime > 0) (vaTime / totalTime) * 100 else 0.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IeKpiCard(
                title = "Total Micro-Motion Time",
                value = String.format("%.2f", totalTime),
                unit = " s",
                subtitle = "${motions.size} elementary actions",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Value-Added Time",
                value = String.format("%.2f", vaTime),
                unit = " s",
                trend = "Transform",
                isPositiveTrend = true,
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Non-Value Added Time",
                value = String.format("%.2f", nvaTime),
                unit = " s",
                isAlert = nvaTime > vaTime,
                subtitle = "Elimination target",
                modifier = Modifier.weight(1f)
            )
            IeKpiCard(
                title = "Motion Efficiency (VA Ratio)",
                value = String.format("%.1f", vaPercent),
                unit = "%",
                trend = "Ratio",
                isPositiveTrend = vaPercent > 50.0,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Manual Entry Form
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("RECORD MOTION ELEMENT", style = IeTypography.tableHeader, color = StitchSlate500)
                    HorizontalDivider(color = StitchSlate200)

                    var desc by remember { mutableStateOf("") }
                    var time by remember { mutableStateOf("") }
                    var category by remember { mutableStateOf(MotionCategory.REACH) }
                    var expanded by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Element Description") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = IeRadius.inputShape
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = IeRadius.inputShape,
                            border = BorderStroke(1.dp, StitchSlate300)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Therblig: ${category.name}", color = StitchSlate900)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = StitchSlate600)
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            MotionCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.name} (${if (cat.isValueAdding) "VA" else "NVA"})", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = { category = cat; expanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Duration (Seconds)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = IeTypography.dataMono,
                        shape = IeRadius.inputShape
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            time.toDoubleOrNull()?.let { t ->
                                viewModel.addMotion(category, desc, t)
                                desc = ""
                                time = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Motion Element")
                    }
                }
            }

            // Motion List
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                shape = IeRadius.cardShape,
                border = BorderStroke(1.dp, StitchSlate200),
                colors = CardDefaults.cardColors(containerColor = StitchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("CHRONOLOGICAL MOTION SEQUENCE", style = IeTypography.tableHeader, color = StitchSlate500)
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = StitchSlate200)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(motions) { motion ->
                        MotionRow(motion, onDelete = { viewModel.deleteMotion(motion.id) })
                    }
                }
            }
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = {
                showAiDialog = false
                viewModel.clearSuggestions()
            },
            shape = IeRadius.dialogShape,
            containerColor = StitchWhite,
            icon = { Icon(Icons.Default.VideoFile, contentDescription = "Video", tint = StitchCobalt600) },
            title = {
                Text(
                    "AI Micro-Motion Extraction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate900
                )
            },
            text = {
                if (isAnalyzingVideo) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator(color = StitchCobalt600, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Gemini is extracting micro-motions from operator footage...", style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
                    }
                } else if (aiSuggestions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("Extracted Candidate Elements:", style = IeTypography.tableHeader, color = StitchSlate600)
                        }
                        items(aiSuggestions) { sug ->
                            Card(
                                shape = IeRadius.cardShape,
                                border = BorderStroke(1.dp, StitchSlate200),
                                colors = CardDefaults.cardColors(containerColor = StitchSlate50),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sug.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = StitchSlate900)
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val category = MotionCategory.valueOf(sug.therblig)
                                            Text(sug.therblig, style = IeTypography.dataMonoBold, color = StitchCobalt700)
                                            Spacer(Modifier.width(8.dp))
                                            Text("${sug.timeMs / 1000.0}s", style = IeTypography.dataMono, color = StitchSlate600)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.acceptSuggestion(sug) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Check, "Accept", tint = StitchVaGreenText)
                                        }
                                        IconButton(onClick = { viewModel.rejectSuggestion(sug.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, "Reject", tint = StitchNvaRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Upload or stream operator video to have Gemini automatically extract micro-motions (Reach, Grasp, Move, Position, Release, Delay) into an analyzed motion sequence.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchSlate600
                    )
                }
            },
            confirmButton = {
                if (!isAnalyzingVideo && aiSuggestions.isEmpty()) {
                    Button(
                        onClick = { viewModel.analyzeVideoMock("Operator at Assembly Station 1") },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                        shape = IeRadius.buttonShape
                    ) {
                        Text("Analyze Video Study")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAiDialog = false
                        viewModel.clearSuggestions()
                    },
                    shape = IeRadius.buttonShape,
                    border = BorderStroke(1.dp, StitchSlate300)
                ) {
                    Text("Close", color = StitchSlate700)
                }
            }
        )
    }
}

@Composable
fun MotionRow(motion: MotionEvent, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        border = BorderStroke(1.dp, StitchSlate200),
        colors = CardDefaults.cardColors(containerColor = StitchSlate50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val category = MotionCategory.valueOf(motion.therblig)
            IeClassificationBadge(
                valueClassification = if (category.isValueAdding) ValueClassification.VA else ValueClassification.NVA
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(motion.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
                Spacer(Modifier.height(2.dp))
                Text(motion.therblig, style = IeTypography.dataMono, color = StitchSlate600)
            }

            Text(
                "${motion.timeMs / 1000.0}s",
                style = IeTypography.dataMonoBold,
                color = StitchSlate900
            )
            Spacer(Modifier.width(12.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = StitchNvaRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

val MotionEvent.timeSec: Double get() = timeMs / 1000.0
val MotionEvent.category: MotionCategory get() = MotionCategory.valueOf(therblig)
