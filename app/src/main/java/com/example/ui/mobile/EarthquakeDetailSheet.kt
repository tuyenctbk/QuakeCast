package com.example.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Earthquake
import com.example.model.UserLocation
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.util.DistanceCalculator
import com.example.util.TimeUtils
import com.example.util.WaveArrivalEstimator

@Composable
fun EarthquakeDetailSheet(
    quake: Earthquake,
    userLocation: UserLocation,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val distanceKm = DistanceCalculator.calculateDistanceKm(
        userLocation.latitude, userLocation.longitude,
        quake.latitude, quake.longitude
    )

    val waveInfo = WaveArrivalEstimator.estimateWaveArrival(distanceKm, quake.depth)
    val estimatedMmi = WaveArrivalEstimator.estimateIntensityMmi(quake.magnitude, distanceKm)

    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "EVENT TELEMETRY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = quake.place,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Surface(
                color = when {
                    quake.magnitude >= 6.0 -> SeismicRed
                    quake.magnitude >= 4.5 -> Color(0xFFFF9100)
                    else -> PulseCyan
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "M ${String.format("%.1f", quake.magnitude)}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (quake.magnitude >= 4.5) Color.White else Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time and Tsunami Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeUtils.formatDateTime(quake.time),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )

            if (quake.hasTsunamiAlert) {
                Surface(
                    color = SeismicRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SeismicRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TSUNAMI WARN", style = MaterialTheme.typography.labelSmall.copy(color = SeismicRed, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(20.dp))

        // Wave Propagation Canvas Graphic
        Text(
            text = "Seismic Wave Propagation Simulator",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Text(
            text = "Distance to ${userLocation.name}: ${String.format("%.0f", distanceKm)} km",
            style = MaterialTheme.typography.bodySmall.copy(color = PulseCyan)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D1B2A)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val epicenter = Offset(size.width * 0.25f, size.height * 0.5f)
                val observer = Offset(size.width * 0.75f, size.height * 0.5f)

                // Epicenter dot
                drawCircle(color = SeismicRed, radius = 6.dp.toPx(), center = epicenter)

                // Observer dot
                drawCircle(color = PulseCyan, radius = 6.dp.toPx(), center = observer)

                // Line connecting epicenter to observer
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = epicenter,
                    end = observer,
                    strokeWidth = 2.dp.toPx()
                )

                // P-Wave Shockwave Ring
                val pMaxR = (observer.x - epicenter.x) * 1.1f
                val pCurrR = pMaxR * waveProgress
                drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = (1f - waveProgress).coerceIn(0f, 1f)),
                    radius = pCurrR,
                    center = epicenter,
                    style = Stroke(width = 2.dp.toPx())
                )

                // S-Wave Shockwave Ring
                val sMaxR = pMaxR * 0.6f
                val sCurrR = sMaxR * waveProgress
                drawCircle(
                    color = SeismicRed.copy(alpha = (1f - waveProgress).coerceIn(0f, 1f)),
                    radius = sCurrR,
                    center = epicenter,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Epicenter", style = MaterialTheme.typography.labelSmall.copy(color = SeismicRed, fontWeight = FontWeight.Bold))
                Text("User Location", style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan, fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grid of Key Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Focal Depth",
                value = "${String.format("%.1f", quake.depth)} km",
                subtitle = quake.depthCategory.label,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "P-Wave Delay",
                value = "${String.format("%.1f", waveInfo.pWaveDelaySec)}s",
                subtitle = "~6.1 km/s",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "S-Wave Delay",
                value = "${String.format("%.1f", waveInfo.sWaveDelaySec)}s",
                subtitle = "~3.5 km/s",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Estimated MMI",
                value = estimatedMmi,
                subtitle = "Intensity Level",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Energy Output",
                value = "${String.format("%.1f", quake.tntTons)} Tons",
                subtitle = "TNT Equivalent",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // External Link & Share Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { com.example.util.ShareUtils.shareEarthquake(context, quake, distanceKm) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("share_earthquake_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.btn_share_quake), color = Color.Black, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(quake.url))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_usgs_website_button"),
                border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.btn_view_usgs), color = PulseCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2D4A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = PulseCyan))
        }
    }
}
