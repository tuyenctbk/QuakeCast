package com.example.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.Earthquake
import com.example.model.UserLocation
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.util.DistanceCalculator
import com.example.util.WaveArrivalEstimator

@Composable
fun ProximityAlertBanner(
    closestQuake: Earthquake,
    distanceKm: Double,
    userLocation: UserLocation,
    onViewQuakeDetails: (Earthquake) -> Unit,
    onVocalizeAlert: ((Earthquake, Double) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    val bearing = DistanceCalculator.calculateBearingDirection(
        userLocation.latitude, userLocation.longitude,
        closestQuake.latitude, closestQuake.longitude
    )

    val waveInfo = WaveArrivalEstimator.estimateWaveArrival(distanceKm, closestQuake.depth)
    val estimatedIntensity = WaveArrivalEstimator.estimateIntensityMmi(closestQuake.magnitude, distanceKm)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("proximity_alert_banner"),
        color = SeismicRed.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SeismicRed.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CrisisAlert,
                        contentDescription = stringResource(R.string.proximity_banner_title),
                        tint = SeismicRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.proximity_banner_title),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = SeismicRed,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.proximity_alert_distance_format, distanceKm, userLocation.name),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.proximity_settings_cd), tint = PulseCyan)
                        }
                    }

                    if (onVocalizeAlert != null) {
                        IconButton(
                            onClick = { onVocalizeAlert(closestQuake, distanceKm) },
                            modifier = Modifier.testTag("tts_vocalize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.btn_speak),
                                tint = PulseCyan
                            )
                        }
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = SeismicRed.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "M ${closestQuake.magnitude} — ${closestQuake.place}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estimated Shaking:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                            Text(estimatedIntensity, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PulseCyan))
                        }
                        Column {
                            Text(stringResource(R.string.primary_p_wave), style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                            Text("${String.format("%.1f", waveInfo.pWaveDelaySec)} sec", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        }
                        Column {
                            Text(stringResource(R.string.secondary_s_wave), style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                            Text("${String.format("%.1f", waveInfo.sWaveDelaySec)} sec", style = MaterialTheme.typography.bodyMedium.copy(color = SeismicRed))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onViewQuakeDetails(closestQuake) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SeismicRed)
                        ) {
                            Text(stringResource(R.string.tap_to_view_details), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }

                        if (onVocalizeAlert != null) {
                            OutlinedButton(
                                onClick = { onVocalizeAlert(closestQuake, distanceKm) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_speak))
                            }
                        }
                    }
                }
            }
        }
    }
}
