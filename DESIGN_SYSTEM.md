# INDUSTRIAL ENGINEERING (IE) COPILOT — DESIGN SYSTEM SPECIFICATION
**Version:** 1.0 (Design-System Lock)  
**Visual Foundation:** Google Stitch "Precision Industrial Operations" Design Language  
**Target Platform:** Jetpack Compose (Material Design 3 with Strict Industrial Tokens)

---

## 1. Design Philosophy & Purpose

The **IE Copilot Design System** is built specifically for **Industrial Engineering, Lean Manufacturing, Operations Management, and Continuous Improvement (Kaizen)** professionals.

### Core Principles
1. **Precision & Density over Whimsy**: Prioritize high-information-density layouts, scannable tabular grids, and crisp micro-typography. Avoid generic consumer-app aesthetics, rounded cartoon bubbles, or decorative AI chatbot fluff.
2. **Visual Hierarchy of Production**: Real-time operations require instantaneous recognition of performance gaps. Bottlenecks, takt violations, non-value-added waste, and sample insufficiency must be identifiable within 200 milliseconds.
3. **Monospace Numerical Fidelity**: All cycle times, standard times, takt times, efficiencies, and statistical confidence values **must** use tabular monospace numerals (`Roboto Mono` / `FontFamily.Monospace`) to ensure column alignment and zero visual jitter.
4. **Standardized Lean Chromatic Semantics**: Lean Manufacturing universally recognizes Value-Added (VA), Necessary Non-Value-Added (NNVA), and Non-Value-Added (NVA) classifications. Colors must strictly adhere to these industrial semantics.

---

## 2. Color Palette & Semantic Tokens

### 2.1 Neutral Slate Foundation
Industrial operational tools require calm, high-contrast, low-eye-strain neutral surfaces.

| Token | Hex | Role |
|---|---|---|
| `StitchSlate950` | `#020617` | Deepest contrast accents, dark drawer headers |
| `StitchSlate900` | `#0F172A` | Primary text, high-emphasis headers, active buttons |
| `StitchSlate800` | `#1E293B` | Secondary headings, active navigation text |
| `StitchSlate700` | `#334155` | Body text, interactive icons, active labels |
| `StitchSlate600` | `#475569` | Secondary captions, units of measurement, icons |
| `StitchSlate500` | `#64748B` | Table column headers, muted subtitles, borders |
| `StitchSlate400` | `#94A3B8` | Inactive switch tracks, disabled borders |
| `StitchSlate300` | `#CBD5E1` | Standard card borders (hover/focused) |
| `StitchSlate200` | `#E2E8F0` | Default 1dp dividing lines and card borders |
| `StitchSlate100` | `#F1F5F9` | Table header background, subtle item containers |
| `StitchSlate50`  | `#F8FAFC` | Primary application canvas background |
| `StitchWhite`    | `#FFFFFF` | Card surface, dialog canvas, elevated panels |

### 2.2 Brand & Operational Cobalt
| Token | Hex | Role |
|---|---|---|
| `StitchCobalt700` | `#1D4ED8` | Primary button hover, high-contrast active tabs |
| `StitchCobalt600` | `#2563EB` | Primary interactive elements, focused nodes, brand icons |
| `StitchCobalt500` | `#3B82F6` | Secondary active indicators |
| `StitchCobalt100` | `#DBEAFE` | Selected filter chip container, subtle selection highlight |
| `StitchCobalt50`  | `#EFF6FF` | Selected row highlight, active navigation drawer item |

### 2.3 Lean Value Analysis (Universal Industrial Semantics)
**MANDATORY**: Never use random colors or swap these semantics.

#### Value-Added (VA) — Emerald Green
*Physical transformation of the product that the customer is willing to pay for.*
- **Base:** `StitchVaGreen` (`#10B981`)
- **Dark/Text:** `StitchVaGreenText` (`#047857`)
- **Container/Tint:** `StitchVaGreenLight` (`#ECFDF5`)

#### Necessary Non-Value-Added (NNVA) — Industrial Amber / Ochre
*Required under current operating conditions (inspection, transport, setup, safety).*
- **Base:** `StitchNnvaAmber` (`#F59E0B`)
- **Dark/Text:** `StitchNnvaAmberText` (`#B45309`)
- **Container/Tint:** `StitchNnvaAmberLight` (`#FFFBEB`)

#### Non-Value-Added / Waste (NVA) — Crimson / Rose
*Pure waste (waiting, rework, excessive motion, defects, unnecessary delays).*
- **Base:** `StitchNvaRed` (`#EF4444`)
- **Dark/Text:** `StitchNvaRedText` (`#B91C1C`)
- **Container/Tint:** `StitchNvaRedLight` (`#FEF2F2`)

#### Takt Time & Target Lines — Deep Indigo
- **Line Stroke:** `StitchTaktLineIndigo` (`#4F46E5`)
- **Text/Badge:** `StitchTaktLineText` (`#3730A3`)
- **Container:** `StitchTaktLineLight` (`#EEF2FF`)

