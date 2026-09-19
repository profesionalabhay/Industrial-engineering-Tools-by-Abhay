package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    data object TimeStudy : Screen("time-study", "Time Study", Icons.Default.Timer)
    data object VideoStudy : Screen("video-study", "Video Study", Icons.Default.Videocam)
    data object Yamazumi : Screen("yamazumi", "Yamazumi", Icons.Default.BarChart)
    data object WorkBalance : Screen("work-balance", "Work Balance", Icons.Default.Sync)
    data object LineBalance : Screen("line-balance", "Line Balance", Icons.Default.LinearScale)
    data object WhatIf : Screen("what-if", "What-If", Icons.Default.Science)
    data object Vsm : Screen("vsm", "VSM", Icons.Default.AccountTree)
    data object Motion : Screen("motion", "Motion", Icons.Default.DirectionsRun)
    data object Spaghetti : Screen("spaghetti", "Spaghetti", Icons.Default.Map)
    data object Layout : Screen("layout", "Layout", Icons.Default.DashboardCustomize)
    data object Capacity : Screen("capacity", "Capacity", Icons.Default.Factory)
    data object Manpower : Screen("manpower", "Manpower", Icons.Default.People)
    data object MultiModel : Screen("multi-model", "Multi-Model", Icons.Default.DynamicFeed)
    data object Oee : Screen("oee", "OEE", Icons.Default.Analytics)
    data object Kaizen : Screen("kaizen", "Kaizen", Icons.Default.Build)
    data object StandardWork : Screen("standard-work", "Standard Work", Icons.AutoMirrored.Filled.List)
    data object Ergonomics : Screen("ergonomics", "Ergonomics", Icons.Default.Accessibility)
    data object Savings : Screen("savings", "Savings", Icons.Default.AttachMoney)
    data object AiCopilot : Screen("ai-copilot", "AI Copilot", Icons.Default.AutoAwesome)
    data object ManualStudy : Screen("manual-study", "Manual Study", Icons.Default.Timer)
    data object Simulation : Screen("simulation", "Simulation", Icons.Default.PlayCircleFilled)
    data object Enterprise : Screen("enterprise", "Enterprise", Icons.Default.Public)
    data object Reports : Screen("reports", "Reports", Icons.Default.Summarize)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object About : Screen("about", "About", Icons.Default.Info)
}

val screens = listOf(
    Screen.Dashboard,
    Screen.Projects,
    Screen.TimeStudy,
    Screen.VideoStudy,
    Screen.ManualStudy,
    Screen.Yamazumi,
    Screen.WorkBalance,
    Screen.LineBalance,
    Screen.WhatIf,
    Screen.Simulation,
    Screen.Vsm,
    Screen.Motion,
    Screen.Spaghetti,
    Screen.Layout,
    Screen.Capacity,
    Screen.Manpower,
    Screen.MultiModel,
    Screen.Oee,
    Screen.Kaizen,
    Screen.StandardWork,
    Screen.Ergonomics,
    Screen.Savings,
    Screen.AiCopilot,
    Screen.Enterprise,
    Screen.Reports,
    Screen.Settings,
    Screen.About
)

data class NavSection(val title: String, val screens: List<Screen>)

val navSections = listOf(
    NavSection("OPERATIONS & MEASUREMENT", listOf(Screen.Dashboard, Screen.Projects, Screen.TimeStudy, Screen.VideoStudy, Screen.ManualStudy, Screen.StandardWork)),
    NavSection("LINE BALANCING & SIMULATION", listOf(Screen.Yamazumi, Screen.WorkBalance, Screen.LineBalance, Screen.WhatIf, Screen.Simulation)),
    NavSection("CAPACITY & LEAN", listOf(Screen.Capacity, Screen.Manpower, Screen.MultiModel, Screen.Oee, Screen.Kaizen)),
    NavSection("DIAGRAMS & MOTION", listOf(Screen.Vsm, Screen.Motion, Screen.Spaghetti, Screen.Layout, Screen.Ergonomics)),
    NavSection("INTELLIGENCE & SYSTEM", listOf(Screen.AiCopilot, Screen.Enterprise, Screen.Savings, Screen.Reports, Screen.Settings, Screen.About))
)
