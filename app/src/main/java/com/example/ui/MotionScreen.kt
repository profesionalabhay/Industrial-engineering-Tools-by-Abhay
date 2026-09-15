package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MotionScreen(viewModel: MotionViewModel, modifier: Modifier = Modifier) {
    val motions by viewModel.motions.collectAsStateWithLifecycle()
    val isAnalyzingVideo by viewModel.isAnalyzingVideo.collectAsStateWithLifecycle()
    val aiSuggestions by viewModel.aiSuggestions.collectAsStateWithLifecycle()

    var showAiDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = "Motion", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Micro-Motion Study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAiDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Video")
                    Spacer(Modifier.width(4.dp))
                    Text("AI Video Analysis")
                }
            }
        }

        // Metrics
        val totalTime = motions.sumOf { it.timeSec }
        val vaTime = motions.filter { it.category.isValueAdding }.sumOf { it.timeSec }
        val nvaTime = totalTime - vaTime
        val vaPercent = if (totalTime > 0) (vaTime / totalTime) * 100 else 0.0

        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricItem("Total Time", String.format("%.1f s", totalTime))
                MetricItem("Value Adding", String.format("%.1f s", vaTime))
                MetricItem("Non-Value Adding", String.format("%.1f s", nvaTime))
                MetricItem("VA Ratio", String.format("%.1f%%", vaPercent))
            }
        }

        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Manual Entry Form
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Manual Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    var desc by remember { mutableStateOf("") }
                    var time by remember { mutableStateOf("") }
                    var category by remember { mutableStateOf(MotionCategory.REACH) }
                    var expanded by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(category.name)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            MotionCategory.values().forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = { category = cat; expanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (Seconds)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            time.toDoubleOrNull()?.let { t ->
                                viewModel.addMotion(category, desc, t)
                                desc = ""
                                time = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Motion Element")
                    }
                }
            }

            // Motion List
            Card(modifier = Modifier.weight(2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Recorded Sequence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
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
            icon = { Icon(Icons.Default.VideoFile, contentDescription = "Video") },
            title = { Text("AI Video Analysis") },
            text = {
                if (isAnalyzingVideo) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Gemini is extracting micro-motions from video...")
                    }
                } else if (aiSuggestions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item { Text("Suggested Elements:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                        items(aiSuggestions) { sug ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sug.description, style = MaterialTheme.typography.bodyMedium)
                                        Row {
                                            Text(sug.category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("${sug.timeSec}s", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.acceptSuggestion(sug) }) {
                                            Icon(Icons.Default.Check, "Accept", tint = Color(0xFF388E3C))
                                        }
                                        IconButton(onClick = { viewModel.rejectSuggestion(sug.id) }) {
                                            Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Upload an operator video to have Gemini automatically extract micro-motions (Reach, Grasp, Walk, etc.) and generate a timeline.")
                }
            },
            confirmButton = {
                if (!isAnalyzingVideo && aiSuggestions.isEmpty()) {
                    Button(onClick = { viewModel.analyzeVideoMock("Operator at Station 1") }) {
                        Text("Analyze Mock Video")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAiDialog = false
                    viewModel.clearSuggestions()
                }) { Text("Close") }
            }
        )
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun MotionRow(motion: MotionElement, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(containerColor = if (motion.category.isValueAdding) Color(0xFF388E3C) else MaterialTheme.colorScheme.error) {
            Text(if (motion.category.isValueAdding) "VA" else "NVA", color = Color.White, modifier = Modifier.padding(4.dp))
        }
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(motion.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(motion.category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Text("${motion.timeSec}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
