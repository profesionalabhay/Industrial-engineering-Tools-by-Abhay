package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ValueClassification
import com.example.ui.theme.*

// =============================================================================
// STITCH "PRECISION INDUSTRIAL OPERATIONS" REUSABLE COMPONENTS
// =============================================================================

/**
 * Standard Stitch Industrial Card with crisp 1dp slate border and 6dp radius.
 */
@Composable
fun IeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = StitchWhite,
    borderColor: Color = StitchSlate200,
    contentPadding: Dp = IeSpacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Industrial Engineering Metric / KPI Card.
 */
@Composable
fun IeKpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    unit: String? = null,
    trend: String? = null,
    isPositiveTrend: Boolean? = null,
    isAlert: Boolean = false,
    icon: ImageVector? = null
) {
    val borderColor = if (isAlert) StitchNvaRed else StitchSlate200
    val valueColor = if (isAlert) StitchNvaRed else StitchSlate900

    Card(
        modifier = modifier,
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = IeTypography.tableHeader,
                    color = StitchSlate500
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isAlert) StitchNvaRed else StitchSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val textStyle = when {
                value.length > 12 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                value.length > 8 -> IeTypography.kpiMedium
                else -> IeTypography.kpiLarge
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = textStyle,
                    color = valueColor,
                    maxLines = 1
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchSlate500,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            if (subtitle != null || trend != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trend != null) {
                        val trendColor = when (isPositiveTrend) {
                            true -> StitchVaGreen
                            false -> StitchNvaRed
                            null -> StitchSlate500
                        }
                        Text(
                            text = trend,
                            style = IeTypography.dataMonoBold,
                            color = trendColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchSlate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stitch Badge / Status Pill.
 */
enum class IeBadgeVariant {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

@Composable
fun IeBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: IeBadgeVariant = IeBadgeVariant.DEFAULT,
    icon: ImageVector? = null
) {
    val (bgColor, textColor, borderColor) = when (variant) {
        IeBadgeVariant.DEFAULT -> Triple(StitchSlate100, StitchSlate700, StitchSlate200)
        IeBadgeVariant.PRIMARY -> Triple(StitchCobalt100, StitchCobalt700, StitchCobalt500.copy(alpha = 0.3f))
        IeBadgeVariant.SUCCESS -> Triple(StitchVaGreenLight, StitchVaGreenText, StitchVaGreen.copy(alpha = 0.3f))
        IeBadgeVariant.WARNING -> Triple(StitchNnvaAmberLight, StitchNnvaAmberText, StitchNnvaAmber.copy(alpha = 0.3f))
        IeBadgeVariant.ERROR -> Triple(StitchNvaRedLight, StitchNvaRedText, StitchNvaRed.copy(alpha = 0.3f))
        IeBadgeVariant.INFO -> Triple(StitchTaktLineLight, StitchTaktLineText, StitchTaktLineIndigo.copy(alpha = 0.3f))
    }

    Surface(
        shape = IeRadius.badgeShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = IeTypography.badgeText,
                color = textColor
            )
        }
    }
}

/**
 * Standardized Lean Value Analysis Classification Pill (VA, NNVA, NVA).
 */
@Composable
fun IeClassificationBadge(
    valueClassification: ValueClassification,
    modifier: Modifier = Modifier,
    timeText: String? = null
) {
    val color = IeLeanTokens.getColorForValueClassification(valueClassification)
    val bg = IeLeanTokens.getContainerForValueClassification(valueClassification)
    val textCol = IeLeanTokens.getTextForValueClassification(valueClassification)

    Surface(
        shape = IeRadius.badgeShape,
        color = bg,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = valueClassification.name,
                style = IeTypography.badgeText,
                color = textCol
            )
            if (timeText != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeText,
                    style = IeTypography.dataMono,
                    color = textCol
                )
            }
        }
    }
}

/**
 * Standard Stitch Chart Frame with header, takt time pill, and unified legend.
 */
@Composable
fun IeChartContainer(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    taktTime: Double? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    legendContent: (@Composable RowScope.() -> Unit)? = null,
    chartContent: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        border = IeBorders.cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Chart Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate900
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchSlate500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (taktTime != null) {
                        Surface(
                            shape = IeRadius.badgeShape,
                            color = StitchTaktLineLight,
                            border = BorderStroke(1.dp, StitchTaktLineIndigo.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "TAKT: ${String.format("%.1fs", taktTime)}",
                                style = IeTypography.dataMonoBold,
                                color = StitchTaktLineText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (actions != null) {
                        actions()
                    }
                }
            }

            // Legend if provided
            if (legendContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    legendContent()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchSlate50, RoundedCornerShape(4.dp))
                    .border(BorderStroke(1.dp, StitchSlate200), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                content = chartContent
            )
        }
    }
}

/**
 * Standard Stitch Chart Legend Item.
 */
@Composable
fun IeLegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 6.dp)
    ) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = IeTypography.badgeText,
            color = StitchSlate700
        )
    }
}

/**
 * Standard Stitch Data Table with horizontal scroll support and technical header.
 */
