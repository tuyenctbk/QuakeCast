package com.example.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DepthCategory
import com.example.model.Earthquake
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SeismicRed
import java.util.Locale

data class MagBucketStat(
    val rangeLabel: String,
    val minMag: Double,
    val maxMag: Double,
    val count: Int,
    val color: Color
)

data class DepthBucketStat(
    val category: DepthCategory,
    val count: Int,
    val avgMag: Double,
    val color: Color
)

@Composable
fun SeismicAnalyticsDashboard(
    earthquakes: List<Earthquake>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(earthquakes) {
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    // Calculate 30-day analytics summaries
    val totalCount = remember(earthquakes) { earthquakes.size.coerceAtLeast(1) }
    val maxMag = remember(earthquakes) { earthquakes.maxOfOrNull { it.magnitude } ?: 0.0 }
    val avgDepth = remember(earthquakes) { if (earthquakes.isNotEmpty()) earthquakes.map { it.depth }.average() else 0.0 }
    val tsunamiCount = remember(earthquakes) { earthquakes.count { it.hasTsunamiAlert } }

    // 1. Magnitude Distribution Buckets
    val magBuckets = remember(earthquakes) {
        listOf(
            MagBucketStat("M1.0–2.9", 1.0, 2.9, earthquakes.count { it.magnitude in 1.0..2.9 }, PulseCyan),
            MagBucketStat("M3.0–3.9", 3.0, 3.9, earthquakes.count { it.magnitude in 3.0..3.9 }, Color(0xFF4CAF50)),
            MagBucketStat("M4.0–4.9", 4.0, 4.9, earthquakes.count { it.magnitude in 4.0..4.9 }, Color(0xFFFFC107)),
            MagBucketStat("M5.0–5.9", 5.0, 5.9, earthquakes.count { it.magnitude in 5.0..5.9 }, Color(0xFFFF9800)),
            MagBucketStat("M6.0–6.9", 6.0, 6.9, earthquakes.count { it.magnitude in 6.0..6.9 }, SeismicRed),
            MagBucketStat("M7.0+", 7.0, 10.0, earthquakes.count { it.magnitude >= 7.0 }, Color(0xFFD500F9))
        )
    }

    // 2. Depth Analytics Breakdown
    val depthStats = remember(earthquakes) {
        val shallow = earthquakes.filter { it.depthCategory == DepthCategory.SHALLOW }
        val mid = earthquakes.filter { it.depthCategory == DepthCategory.MID }
        val deep = earthquakes.filter { it.depthCategory == DepthCategory.DEEP }

        listOf(
            DepthBucketStat(
                category = DepthCategory.SHALLOW,
                count = shallow.size,
                avgMag = if (shallow.isNotEmpty()) shallow.map { it.magnitude }.average() else 0.0,
                color = SeismicRed
            ),
            DepthBucketStat(
                category = DepthCategory.MID,
                count = mid.size,
                avgMag = if (mid.isNotEmpty()) mid.map { it.magnitude }.average() else 0.0,
                color = Color(0xFFFF9800)
            ),
            DepthBucketStat(
                category = DepthCategory.DEEP,
                count = deep.size,
                avgMag = if (deep.isNotEmpty()) deep.map { it.magnitude }.average() else 0.0,
                color = PulseCyan
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("seismic_analytics_dashboard")
    ) {
        // Dashboard Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "30-DAY SEISMIC ANALYTICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = "Magnitude Distribution & Hypocenter Analytics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Surface(
                color = PulseCyan.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("30 Days", style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan, fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Analytics KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                label = "Total Events",
                value = "$totalCount",
                icon = Icons.Default.Public,
                accentColor = PulseCyan,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                label = "Peak Magnitude",
                value = "M ${String.format(Locale.US, "%.1f", maxMag)}",
                icon = Icons.Default.ShowChart,
                accentColor = SeismicRed,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                label = "Avg Depth",
                value = "${avgDepth.toInt()} km",
                icon = Icons.Default.South,
                accentColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                label = "Tsunami Alerts",
                value = "$tsunamiCount",
                icon = Icons.Default.Warning,
                accentColor = if (tsunamiCount > 0) SeismicRed else SafeGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: MAGNITUDE DISTRIBUTION ANALYTICS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = PulseCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Magnitude Range Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                    Text(
                        text = "USGS Telemetry",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Magnitude Distribution Bars
                val maxBucketCount = remember(magBuckets) { (magBuckets.maxOfOrNull { it.count } ?: 10).coerceAtLeast(1) }

                magBuckets.forEach { bucket ->
                    val percentage = (bucket.count.toFloat() / totalCount * 100).toInt()
                    val barWidthRatio = (bucket.count.toFloat() / maxBucketCount) * animProgress.value

                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = bucket.color,
                                    shape = CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bucket.rangeLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }
                            Text(
                                text = "${bucket.count} events ($percentage%)",
                                style = MaterialTheme.typography.bodySmall.copy(color = bucket.color, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barWidthRatio.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(bucket.color.copy(alpha = 0.6f), bucket.color)
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: HYPOCENTER DEPTH FREQUENCY ANALYTICS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = PulseCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hypocenter Depth Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                    Text(
                        text = "Fault Structure",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    depthStats.forEach { stat ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16233B)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, stat.color.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stat.category.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = stat.category.depthRange,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "${stat.count}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = stat.color
                                    )
                                )
                                Text(
                                    text = "Avg M ${String.format(Locale.US, "%.1f", stat.avgMag)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 3: 30-DAY DAILY FREQUENCY TIMELINE
        SeismicFrequencyChart(
            earthquakes = earthquakes,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KpiMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp),
                maxLines = 1
            )
        }
    }
}
