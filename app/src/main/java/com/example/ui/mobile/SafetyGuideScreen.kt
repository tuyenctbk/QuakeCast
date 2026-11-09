package com.example.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SeismicRed

@Composable
fun SafetyGuideScreen(
    isSupporter: Boolean = false,
    onOpenDonation: (() -> Unit)? = null,
    onMilestoneAchieved: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val kitItems = remember {
        mutableStateMapOf(
            "Drinking Water (1 gallon/person/day for 3 days)" to true,
            "Non-perishable canned food & manual opener" to true,
            "Battery-powered or hand-crank emergency radio" to false,
            "Flashlight & extra high-capacity batteries" to true,
            "First Aid Kit & essential prescription medications" to true,
            "Whistle to signal search & rescue teams" to false,
            "Dust mask to filter contaminated airborne particulate" to false,
            "Multi-tool / wrench to turn off gas and main water valve" to false
        )
    }

    val completedCount = kitItems.values.count { it }
    val totalCount = kitItems.size
    val progress = completedCount.toFloat() / totalCount
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "kit_progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.safety_guide_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Protocol Card: DROP, COVER, HOLD ON
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SeismicRed.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, SeismicRed.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.safety_subtitle),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = SeismicRed,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Surface(
                        color = SeismicRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIFE CRITICAL",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SafetyStep(step = stringResource(R.string.safety_drop_title), desc = stringResource(R.string.safety_drop_desc))
                Spacer(modifier = Modifier.height(10.dp))
                SafetyStep(step = stringResource(R.string.safety_indoor_title), desc = stringResource(R.string.safety_indoor_desc))
                Spacer(modifier = Modifier.height(10.dp))
                SafetyStep(step = stringResource(R.string.safety_hazard_title), desc = stringResource(R.string.safety_hazard_desc))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tsunami Evacuation Protocol
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coastal Tsunami Protocol",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If you feel strong coastal shaking lasting 20+ seconds, do NOT wait for sirens or official bulletins. Immediately move inland or to high ground (at least 100 feet / 30 meters above sea level).",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Survival Kit Checklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Survival Go-Bag Checklist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = "$completedCount of $totalCount (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (progress >= 0.75f) SafeGreen else PulseCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (progress >= 0.75f) SafeGreen else PulseCyan,
                    trackColor = Color(0xFF1E2D4A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                kitItems.forEach { (item, isChecked) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                kitItems[item] = it
                                if (it) onMilestoneAchieved?.invoke()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SafeGreen,
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isChecked) Color.White else Color.Gray,
                                fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Emergency Contacts
        Text(
            text = "Emergency Resources",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permanent Static Support & About Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2D4A)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☕", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Support Independent Development",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    if (isSupporter) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Supporter 💛",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "QuakeCast is 100% ad-free, open science, and built without data tracking. If you enjoy the real-time 3D globe and proximity warnings, consider supporting server and development costs.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onOpenDonation?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSupporter) "Manage Support / Tip Jar" else "Open Tip Jar (Google Play / PayPal / Ko-fi)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SafetyStep(step: String, desc: String) {
    Column {
        Text(step, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color.White))
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray))
    }
}
