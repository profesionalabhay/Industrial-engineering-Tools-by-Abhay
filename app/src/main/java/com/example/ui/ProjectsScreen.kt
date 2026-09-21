package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val plants by viewModel.plants.collectAsState()
    val lines by viewModel.lines.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manufacturing Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Manage plants, lines, and projects", style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add New")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchWhite)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = StitchCobalt600,
                contentColor = StitchWhite,
                shape = IeRadius.buttonShape,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New ${if (selectedTab == 0) "Project" else "Plant/Line"}") }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = StitchSlate50,
                contentColor = StitchCobalt600,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = StitchCobalt600
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(Modifier.padding(16.dp)) { Text("Projects") }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(Modifier.padding(16.dp)) { Text("Plants & Lines") }
                }
            }

            if (selectedTab == 0) {
                if (projects.isEmpty()) {
                    IeEmptyState(
                        title = "No Projects Found",
                        message = "Start by creating a project to optimize your manufacturing operations.",
                        icon = Icons.Default.Inventory
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                isSelected = project.id == currentProject?.id,
                                onSelect = { viewModel.selectProject(project) }
                            )
                        }
                    }
                }
            } else {
                PlantAndLineList(plants, lines)
            }
        }
    }

    if (showCreateDialog) {
        if (selectedTab == 0) {
            CreateProjectDialog(
                plants = plants,
                lines = lines,
                onDismiss = { showCreateDialog = false },
                onCreate = { name, lineId, desc ->
                    viewModel.createProject(Project(
                        id = "P-${UUID.randomUUID().toString().take(6)}",
                        lineId = lineId,
                        name = name,
                        description = desc,
                        status = "Active"
                    ))
                    showCreateDialog = false
                }
            )
        } else {
            CreatePlantLineDialog(
                onDismiss = { showCreateDialog = false },
                onCreatePlant = { name, loc ->
                    viewModel.createPlant(Plant("PLT-${UUID.randomUUID().toString().take(4)}", name, loc))
                    showCreateDialog = false
                },
                onCreateLine = { plantId, name ->
                    viewModel.createLine(Line("LN-${UUID.randomUUID().toString().take(4)}", plantId, name))
                    showCreateDialog = false
                },
                plants = plants
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StitchCobalt50 else Color.White
        ),
        border = if (isSelected) IeBorders.activeBorder else IeBorders.cardBorder,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchSlate900
                )
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate500,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                IeBadge(project.status, variant = if (project.status == "Active") IeBadgeVariant.SUCCESS else IeBadgeVariant.DEFAULT)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = StitchCobalt600)
            }
        }
    }
}

@Composable
fun PlantAndLineList(plants: List<Plant>, lines: List<Line>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (plants.isEmpty()) {
            item {
                IeEmptyState(
                    title = "No Plants Defined",
                    message = "Define your manufacturing facilities to organize your assembly lines.",
                    icon = Icons.Default.Factory
                )
            }
        } else {
            items(plants) { plant ->
                Column {
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate900
                    )
                    Text(plant.location, style = MaterialTheme.typography.bodySmall, color = StitchSlate500)
                    Spacer(Modifier.height(8.dp))
                    
                    val plantLines = lines.filter { it.plantId == plant.id }
                    if (plantLines.isEmpty()) {
                        Text("No lines configured for this plant.", style = MaterialTheme.typography.bodySmall, color = StitchSlate400)
                    } else {
                        plantLines.forEach { line ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = StitchSlate50),
                                shape = RoundedCornerShape(IeRadius.sm)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LinearScale, contentDescription = null, tint = StitchSlate400, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(line.name, style = MaterialTheme.typography.bodyMedium, color = StitchSlate700)
                                }
                            }
                        }
                    }
                }
                Divider(Modifier.padding(vertical = 12.dp), color = StitchSlate100)
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    plants: List<Plant>,
    lines: List<Line>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedLineId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                Text("Select Assembly Line", style = MaterialTheme.typography.labelMedium)
                if (lines.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("No lines available. Create a plant and line first.", color = StitchNvaRed, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { 
                                // Switch to tab 1 and show the other dialog or just handle it here
                                // For simplicity, we'll suggest switching tabs or provide a quick action
                            },
                            shape = IeRadius.buttonShape
                        ) {
                            Text("Configure Manufacturing Line")
                        }
                    }
                } else {
                    lines.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLineId = line.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedLineId == line.id, onClick = { selectedLineId = line.id })
                            Spacer(Modifier.width(8.dp))
                            Text(line.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedLineId, desc) },
                enabled = name.isNotEmpty() && selectedLineId.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreatePlantLineDialog(
    onDismiss: () -> Unit,
    onCreatePlant: (String, String) -> Unit,
    onCreateLine: (String, String) -> Unit,
    plants: List<Plant>
) {
    var isCreatingPlant by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedPlantId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreatingPlant) "Add New Plant" else "Add New Line") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    FilterChip(selected = isCreatingPlant, onClick = { isCreatingPlant = true }, label = { Text("Plant") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !isCreatingPlant, onClick = { isCreatingPlant = false }, label = { Text("Line") })
                }
                
                if (isCreatingPlant) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plant Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Line Name") }, modifier = Modifier.fillMaxWidth())
                    Text("Select Plant", style = MaterialTheme.typography.labelMedium)
                    plants.forEach { plant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlantId = plant.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedPlantId == plant.id, onClick = { selectedPlantId = plant.id })
                            Spacer(Modifier.width(8.dp))
                            Text(plant.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (isCreatingPlant) onCreatePlant(name, location) else onCreateLine(selectedPlantId, name)
                },
                enabled = if (isCreatingPlant) name.isNotEmpty() else name.isNotEmpty() && selectedPlantId.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
