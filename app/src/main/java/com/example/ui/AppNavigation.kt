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
    data object Reports : Screen("reports", "Reports", Icons.Default.Summarize)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val screens = listOf(
    Screen.Dashboard,
    Screen.Projects,
    Screen.TimeStudy,
    Screen.VideoStudy,
    Screen.Yamazumi,
    Screen.WorkBalance,
    Screen.LineBalance,
    Screen.WhatIf,
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
    Screen.Reports,
    Screen.Settings
)
