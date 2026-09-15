package com.example.ui

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*

@Composable
fun VideoStudyScreen(viewModel: VideoStudyViewModel, modifier: Modifier = Modifier) {
    val videoUri by viewModel.videoUri.collectAsStateWithLifecycle()
    val proposedElements by viewModel.proposedElements.collectAsStateWithLifecycle()
    val videoTime by viewModel.videoTime.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    if (videoUri == null) {
        EmptyState(
            title = "Video Time Study",
            message = "Upload a video of the assembly process to use Gemini AI for automatic element detection.",
            icon = Icons.Default.Videocam,
            actionText = "Upload Video",
            onAction = { viewModel.uploadVideo() }
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.weight(1f)) {
            // Left Side: Video Player & Controls
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Video Player Mock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Gemini is analyzing video...", color = Color.White)
                        }
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { viewModel.togglePlay() }
                        )
                        Text(
                            text = String.format("%02d:%05.2f", (videoTime / 60).toInt(), videoTime % 60),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.togglePlay() }) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                            }
                            IconButton(onClick = { viewModel.stepBackward() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Step Backward")
                            }
                            IconButton(onClick = { viewModel.stepForward() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Step Forward")
                            }
                        }
                        
                        Slider(
                            value = videoTime.toFloat(),
                            onValueChange = { viewModel.seek(it.toDouble()) },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${playbackSpeed}x")
                            IconButton(onClick = { viewModel.setSpeed(if (playbackSpeed == 1.0) 0.5 else if (playbackSpeed == 0.5) 0.25 else 1.0) }) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.simulateAiAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalyzing
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI")
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze with Gemini AI")
                }
            }

            // Right Side: Detected Elements
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp)
                    .padding(end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("Detected Elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (proposedElements.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("No elements detected yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(proposedElements) { element ->
                                ProposedElementCard(element, viewModel)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom: Timeline with Segments
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text("Element Timeline", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val primaryColor = MaterialTheme.colorScheme.primary
                val errorColor = MaterialTheme.colorScheme.error
                val warningColor = Color(0xFFF57C00) // Orange
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw track
                    drawRect(color = surfaceVariant, size = Size(size.width, 24.dp.toPx()))
                    
                    // Draw elements
                    proposedElements.forEach { el ->
                        val startX = (el.startTime / duration) * size.width
                        val endX = (el.endTime / duration) * size.width
                        
                        val color = when (el.status) {
                            ProposedStatus.ACCEPTED -> primaryColor
                            ProposedStatus.REJECTED -> errorColor
                            ProposedStatus.LOW_CONFIDENCE -> warningColor
                            ProposedStatus.PROPOSED -> primaryColor.copy(alpha = 0.5f)
                        }
                        
                        drawRect(
                            color = color,
                            topLeft = Offset(startX.toFloat(), 0f),
                            size = Size((endX - startX).toFloat(), 24.dp.toPx())
                        )
                    }
                    
                    // Draw playhead
                    val playheadX = (videoTime / duration) * size.width
                    drawLine(
                        color = errorColor,
                        start = Offset(playheadX.toFloat(), 0f),
                        end = Offset(playheadX.toFloat(), size.height),
                        strokeWidth = 4f
                    )
                }
            }
        }
    }
}

@Composable
fun ProposedElementCard(element: ProposedElement, viewModel: VideoStudyViewModel) {
    var isEditing by remember { mutableStateOf(false) }
    
    val containerColor = when (element.status) {
        ProposedStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ProposedStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ProposedStatus.LOW_CONFIDENCE -> Color(0xFFFFF3E0)
        ProposedStatus.PROPOSED -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.seek(element.startTime) },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (element.status == ProposedStatus.PROPOSED) {
                Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("AI PROPOSED", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                Spacer(Modifier.height(4.dp))
            } else if (element.status == ProposedStatus.LOW_CONFIDENCE) {
                Badge(containerColor = Color(0xFFF57C00)) { Text("LOW CONFIDENCE - IE Validation Required", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color.White) }
                Spacer(Modifier.height(4.dp))
            }

            if (isEditing) {
                var name by remember { mutableStateOf(element.name) }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                    Button(onClick = { 
                        viewModel.editElement(element.id, name, element.startTime, element.endTime, element.classification)
                        isEditing = false 
                    }) { Text("Save") }
                }
            } else {
                Text(element.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(element.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(String.format("%.2fs - %.2fs (%.2fs)", element.startTime, element.endTime, element.duration), style = MaterialTheme.typography.labelMedium)
                    Text("Conf: ${String.format("%.0f%%", element.confidence * 100)}", style = MaterialTheme.typography.labelMedium, color = if (element.confidence < 0.8) Color(0xFFD84315) else MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    if (element.status != ProposedStatus.ACCEPTED) {
                        IconButton(onClick = { viewModel.acceptElement(element.id) }) { Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary) }
                    }
                    if (element.status != ProposedStatus.REJECTED) {
                        IconButton(onClick = { viewModel.rejectElement(element.id) }) { Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
