package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ManufacturingApplication
import com.example.data.ManufacturingRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as ManufacturingApplication
    val repository = application.repository
    val factory = ViewModelFactory(repository)

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val projectViewModel: ProjectViewModel = viewModel(factory = factory)
    val timeStudyViewModel: TimeStudyViewModel = viewModel(factory = factory)
    val videoStudyViewModel: VideoStudyViewModel = viewModel(factory = factory)
    val yamazumiViewModel: YamazumiViewModel = viewModel(factory = factory)
    val workBalanceViewModel: WorkBalanceViewModel = viewModel(factory = factory)
    val whatIfViewModel: WhatIfViewModel = viewModel(factory = factory)
    val vsmViewModel: VsmViewModel = viewModel(factory = factory)
    val motionViewModel: MotionViewModel = viewModel(factory = factory)
    val spaghettiViewModel: SpaghettiViewModel = viewModel(factory = factory)
    val capacityViewModel: CapacityViewModel = viewModel(factory = factory)
    val multiModelViewModel: MultiModelViewModel = viewModel(factory = factory)
    val oeeViewModel: OeeViewModel = viewModel(factory = factory)
    val rcaViewModel: RcaViewModel = viewModel(factory = factory)
    val kaizenViewModel: KaizenViewModel = viewModel(factory = factory)
    val standardWorkViewModel: StandardWorkViewModel = viewModel(factory = factory)
    val opexDashboardViewModel: OpExDashboardViewModel = viewModel(factory = factory)
    val ergoViewModel: ErgoViewModel = viewModel(factory = factory)
    val aiCopilotViewModel: AiCopilotViewModel = viewModel(factory = factory)
    val manualTimeStudyViewModel: ManualTimeStudyViewModel = viewModel(factory = factory)
    val simulationViewModel: SimulationViewModel = viewModel(factory = factory)
    val enterpriseViewModel: EnterpriseViewModel = viewModel(factory = factory)
    val savingsViewModel: SavingsViewModel = viewModel(factory = factory)
    
    // Initialize ViewModels with default project
    LaunchedEffect(Unit) {
        val defaultProjectId = "P-001"
        projectViewModel.selectProjectById(defaultProjectId)
        timeStudyViewModel.initialize(defaultProjectId)
        videoStudyViewModel.initialize(defaultProjectId)
        yamazumiViewModel.initialize(defaultProjectId)
        workBalanceViewModel.initialize(defaultProjectId)
        oeeViewModel.initialize(defaultProjectId)
        kaizenViewModel.initialize(defaultProjectId)
        rcaViewModel.initialize(defaultProjectId)
        opexDashboardViewModel.initialize(defaultProjectId)
        ergoViewModel.initialize(defaultProjectId)
        aiCopilotViewModel.initialize(defaultProjectId)
        vsmViewModel.initialize(defaultProjectId)
        spaghettiViewModel.initialize(defaultProjectId)
        motionViewModel.initialize(defaultProjectId, "WE-001") 
        multiModelViewModel.initialize(defaultProjectId)
        whatIfViewModel.initialize(defaultProjectId)
        savingsViewModel.initialize(defaultProjectId)
        capacityViewModel.initialize(defaultProjectId)
        standardWorkViewModel.initialize(defaultProjectId)
        manualTimeStudyViewModel.initialize("VS-001")
        simulationViewModel.initialize(defaultProjectId)
        enterpriseViewModel.initialize()
        
        // Seed data if database is empty
        repository.seedSampleData()
    }

    val currentProject by projectViewModel.currentProject.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    var projectDropdownExpanded by remember { mutableStateOf(false) }

    // Find the category of the current screen for breadcrumbs
    val currentSection = navSections.find { section -> section.screens.any { it.route == currentRoute } }
    val currentScreen = screens.find { it.route == currentRoute } ?: Screen.Dashboard

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = StitchWhite,
                drawerContentColor = StitchSlate900,
                modifier = Modifier.width(320.dp)
            ) {
                // Technical Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StitchSlate900)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(StitchCobalt600, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = StitchWhite, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "IE COPILOT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchWhite,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Developed by Abhay Singh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StitchSlate400,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Surface(
                            shape = IeRadius.badgeShape,
                            color = StitchCobalt700,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "V2.6 RELEASE",
                                style = IeTypography.badgeText,
                                color = StitchWhite,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = StitchSlate200)

                // Categorized Navigation List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    navSections.forEach { section ->
                        item {
                            Text(
                                text = section.title,
                                style = IeTypography.tableHeader,
                                color = StitchSlate400,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        items(section.screens.size) { idx ->
                            val screen = section.screens[idx]
                            val isSelected = currentRoute == screen.route
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .clip(IeRadius.cardShape)
                                    .clickable {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                color = if (isSelected) StitchCobalt100 else Color.Transparent,
                                shape = IeRadius.cardShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) StitchCobalt700 else StitchSlate600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = screen.title,
                                        style = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StitchCobalt700 else StitchSlate800
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    // Precision Industrial Workbench TopBar
                    TopAppBar(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                    Surface(
                                        shape = IeRadius.cardShape,
                                        color = StitchSlate800,
                                        border = BorderStroke(1.dp, StitchSlate700),
                                        modifier = Modifier.clickable { projectDropdownExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(StitchVaGreen)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = currentProject.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = StitchWhite
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Project",
                                                tint = StitchSlate400,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = projectDropdownExpanded,
                                        onDismissRequest = { projectDropdownExpanded = false }
                                    ) {
                                        projects.forEach { project ->
                                            DropdownMenuItem(
                                                text = { Text(project.name, style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    projectViewModel.selectProject(project)
                                                    projectDropdownExpanded = false
                                                    scope.launch { snackbarHostState.showSnackbar("Active project: ${project.name}") }
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Surface(
                                    shape = IeRadius.badgeShape,
                                    color = StitchSlate800,
                                    border = BorderStroke(1.dp, StitchSlate700)
                                ) {
                                    Text(
                                        text = "STATION 04 • ASSEMBLY",
                                        style = IeTypography.dataMono,
                                        fontSize = 11.sp,
                                        color = StitchSlate300,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = StitchWhite)
                            }
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Workbench search active") } }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = StitchSlate300)
                            }
                            IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("All line parameters nominal") } }) {
                                BadgedBox(badge = { 
                                    Surface(
                                        shape = CircleShape,
                                        color = StitchNvaRed,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = StitchSlate300)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp, start = 4.dp)
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StitchCobalt600),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("IE", color = StitchWhite, style = IeTypography.dataMonoBold, fontSize = 11.sp)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StitchSlate900,
                            titleContentColor = StitchWhite,
                            navigationIconContentColor = StitchWhite,
                            actionIconContentColor = StitchSlate300
                        )
                    )

                    // Engineering Breadcrumb Bar
                    Surface(
                        color = StitchSlate100,
                        border = BorderStroke(1.dp, StitchSlate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IE COPILOT",
                                style = IeTypography.breadcrumb,
                                color = StitchSlate500
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = StitchSlate400
                            )
                            if (currentSection != null) {
                                Text(
                                    text = currentSection.title,
                                    style = IeTypography.breadcrumb,
                                    color = StitchSlate500
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = StitchSlate400
                                )
                            }
                            Text(
                                text = currentScreen.title.uppercase(),
                                style = IeTypography.breadcrumb,
                                color = StitchCobalt700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            containerColor = StitchSlate50
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) { 
                    DashboardScreen(onNavigateToMultiModel = { navController.navigate(Screen.MultiModel.route) }) 
                }
                composable(Screen.Projects.route) { PlaceholderScreen(Screen.Projects) }
                composable(Screen.TimeStudy.route) { TimeStudyScreen(timeStudyViewModel) }
                composable(Screen.VideoStudy.route) { VideoStudyScreen(videoStudyViewModel) }
                composable(Screen.ManualStudy.route) { 
                    ManualTimeStudyScreen(manualTimeStudyViewModel, "VS-001") 
                }
                composable(Screen.Yamazumi.route) { YamazumiScreen(yamazumiViewModel) }
                composable(Screen.WorkBalance.route) { WorkBalanceScreen(workBalanceViewModel) }
                composable(Screen.LineBalance.route) { PlaceholderScreen(Screen.LineBalance) }
                composable(Screen.WhatIf.route) { WhatIfScreen(whatIfViewModel) }
                composable(Screen.Simulation.route) { 
                    SimulationScreen(simulationViewModel, currentProject.id) 
                }
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
                composable(Screen.MultiModel.route) { MultiModelScreen(multiModelViewModel) }
                composable(Screen.Oee.route) { 
                    OeeScreen(oeeViewModel, currentProject.id) 
                }
                composable(Screen.Kaizen.route) { 
                    KaizenScreen(kaizenViewModel, currentProject.id) 
                }
                composable(Screen.StandardWork.route) { 
                    StandardWorkScreen(
                        viewModel = standardWorkViewModel,
                        stationId = "ST-04",
                        modelId = "MDL-1"
                    ) 
                }
                composable(Screen.Ergonomics.route) { 
                    ErgoScreen(ergoViewModel, currentProject.id)
                }
                composable(Screen.Savings.route) { 
                    SavingsScreen(savingsViewModel) 
                }
                composable(Screen.AiCopilot.route) { 
                    AiCopilotScreen(
                        viewModel = aiCopilotViewModel,
                        projectId = currentProject.id
                    ) 
                }
                composable(Screen.Enterprise.route) { 
                    EnterpriseAnalyticsScreen(enterpriseViewModel) 
                }
                composable(Screen.Reports.route) { 
                    OpExDashboardScreen(
                        viewModel = opexDashboardViewModel,
                        projectId = currentProject.id,
                        onNavigateToOee = { navController.navigate(Screen.Oee.route) },
                        onNavigateToKaizen = { navController.navigate(Screen.Kaizen.route) },
                        onNavigateToRca = { navController.navigate(Screen.Kaizen.route) } // RCA is in Kaizen/Loss flow
                    )
                }
                composable(Screen.Settings.route) { 
                    AiSettingsScreen(repository = repository) 
                }
                composable(Screen.About.route) {
                    AboutScreen()
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(onNavigateToMultiModel: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf("LINE OVERVIEW", "ANALYTICS & TAKT", "BOTTLENECK REPORTS")
            
            Surface(
                shape = IeRadius.cardShape,
                border = IeBorders.cardBorder,
                color = StitchWhite
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = StitchWhite,
                    contentColor = StitchCobalt700,
                    divider = { HorizontalDivider(color = StitchSlate200) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title,
                                    style = IeTypography.tableHeader,
                                    color = if (selectedTabIndex == index) StitchCobalt700 else StitchSlate600
                                ) 
                            }
                        )
                    }
                }
            }
        }

        // Multi-Model Production Mix KPI Card
        item {
            IeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Mixed-Model Production Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate900
                                )
                                Spacer(Modifier.width(8.dp))
                                IeBadge("3 Models Active", variant = IeBadgeVariant.PRIMARY)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Delta Standard (50%) • Delta Sport (30%) • Delta Pro (20%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSlate500
                            )
                        }

                        Button(
                            onClick = onNavigateToMultiModel,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                            shape = IeRadius.buttonShape
                        ) {
                            Text("Open Balancing", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IeKpiCard(
                            title = "Line Takt",
                            value = "27.0s",
                            subtitle = "1000 units/shift",
                            modifier = Modifier.weight(1f)
                        )
                        IeKpiCard(
                            title = "Weighted BE",
                            value = "88.6%",
                            isPositiveTrend = true,
                            trend = "Loss: 11.4%",
                            modifier = Modifier.weight(1f)
                        )
                        IeKpiCard(
                            title = "Weighted CT",
                            value = "23.9s",
                            unit = "sec",
                            modifier = Modifier.weight(1f)
                        )
                        IeKpiCard(
                            title = "Peak CT",
                            value = "31.0s",
                            unit = "sec",
                            subtitle = "Delta Pro @ ST-05",
                            isAlert = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IeKpiCard(
                    title = "OEE Overall",
                    value = "85.4%",
                    trend = "+2.1%",
                    isPositiveTrend = true,
                    subtitle = "Target: 85.0%",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Max Cycle Time",
                    value = "54.2s",
                    unit = "sec",
                    trend = "-1.8s",
                    isPositiveTrend = true,
                    subtitle = "Takt: 60.0s",
                    modifier = Modifier.weight(1f)
                )
                IeKpiCard(
                    title = "Daily Output",
                    value = "1,204",
                    unit = "units",
                    trend = "+42",
                    isPositiveTrend = true,
                    subtitle = "Plan: 1,180",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            IeChartContainer(
                title = "Line Production & Cycle Performance",
                subtitle = "Station Cycle Time vs. Takt Time (60.0s)",
                taktTime = 60.0,
                legendContent = {
                    IeLegendItem("VA (Value-Added)", IeLeanTokens.vaColor)
                    IeLegendItem("NNVA (Necessary)", IeLeanTokens.nnvaColor)
                    IeLegendItem("NVA (Waste)", IeLeanTokens.nvaColor)
                    IeLegendItem("Takt Time", IeLeanTokens.taktColor, isDashed = true)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Live Production Stream Monitoring Active • 12 Stations Online",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchSlate500
                    )
                }
            }
        }

        item {
            IeSectionHeader(
                title = "Station Status & Operator Assignments",
                subtitle = "Real-time balancing tracking"
            )
            IeTable(
                headers = listOf("Station", "Operator", "Cycle Time", "Takt Delta", "Status"),
                rows = listOf(
                    listOf("ST-01 Sub-assembly", "John D.", "52.4s", "-7.6s", "BALANCED"),
                    listOf("ST-02 Welding Rig", "Jane S.", "58.1s", "-1.9s", "NEAR TAKT"),
                    listOf("ST-03 Main Fasten", "Mike T.", "63.2s", "+3.2s", "OVER TAKT"),
                    listOf("ST-04 QC Inspection", "Sarah K.", "48.0s", "-12.0s", "BALANCED")
                ),
                columnWidths = listOf(140.dp, 100.dp, 80.dp, 80.dp, 100.dp),
                isNumericColumn = listOf(false, false, true, true, false)
            )
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    IeKpiCard(title = title, value = value, modifier = modifier)
}

@Composable
fun PlaceholderScreen(screen: Screen) {
    IeEmptyState(
        title = "${screen.title} Module",
        message = "This module is part of the IE Copilot precision engineering architecture. Connected to project data model.",
        icon = screen.icon,
        actionText = "Back to Dashboard",
        onAction = { /* No-op */ }
    )
}