@Composable
fun IeTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    columnWidths: List<Dp>? = null,
    isNumericColumn: List<Boolean>? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = IeRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = StitchWhite),
        border = IeBorders.cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(StitchSlate100)
                    .border(BorderStroke(1.dp, StitchSlate200))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { i, title ->
                    val colWidth = columnWidths?.getOrNull(i)
                    val align = if (isNumericColumn?.getOrNull(i) == true) TextAlign.End else TextAlign.Start
                    val cellModifier = if (colWidth != null) Modifier.width(colWidth) else Modifier.widthIn(min = 100.dp)

                    Text(
                        text = title.uppercase(),
                        style = IeTypography.tableHeader,
                        color = StitchSlate600,
                        textAlign = align,
                        modifier = cellModifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Body Rows
            rows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 1) StitchSlate50 else StitchWhite
                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { i, cell ->
                        val colWidth = columnWidths?.getOrNull(i)
                        val isNum = isNumericColumn?.getOrNull(i) == true
                        val align = if (isNum) TextAlign.End else TextAlign.Start
                        val textStyle = if (isNum) IeTypography.dataMono else MaterialTheme.typography.bodyMedium
                        val cellModifier = if (colWidth != null) Modifier.width(colWidth) else Modifier.widthIn(min = 100.dp)

                        Text(
                            text = cell,
                            style = textStyle,
                            color = StitchSlate800,
                            textAlign = align,
                            modifier = cellModifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                if (index < rows.size - 1) {
                    HorizontalDivider(color = StitchSlate200, thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Standard Stitch OEE Circular Gauge.
 */
@Composable
fun IeOeeGauge(
    label: String,
    percentage: Double,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = StitchCobalt600
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { percentage.toFloat() },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 10.dp,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = IeTypography.kpiLarge,
                color = StitchSlate900
            )
            Text(
                text = label.uppercase(),
                style = IeTypography.badgeText,
                color = StitchSlate500
            )
        }
    }
}

/**
 * Technical Linear Progress for Pareto/Loss Analysis.
 */
@Composable
fun IeLinearProgress(
    label: String,
    value: String,
    percentage: Float,
    modifier: Modifier = Modifier,
    color: Color = StitchCobalt600,
    secondaryLabel: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = StitchSlate800)
                if (secondaryLabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = secondaryLabel, style = IeTypography.badgeText, color = StitchSlate500)
                }
            }
            Text(text = value, style = IeTypography.dataMonoBold, color = StitchSlate900)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

/**
 * Section Header with subtitle.
 */
@Composable
fun IeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate500
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

/**
 * Stitch Dialog Wrapper.
 */
@Composable
fun IeDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String = "Confirm",
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancel",
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = IeRadius.dialogShape,
        containerColor = StitchWhite,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchSlate900
            )
        },
        text = { content() },
        confirmButton = {
            if (onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchSlate900,
                        contentColor = StitchWhite
                    ),
                    shape = IeRadius.buttonShape
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = IeRadius.buttonShape,
                border = BorderStroke(1.dp, StitchSlate300)
            ) {
                Text(dismissText, color = StitchSlate700)
            }
        }
    )
}

/**
 * States: Loading, Error, Empty.
 */
@Composable
fun IeLoadingState(message: String = "Processing...") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = StitchCobalt600,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = StitchSlate600)
        }
    }
}

@Composable
fun IeErrorState(message: String = "An error occurred", onRetry: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Error, contentDescription = "Error", tint = StitchNvaRed, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = StitchNvaRedText, textAlign = TextAlign.Center)
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun IeEmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(
                shape = CircleShape,
                color = StitchSlate100,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = StitchSlate500, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchSlate900)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchSlate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp)
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSlate900),
                    shape = IeRadius.buttonShape
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

// =============================================================================
// BACKWARD COMPATIBILITY ALIASES
// =============================================================================

@Composable
fun ChartContainer(title: String, content: @Composable () -> Unit) {
    IeChartContainer(title = title, chartContent = { content() })
}

@Composable
fun DataTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    IeTable(headers = headers, rows = rows, modifier = modifier)
}

@Composable
fun LoadingState(message: String = "Loading...") {
    IeLoadingState(message = message)
}

@Composable
fun ErrorState(message: String = "An error occurred", onRetry: (() -> Unit)? = null) {
    IeErrorState(message = message, onRetry = onRetry)
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    IeEmptyState(title = title, message = message, icon = icon, actionText = actionText, onAction = onAction, modifier = modifier)
}

@Composable
fun SectionHeader(title: String) {
    IeSectionHeader(title = title)
}

@Composable
fun AppDialog(
    title: String,
    content: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    IeDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        confirmText = confirmText,
        dismissText = dismissText,
        content = content
    )
}

@Composable
fun IeReportBranding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = StitchSlate100, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "IE COPILOT • PRODUCTION RELEASE V2.6",
            style = IeTypography.dataMonoBold,
            color = StitchSlate400,
            fontSize = 10.sp
        )
        Text(
            text = "Developed by Abhay Singh | Manufacturing Excellence",
            style = IeTypography.badgeText,
            color = StitchSlate500,
            fontSize = 11.sp
        )
        Text(
            text = "Report Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
            style = IeTypography.badgeText,
            color = StitchSlate300,
            fontSize = 9.sp
        )
    }
}