---

## 3. Typography Hierarchy (`IeTypography`)

The system uses clean system sans-serif for reading and system monospace for all engineering data.

| Style Name | Font / Family | Size | Weight | Tracking | Usage |
|---|---|---|---|---|---|
| `screenTitle` | Sans-Serif | 20sp | Bold (700) | -0.2sp | Screen and view headings |
| `cardTitle` | Sans-Serif | 14sp | Bold (700) | 0.0sp | Card headers, section titles |
| `tableHeader` | Sans-Serif | 11sp | Bold (700) | 0.8sp (ALL CAPS) | Table headers, palette categories |
| `body` | Sans-Serif | 13sp | Normal (400) | 0.1sp | Standard explanatory text |
| `caption` | Sans-Serif | 11sp | Medium (500) | 0.0sp | Subtitles, helper text |
| `badgeText` | Sans-Serif | 11sp | Bold (700) | 0.3sp | Pills, status chips |
| `kpiLarge` | Monospace | 28sp | Bold (700) | -0.5sp | Primary dashboard metrics |
| `kpiMedium` | Monospace | 20sp | Bold (700) | -0.2sp | Compact card metrics |
| `dataMono` | Monospace | 12sp | Normal (400) | 0.0sp | Cycle times, standard times, inputs |
| `dataMonoBold` | Monospace | 12sp | Bold (700) | 0.0sp | Column totals, key durations |

---

## 4. Spacing, Elevation & Corner Radii

### 4.1 Spacing Grid (`IeSpacing`)
Based strictly on an 4dp/8dp engineering grid:
- `xxs`: 2dp (micro-separators)
- `xs`: 4dp (icon-to-text gaps)
- `sm`: 8dp (component inner spacing)
- `md`: 12dp (standard card item vertical spacing)
- `lg`: 16dp (screen margins, card internal padding)
- `xl`: 20dp (major section dividers)
- `screenPadding`: 16dp

### 4.2 Elevation & Borders (`IeBorders`)
- **Elevation**: Kept low (0dp to 1dp) to ensure clean industrial paper/sheet feel.
- **Borders**: All cards, inputs, and tables use crisp 1dp borders with `StitchSlate200` (`IeBorders.cardBorder`).

### 4.3 Corner Radii (`IeRadius`)
- `cardShape`: `RoundedCornerShape(6.dp)`
- `buttonShape`: `RoundedCornerShape(6.dp)`
- `inputShape`: `RoundedCornerShape(6.dp)`
- `badgeShape`: `RoundedCornerShape(4.dp)`
- `tableShape`: `RoundedCornerShape(6.dp)`
- `dialogShape`: `RoundedCornerShape(8.dp)`

---

## 5. Standard Reusable Component Library (`Components.kt`)

All screens must use the standardized `Ie*` components:

### 5.1 `IeCard`
```kotlin
IeCard(modifier = Modifier.fillMaxWidth()) {
    // Content inside a white, 1dp slate-bordered card
}
```

### 5.2 `IeKpiCard`
Used for metrics across all analytical modules.
```kotlin
IeKpiCard(
    title = "Cycle Time",
    value = "45.2",
    unit = "s",
    trend = "+2.1%",
    isPositiveTrend = false,
    isAlert = false,
    subtitle = "Station Bottleneck"
)
```

### 5.3 `IeClassificationBadge`
Visual indicator for VA, NNVA, or NVA work elements.
```kotlin
IeClassificationBadge(
    valueClassification = ValueClassification.VA,
    timeText = "32.4s"
)
```

### 5.4 `IeBadge`
Status pills for scenario states, sufficiency, or errors.
- Variants: `PRIMARY`, `SUCCESS`, `WARNING`, `ERROR`, `INFO`, `NEUTRAL`.

### 5.5 `IeTable`
Tabular view with fixed headers, zebra striping, and right-aligned monospace numerals.

### 5.6 `IeChartContainer`
Encapsulates canvas charts with standard title, legend, and 1dp border.

---

## 6. Layout Shell & Navigation

- **Top Navigation Bar**: Fixed at 48dp with context breadcrumb (`Plant > Line > Project`), scenario badge, and clean icon buttons.
- **Navigation Drawer**: 280dp wide, dark slate header with plant overview, grouped analytical modules with icons and active cobalt pill states.
- **Screen Viewport**: Full edge-to-edge layout with `Scaffold` handling window insets.

---

## 7. Version 2 Evolution Guardrails (Lock Rules)

1. **NO Arbitrary Themes**: Do NOT reintroduce purple/violet Material 3 default colors or rounded playful shapes.
2. **NO Logic Regressions**: Time calculations, Yamazumi stacking, What-If simulator formulas, and takt formulas must remain mathematically rigorous.
3. **Strict Visual Source of Truth**: Any newly created V2 screen (e.g. standard work sheet generator, ergonomic RULA assessment) must strictly inherit `IeSpacing`, `IeRadius`, `IeTypography`, and `IeCard` components.
