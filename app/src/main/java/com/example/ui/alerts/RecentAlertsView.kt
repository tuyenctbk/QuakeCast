package com.example.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Earthquake
import com.example.model.UserLocation
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SeismicRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentAlertsView(
    proximityAlerts: List<Pair<Earthquake, Double>>,
    userLocation: UserLocation,
    selectedQuake: Earthquake?,
    onSelectQuake: (Earthquake) -> Unit,
    onVocalizeAlert: (Earthquake, Double) -> Unit,
    onOpenProximitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Top Header Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PulseCyan.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Proximity Tremor History",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Radius: ${userLocation.radiusKm.toInt()} km from ${userLocation.name}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenProximitySettings,
                        modifier = Modifier.testTag("alerts_settings_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Adjust Alert Radius", tint = PulseCyan)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AlertStatChip(
                        label = "Total In Range",
                        value = "${proximityAlerts.size}",
                        accentColor = PulseCyan
                    )
                    AlertStatChip(
                        label = "Closest Tremor",
                        value = if (proximityAlerts.isNotEmpty()) "${proximityAlerts.first().second.toInt()} km" else "None",
                        accentColor = SafeGreen
                    )
                    AlertStatChip(
                        label = "Max Mag",
                        value = if (proximityAlerts.isNotEmpty()) "M ${proximityAlerts.maxOf { it.first.magnitude }}" else "N/A",
                        accentColor = SeismicRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (proximityAlerts.isEmpty()) {
            EmptyProximityAlertsView(onOpenProximitySettings = onOpenProximitySettings)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRIGGERED PROXIMITY ALERTS (${proximityAlerts.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "Sorted by Distance",
                    style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recent_alerts_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(proximityAlerts, key = { it.first.id }) { (quake, distanceKm) ->
                    val isSelected = selectedQuake?.id == quake.id

                    ProximityAlertHistoryCard(
                        quake = quake,
                        distanceKm = distanceKm,
                        isSelected = isSelected,
                        onSelect = { onSelectQuake(quake) },
                        onVocalize = { onVocalizeAlert(quake, distanceKm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertStatChip(
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun ProximityAlertHistoryCard(
    quake: Earthquake,
    distanceKm: Double,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onVocalize: () -> Unit
) {
    val magColor = when {
        quake.magnitude >= 5.0 -> SeismicRed
        quake.magnitude >= 3.0 -> Color(0xFFFF9800)
        else -> PulseCyan
    }

    val formattedTime = remember(quake.time) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(quake.time))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_alert_item_${quake.id}")
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PulseCyan.copy(alpha = 0.15f) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan)
        else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Magnitude Circle Badge
                Surface(
                    color = magColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, magColor)
                ) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", quake.magnitude),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = magColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quake.place,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        if (quake.hasTsunamiAlert) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SeismicRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Tsunami Alert",
                                        tint = SeismicRed,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "TSUNAMI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = SeismicRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PulseCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${distanceKm.toInt()} km away",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PulseCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Depth: ${quake.depth.toInt()} km • Coordinates: ${String.format(Locale.US, "%.2f", quake.latitude)}, ${String.format(Locale.US, "%.2f", quake.longitude)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontSize = 10.sp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = { com.example.util.ShareUtils.shareEarthquake(context, quake, distanceKm) },
                    modifier = Modifier.testTag("share_alert_${quake.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Alert Telemetry",
                        tint = PulseCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onVocalize,
                    modifier = Modifier.testTag("speak_alert_${quake.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Vocalize Alert",
                        tint = PulseCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Inspect",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyProximityAlertsView(onOpenProximitySettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = SafeGreen.copy(alpha = 0.15f),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SafeGreen,
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Nearby Tremors Detected",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "No earthquakes matching your feed criteria have occurred within your configured alert radius.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onOpenProximitySettings,
            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adjust Radius / Location", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
