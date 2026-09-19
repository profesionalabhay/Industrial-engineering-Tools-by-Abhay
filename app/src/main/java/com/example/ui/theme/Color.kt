package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// STITCH "PRECISION INDUSTRIAL OPERATIONS" DESIGN TOKENS
// =============================================================================

// Slate & Steel Neutral Palette (Light)
val StitchSlate950 = Color(0xFF020617)
val StitchSlate900 = Color(0xFF0F172A) // Primary text & header brand
val StitchSlate800 = Color(0xFF1E293B) // Dark container & heavy borders
val StitchSlate700 = Color(0xFF334155) // Secondary text
val StitchSlate600 = Color(0xFF475569) // Muted text & icons
val StitchSlate500 = Color(0xFF64748B) // Subtle metadata
val StitchSlate400 = Color(0xFF94A3B8) // Disabled text & subtle borders
val StitchSlate300 = Color(0xFFCBD5E1) // Standard element borders
val StitchSlate200 = Color(0xFFE2E8F0) // Surface dividers & card outlines
val StitchSlate100 = Color(0xFFF1F5F9) // Surface secondary & table headers
val StitchSlate50  = Color(0xFFF8FAFC) // App canvas background
val StitchWhite    = Color(0xFFFFFFFF) // Surface card white

// Precision Industrial Brand Accents
val StitchCobalt600 = Color(0xFF0284C7) // Action & highlight cobalt blue
val StitchCobalt700 = Color(0xFF0369A1)
val StitchCobalt500 = Color(0xFF0EA5E9)
val StitchCobalt100 = Color(0xFFE0F2FE) // Primary focus tint
val StitchCobalt50  = Color(0xFFF0F9FF)

// Lean Value Analysis Unified Classification
val StitchVaGreen        = Color(0xFF10B981) // Value-Added (Emerald 500)
val StitchVaGreenDark    = Color(0xFF059669)
val StitchVaGreenLight   = Color(0xFFECFDF5) // Emerald 50
val StitchVaGreenText    = Color(0xFF065F46)

val StitchNnvaAmber      = Color(0xFFF59E0B) // Necessary Non-Value Added (Amber 500)
val StitchNnvaAmberDark  = Color(0xFFD97706)
val StitchNnvaAmberLight = Color(0xFFFFFBEB) // Amber 50
val StitchNnvaAmberText  = Color(0xFF92400E)

val StitchNvaRed         = Color(0xFFEF4444) // Non-Value Added / Waste (Rose 500)
val StitchNvaRedDark     = Color(0xFFDC2626)
val StitchNvaRedLight    = Color(0xFFFEF2F2) // Red 50
val StitchNvaRedText     = Color(0xFF991B1B)

// Takt Time Reference Line & Cycle Targets
val StitchTaktLineIndigo = Color(0xFF6366F1) // Indigo 500
val StitchTaktLineLight  = Color(0xFFEEF2FF)
val StitchTaktLineText   = Color(0xFF3730A3)

// Material 3 Color Mappings - Light Theme
val PrimaryLight = StitchSlate900
val OnPrimaryLight = StitchWhite
val PrimaryContainerLight = StitchCobalt100
val OnPrimaryContainerLight = StitchCobalt700

val SecondaryLight = StitchCobalt600
val OnSecondaryLight = StitchWhite
val SecondaryContainerLight = StitchSlate100
val OnSecondaryContainerLight = StitchSlate800

val TertiaryLight = StitchSlate700
val OnTertiaryLight = StitchWhite
val TertiaryContainerLight = StitchSlate100
val OnTertiaryContainerLight = StitchSlate900

val ErrorLight = StitchNvaRed
val OnErrorLight = StitchWhite
val ErrorContainerLight = StitchNvaRedLight
val OnErrorContainerLight = StitchNvaRedText

val BackgroundLight = StitchSlate50
val OnBackgroundLight = StitchSlate900
val SurfaceLight = StitchWhite
val OnSurfaceLight = StitchSlate900
val SurfaceVariantLight = StitchSlate100
val OnSurfaceVariantLight = StitchSlate700
val OutlineLight = StitchSlate200

// Material 3 Color Mappings - Dark Theme
val PrimaryDark = StitchCobalt500
val OnPrimaryDark = StitchSlate950
val PrimaryContainerDark = Color(0xFF0C4A6E)
val OnPrimaryContainerDark = StitchCobalt100

val SecondaryDark = Color(0xFF94A3B8)
val OnSecondaryDark = StitchSlate950
val SecondaryContainerDark = Color(0xFF1E293B)
val OnSecondaryContainerDark = Color(0xFFE2E8F0)

val TertiaryDark = Color(0xFFCBD5E1)
val OnTertiaryDark = StitchSlate950
val TertiaryContainerDark = Color(0xFF334155)
val OnTertiaryContainerDark = StitchWhite

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val BackgroundDark = Color(0xFF090D16)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF0F172A)
val OnSurfaceDark = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFFCBD5E1)
val OutlineDark = Color(0xFF334155)
