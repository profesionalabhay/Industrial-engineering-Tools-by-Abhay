package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiChatMessage
import com.example.data.AiRole
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotScreen(viewModel: AiCopilotViewModel, projectId: String) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = StitchWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StitchCobalt600, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StitchWhite, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("AI IE COPILOT", style = IeTypography.tableHeader, color = StitchSlate900)
                            Text(
                                text = "${uiState.selectedModel?.name ?: "No model selected"} • ${uiState.selectedProvider?.name ?: ""}",
                                style = IeTypography.breadcrumb,
                                color = StitchSlate500,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Row {
                        IconButton(onClick = { showModelSelector = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "AI Settings", tint = StitchCobalt700)
                        }
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = StitchNvaRed)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = StitchWhite,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column {
                    if (uiState.isAnalyzing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = StitchCobalt600,
                            trackColor = StitchCobalt100
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask IE Copilot...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            shape = IeRadius.cardShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StitchCobalt600,
                                unfocusedBorderColor = StitchSlate300
                            ),
                            maxLines = 4,
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { inputText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank() && !uiState.isAnalyzing) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    keyboardController?.hide()
                                }
                            },
                            containerColor = if (inputText.isNotBlank()) StitchCobalt600 else StitchSlate300,
                            contentColor = StitchWhite,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StitchSlate50)
        ) {
            if (uiState.messages.isEmpty()) {
                AiCopilotEmptyState { inputText = it }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatMessageItem(message)
                    }
                    if (uiState.isAnalyzing) {
                        item {
                            AiThinkingItem()
                        }
                    }
                }
            }
            
            if (uiState.error != null) {
                Surface(
                    color = StitchNvaRed.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = IeRadius.cardShape
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = StitchWhite)
                        Spacer(Modifier.width(12.dp))
                        Text(uiState.error ?: "", color = StitchWhite, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showModelSelector) {
        ModalBottomSheet(
            onDismissRequest = { showModelSelector = false },
            containerColor = StitchWhite
        ) {
            AiModelSelector(
                providers = uiState.providers,
                models = uiState.models,
                selectedProvider = uiState.selectedProvider,
                selectedModel = uiState.selectedModel,
                onProviderSelected = { viewModel.selectProvider(it) },
                onModelSelected = { viewModel.selectModel(it) },
                onDismiss = { showModelSelector = false }
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: AiChatMessage) {
    val isUser = message.role == AiRole.USER
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(StitchCobalt100),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StitchCobalt700, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) StitchCobalt600 else StitchWhite,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            border = if (isUser) null else IeBorders.cardBorder,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) StitchWhite else StitchSlate900
                )
                
                if (!isUser && message.evidenceIds.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = StitchSlate50,
                        shape = RoundedCornerShape(4.dp),
                        border = IeBorders.cardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = StitchCobalt600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Evidence Attached (${message.evidenceIds.size})",
                                style = IeTypography.breadcrumb,
                                color = StitchCobalt700,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(StitchSlate200),
                contentAlignment = Alignment.Center
            ) {
                Text("IE", style = IeTypography.dataMonoBold, fontSize = 10.sp, color = StitchSlate700)
            }
        }
    }
}

@Composable
fun AiThinkingItem() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(StitchCobalt100),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StitchCobalt700, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Analyzing workstation performance...",
            style = IeTypography.breadcrumb,
            color = StitchSlate500,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun AiCopilotEmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = StitchCobalt100,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Precision IE Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StitchSlate900
        )
        Text(
            "AI Copilot is connected to your deterministic IE engine. I can help analyze bottlenecks, balance efficiency, and suggest improvements based on calculated project data.",
            style = MaterialTheme.typography.bodyMedium,
            color = StitchSlate500,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "SUGGESTED ANALYSES",
            style = IeTypography.tableHeader,
            color = StitchSlate400,
            fontSize = 11.sp
        )
        
        Spacer(Modifier.height(12.dp))
        
        val suggestions = listOf(
            "Why is Station 5 the bottleneck?",
            "Identify stations with hidden capacity",
            "What happens if manpower changes from 4 to 3?",
            "Summarize project health for management"
        )
        
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestionClick(suggestion) },
                color = StitchWhite,
                shape = IeRadius.cardShape,
                border = IeBorders.cardBorder
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = StitchCobalt600, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
                }
            }
        }
    }
}

@Composable
fun AiModelSelector(
    providers: List<com.example.data.AIProviderConfig>,
    models: List<com.example.data.AIModelConfig>,
    selectedProvider: com.example.data.AIProviderConfig?,
    selectedModel: com.example.data.AIModelConfig?,
    onProviderSelected: (com.example.data.AIProviderConfig) -> Unit,
    onModelSelected: (com.example.data.AIModelConfig) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("AI Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Text("Active Provider", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(Modifier.height(8.dp))
        
        providers.forEach { provider ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProviderSelected(provider) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProvider?.id == provider.id,
                    onClick = { onProviderSelected(provider) },
                    colors = RadioButtonDefaults.colors(selectedColor = StitchCobalt600)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(provider.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(provider.type.name, style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text("Active Model", style = IeTypography.tableHeader, color = StitchSlate500)
        Spacer(Modifier.height(8.dp))
        
        val filteredModels = models.filter { it.providerId == selectedProvider?.id }
        filteredModels.forEach { model ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModelSelected(model) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedModel?.id == model.id,
                    onClick = { onModelSelected(model) },
                    colors = RadioButtonDefaults.colors(selectedColor = StitchCobalt600)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(model.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Task: ${model.taskType}", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
            shape = IeRadius.buttonShape
        ) {
            Text("Apply Configuration")
        }
        Spacer(Modifier.height(16.dp))
    }
}
