package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(repository: ManufacturingRepository) {
    var providers by remember { mutableStateOf(repository.getAiProviders()) }
    var models by remember { mutableStateOf(repository.getAiModels()) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<AIProviderConfig?>(null) }

    Scaffold(
        topBar = {
            IeSectionHeader(
                title = "AI & MODEL CONFIGURATION",
                subtitle = "Manage inference endpoints and model routing"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProviderDialog = true },
                containerColor = StitchSlate900,
                contentColor = StitchWhite,
                shape = IeRadius.buttonShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Provider")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "CONFIGURED PROVIDERS",
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
            }

            items(providers) { provider ->
                IeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (provider.isActive) StitchCobalt600 else StitchSlate300,
                                        IeRadius.cardShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(provider.type) {
                                        AIProviderType.GEMINI -> Icons.Default.Cloud
                                        AIProviderType.NVIDIA_NIM -> Icons.Default.Memory
                                        else -> Icons.Default.Dns
                                    },
                                    contentDescription = null,
                                    tint = StitchWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(provider.baseUrl, style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                            }
                        }
                        
                        Row {
                            IconButton(onClick = { editingProvider = provider; showAddProviderDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = StitchCobalt700)
                            }
                            Switch(
                                checked = provider.isActive,
                                onCheckedChange = { 
                                    val updated = provider.copy(isActive = it)
                                    repository.saveAiProvider(updated)
                                    providers = repository.getAiProviders()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = StitchCobalt600)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = StitchSlate100)
                    Spacer(Modifier.height(12.dp))
                    
                    Text("AVAILABLE MODELS", style = IeTypography.breadcrumb, color = StitchSlate400)
                    Spacer(Modifier.height(8.dp))
                    
                    val providerModels = models.filter { it.providerId == provider.id }
                    providerModels.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (model.isDefault) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(model.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            IeBadge(model.taskType, variant = IeBadgeVariant.INFO)
                        }
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp)) // FAB padding
            }
        }
    }

    if (showAddProviderDialog) {
        ProviderConfigDialog(
            provider = editingProvider,
            onDismiss = { showAddProviderDialog = false; editingProvider = null },
            onSave = { updatedProvider ->
                repository.saveAiProvider(updatedProvider)
                providers = repository.getAiProviders()
                showAddProviderDialog = false
                editingProvider = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigDialog(
    provider: AIProviderConfig?,
    onDismiss: () -> Unit,
    onSave: (AIProviderConfig) -> Unit
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    var type by remember { mutableStateOf(provider?.type ?: AIProviderType.OPENAI_COMPATIBLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == null) "Add AI Provider" else "Edit Provider", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Provider Name (e.g. My NVIDIA Server)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Provider Type", style = IeTypography.tableHeader, color = StitchSlate500)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIProviderType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StitchCobalt100, selectedLabelColor = StitchCobalt700)
                        )
                    }
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL / Endpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.openai.com/v1") }
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(AIProviderConfig(
                        id = provider?.id ?: "P-${System.currentTimeMillis()}",
                        name = name,
                        type = type,
                        baseUrl = baseUrl,
                        apiKey = apiKey.ifBlank { null }
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) {
                Text("Save Provider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
