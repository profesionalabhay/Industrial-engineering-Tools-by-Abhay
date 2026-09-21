package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CycleTimeMetric
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape

@Composable
fun ProcessMetricsDashboard(
    cycleTimes: List<CycleTimeMetric>,
    overallEfficiency: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Machine Efficiency Summary (OEE Gauge style)
        EfficiencyGaugeCard(overallEfficiency)

        // Cycle Time Comparison Chart
        CycleTimeComparisonChart(cycleTimes)
    }
}

@Composable
fun EfficiencyGaugeCard(efficiency: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StitchSlate50),
        shape = IeRadius.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MACHINE EFFICIENCY",
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchSlate600
                )
                Text(
                    text = "Overall Equipment Effectiveness (OEE)",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchSlate500
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { efficiency.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (efficiency >= 0.85) StitchVaGreen else if (efficiency >= 0.65) StitchNnvaAmber else StitchNvaRed,
                    trackColor = StitchSlate200,
                    strokeCap = StrokeCap.Round
                )
            }
            
            Box(
                modifier = Modifier.padding(start = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(efficiency * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate900
                    )
                )
            }
        }
    }
}

@Composable
fun CycleTimeComparisonChart(metrics: List<CycleTimeMetric>) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    
    remember(metrics) {
        modelProducer.tryRunTransaction {
            columnSeries {
                series(metrics.map { it.planned })
                series(metrics.map { it.actual })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = IeRadius.cardShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchSlate200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CYCLE TIME COMPARISON (SECONDS)",
                style = MaterialTheme.typography.labelLarge,
                color = StitchSlate900
            )
            Text(
                text = "Planned vs. Actual per critical station",
                style = MaterialTheme.typography.bodySmall,
                color = StitchSlate500
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(color = StitchSlate400, thickness = 12.dp, shape = Shape.rounded(allPercent = 25)),
                            rememberLineComponent(color = StitchTaktLineIndigo, thickness = 12.dp, shape = Shape.rounded(allPercent = 25))
                        )
                    ),
                    startAxis = rememberStartAxis(
                        label = rememberTextComponent(color = StitchSlate600, textSize = 10.sp),
                        guideline = rememberLineComponent(color = StitchSlate100)
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = rememberTextComponent(color = StitchSlate600, textSize = 10.sp),
                        valueFormatter = { value, _, _ ->
                            metrics.getOrNull(value.toInt())?.stationName ?: ""
                        }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = StitchSlate400, label = "Planned")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = StitchTaktLineIndigo, label = "Actual")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = StitchSlate600)
    }
}
