package com.example.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DepthCategory
import com.example.model.Earthquake
import com.example.model.UserLocation
import com.example.ui.theme.DeepDepthColor
import com.example.ui.theme.MidDepthColor
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.ui.theme.ShallowDepthColor
import com.example.util.DistanceCalculator
import com.example.util.TimeUtils

@Composable
fun EarthquakeListItem(
    quake: Earthquake,
    userLocation: UserLocation?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceKm = userLocation?.let {
        DistanceCalculator.calculateDistanceKm(it.latitude, it.longitude, quake.latitude, quake.longitude)
    }

    val magColor = when {
        quake.magnitude >= 6.0 -> SeismicRed
        quake.magnitude >= 4.5 -> Color(0xFFFF9100)
        quake.magnitude >= 2.5 -> Color(0xFFFFD600)
        else -> PulseCyan
    }

    val depthColor = when (quake.depthCategory) {
        DepthCategory.SHALLOW -> ShallowDepthColor
        DepthCategory.MID -> MidDepthColor
        DepthCategory.DEEP -> DeepDepthColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("earthquake_item_${quake.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, magColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Magnitude Badge Box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(magColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f", quake.magnitude),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (quake.magnitude >= 2.5 && quake.magnitude < 4.5) Color.Black else Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = quake.place,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (quake.hasTsunamiAlert) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.filter_tsunami),
                            tint = SeismicRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Depth Pill
                    Surface(
                        color = depthColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${quake.depth} km (${quake.depthCategory.label})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = depthColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Time Ago
                    Text(
                        text = TimeUtils.formatRelativeTime(quake.time),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Distance from user
                    if (distanceKm != null) {
                        Text(
                            text = "·  " + androidx.compose.ui.res.stringResource(com.example.R.string.distance_format, distanceKm),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PulseCyan,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
