package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val projectViewModel: ProjectViewModel = viewModel()
    val timeStudyViewModel: TimeStudyViewModel = viewModel()
    val videoStudyViewModel: VideoStudyViewModel = viewModel()
    val yamazumiViewModel: YamazumiViewModel = viewModel()
    val workBalanceViewModel: WorkBalanceViewModel = viewModel()
    val whatIfViewModel: WhatIfViewModel = viewModel()
    val vsmViewModel: VsmViewModel = viewModel()
    val motionViewModel: MotionViewModel = viewModel()
    val spaghettiViewModel: SpaghettiViewModel = viewModel()
    val capacityViewModel: CapacityViewModel = viewModel()
    val currentProject by projectViewModel.currentProject.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    var projectDropdownExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "IE Copilot",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                LazyColumn {
                    items(screens.size) { index ->
                        val screen = screens[index]
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = { 
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { projectDropdownExpanded = true }.padding(8.dp)
                                ) {
                                    Text(
                                        currentProject.name,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Project")
                                }
                                DropdownMenu(
                                    expanded = projectDropdownExpanded,
                                    onDismissRequest = { projectDropdownExpanded = false }
                                ) {
                                    projects.forEach { project ->
                                        DropdownMenuItem(
                                            text = { Text(project.name) },
                                            onClick = {
                                                projectViewModel.selectProject(project)
                                                projectDropdownExpanded = false
                                                scope.launch { snackbarHostState.showSnackbar("Project changed to ${project.name}") }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Search not implemented") } }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("No new notifications") } }) {
                                BadgedBox(badge = { Badge { Text("3") } }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                            IconButton(onClick = { /* TODO Settings */ }) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("IE", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    // Breadcrumbs area
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Home", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                screens.find { it.route == currentRoute }?.title ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) { DashboardScreen() }
                composable(Screen.Projects.route) { PlaceholderScreen(Screen.Projects) }
                composable(Screen.TimeStudy.route) { TimeStudyScreen(timeStudyViewModel) }
                composable(Screen.VideoStudy.route) { VideoStudyScreen(videoStudyViewModel) }
                composable(Screen.Yamazumi.route) { YamazumiScreen(yamazumiViewModel) }
                composable(Screen.WorkBalance.route) { WorkBalanceScreen(workBalanceViewModel) }
                composable(Screen.LineBalance.route) { PlaceholderScreen(Screen.LineBalance) }
                composable(Screen.WhatIf.route) { WhatIfScreen(whatIfViewModel) }
                composable(Screen.Vsm.route) { VsmScreen(vsmViewModel) }
                composable(Screen.Motion.route) { MotionScreen(motionViewModel) }
                composable(Screen.Spaghetti.route) { SpaghettiScreen(spaghettiViewModel) }
                composable(Screen.Layout.route) { PlaceholderScreen(Screen.Layout) }
                composable(Screen.Capacity.route) { CapacityScreen(capacityViewModel) }
                composable(Screen.Manpower.route) { 
                    ManpowerScreen(
                        viewModel = capacityViewModel,
                        onNavigateToWhatIf = { navController.navigate(Screen.WhatIf.route) }
                    ) 
                }
                composable(Screen.MultiModel.route) { PlaceholderScreen(Screen.MultiModel) }
                composable(Screen.Oee.route) { PlaceholderScreen(Screen.Oee) }
                composable(Screen.Kaizen.route) { PlaceholderScreen(Screen.Kaizen) }
                composable(Screen.StandardWork.route) { PlaceholderScreen(Screen.StandardWork) }
                composable(Screen.Ergonomics.route) { PlaceholderScreen(Screen.Ergonomics) }
                composable(Screen.Savings.route) { PlaceholderScreen(Screen.Savings) }
                composable(Screen.AiCopilot.route) { PlaceholderScreen(Screen.AiCopilot) }
                composable(Screen.Reports.route) { PlaceholderScreen(Screen.Reports) }
                composable(Screen.Settings.route) { PlaceholderScreen(Screen.Settings) }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf("Overview", "Analytics", "Reports")
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KpiCard("OEE", "85.4%", Modifier.weight(1f))
                KpiCard("Cycle Time", "45s", Modifier.weight(1f))
                KpiCard("Output", "1,204", Modifier.weight(1f))
            }
        }

        item {
            ChartContainer("Production Trend") {
                Text("Chart Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SectionHeader("Recent Observations")
            DataTable(
                headers = listOf("Station", "Operator", "Status"),
                rows = listOf(
                    listOf("St-04", "John D.", "Active"),
                    listOf("St-05", "Jane S.", "Idle"),
                    listOf("St-06", "Mike T.", "Active")
                )
            )
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PlaceholderScreen(screen: Screen) {
    EmptyState(
        title = "${screen.title} Module",
        message = "This module is part of the IE Copilot architecture but has not been implemented yet. It will connect to the central data model.",
        icon = screen.icon,
        actionText = "Go to Dashboard",
        onAction = { /* Handle navigation if needed, or leave dummy */ }
    )
}
