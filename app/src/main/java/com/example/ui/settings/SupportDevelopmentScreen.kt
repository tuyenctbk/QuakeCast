package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.donation.DonationManager
import com.example.donation.DonationState
import com.example.donation.PlayBillingTier
import com.example.engagement.ActivePromptType
import com.example.engagement.UserEngagementManager
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SafeGreen
import com.example.util.ShakeSensitivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDevelopmentScreen(
    donationState: DonationState,
    donationManager: DonationManager,
    engagementManager: UserEngagementManager,
    shakeSensitivity: ShakeSensitivity,
    onUpdateShakeSensitivity: (ShakeSensitivity) -> Unit,
    appThemeMode: AppThemeMode,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val engagementState by engagementManager.state.collectAsState()

    var showThankYouDialog by remember { mutableStateOf(false) }
    var lastPurchasedTier by remember { mutableStateOf<PlayBillingTier?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Supporter Badge Header Card if active
            if (donationState.isSupporter) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💛", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.supporter_active_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = SafeGreen
                                )
                            )
                            Text(
                                text = stringResource(R.string.supporter_active_desc, donationState.supporterTier ?: ""),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION: MANUAL TREMOR / SHAKE REFRESH SENSITIVITY (SLIDER)
            Text(
                text = stringResource(R.string.settings_shake_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_shake_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Enable / Disable Header Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📳", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.shake_to_refresh_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = if (shakeSensitivity != ShakeSensitivity.OFF) stringResource(R.string.shake_active_status) else stringResource(R.string.disabled_status),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (shakeSensitivity != ShakeSensitivity.OFF) SafeGreen else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                        Switch(
                            checked = shakeSensitivity != ShakeSensitivity.OFF,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    val savedThresh = com.example.util.ShakeDetector.getSavedThreshold(context)
                                    val sens = when {
                                        savedThresh <= 2.0f -> ShakeSensitivity.HIGH
                                        savedThresh <= 3.0f -> ShakeSensitivity.MEDIUM
                                        else -> ShakeSensitivity.LOW
                                    }
                                    onUpdateShakeSensitivity(sens)
                                } else {
                                    onUpdateShakeSensitivity(ShakeSensitivity.OFF)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = PulseCyan,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF202A3C)
                            ),
                            modifier = Modifier.testTag("shake_sensor_enable_switch")
                        )
                    }

                    if (shakeSensitivity != ShakeSensitivity.OFF) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Value & Dynamic Qualitative Classification
                        val currentG = if (shakeSensitivity == ShakeSensitivity.OFF) 2.5f else shakeSensitivity.thresholdG
                        val sensitivityLevelText = when {
                            currentG <= 2.0f -> stringResource(R.string.shake_sens_high_desc)
                            currentG <= 3.0f -> stringResource(R.string.shake_sens_med_desc)
                            else -> stringResource(R.string.shake_sens_low_desc)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.shake_required_force),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                color = PulseCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f g", currentG),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.titleSmall.copy(color = PulseCyan, fontWeight = FontWeight.Black)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Slider
                        Slider(
                            value = currentG,
                            onValueChange = { newG ->
                                val steppedG = (Math.round(newG * 10f) / 10f).coerceIn(1.5f, 4.5f)
                                val nearestSens = when {
                                    steppedG <= 2.0f -> ShakeSensitivity.HIGH
                                    steppedG <= 3.0f -> ShakeSensitivity.MEDIUM
                                    else -> ShakeSensitivity.LOW
                                }
                                com.example.util.ShakeDetector.saveSavedThreshold(context, steppedG)
                                onUpdateShakeSensitivity(nearestSens)
                            },
                            valueRange = 1.5f..4.5f,
                            steps = 29, // 0.1g increments between 1.5g and 4.5g
                            colors = SliderDefaults.colors(
                                thumbColor = PulseCyan,
                                activeTrackColor = PulseCyan,
                                inactiveTrackColor = Color(0xFF1E2D4A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shake_sensitivity_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1.5g", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                            Text("2.5g", style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan))
                            Text("4.5g", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = sensitivityLevelText,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Jump Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ShakeSensitivity.HIGH to stringResource(R.string.shake_preset_gentle),
                                ShakeSensitivity.MEDIUM to stringResource(R.string.shake_preset_standard),
                                ShakeSensitivity.LOW to stringResource(R.string.shake_preset_firm)
                            ).forEach { (preset, label) ->
                                val isSelected = shakeSensitivity == preset
                                OutlinedButton(
                                    onClick = {
                                        com.example.util.ShakeDetector.saveSavedThreshold(context, preset.thresholdG)
                                        onUpdateShakeSensitivity(preset)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("preset_${preset.name.lowercase()}"),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) PulseCyan else Color.White.copy(alpha = 0.15f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) PulseCyan.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) PulseCyan else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: GLOBAL THEME TOGGLE
            Text(
                text = stringResource(R.string.settings_theme_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_theme_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎨", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_theme_mode_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = stringResource(R.string.settings_current_theme, appThemeMode.label),
                                    style = MaterialTheme.typography.bodySmall.copy(color = PulseCyan, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemeMode.values().forEach { mode ->
                        val isSelected = appThemeMode == mode
                        val (modeTitle, modeDesc) = when (mode) {
                            AppThemeMode.SYSTEM -> stringResource(R.string.theme_system_title) to stringResource(R.string.theme_system_desc)
                            AppThemeMode.DARK -> stringResource(R.string.theme_dark_title) to stringResource(R.string.theme_dark_desc)
                            AppThemeMode.LIGHT -> stringResource(R.string.theme_light_title) to stringResource(R.string.theme_light_desc)
                        }

                        Surface(
                            onClick = { onUpdateThemeMode(mode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("theme_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PulseCyan.copy(alpha = 0.15f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PulseCyan else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onUpdateThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PulseCyan, unselectedColor = Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = modeTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = modeDesc,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1: STATIC DONATION PRESETS (GOOGLE PLAY BILLING)
            Text(
                text = stringResource(R.string.settings_support_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_support_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Google Play Presets Grid
            PlayBillingTier.values().forEach { tier ->
                val isPurchasedTier = donationState.isSupporter && donationState.supporterTier == tier.title

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("settings_tier_${tier.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPurchasedTier) SafeGreen.copy(alpha = 0.2f) else DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isPurchasedTier) androidx.compose.foundation.BorderStroke(1.5.dp, SafeGreen)
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
                            Text(tier.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tier.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = tier.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                lastPurchasedTier = tier
                                donationManager.markAsSupporter(tier)
                                showThankYouDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPurchasedTier) SafeGreen else PulseCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPurchasedTier) stringResource(R.string.donation_btn_active) else tier.price,
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: PAYPAL & KO-FI DIRECT PLATFORM LINKS
            Text(
                text = stringResource(R.string.settings_tip_jars_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PayPal Direct
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📲", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PayPal.me", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.settings_paypal_desc), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val url = "https://paypal.me/QuakeCastSeismic/5"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.settings_paypal_button), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Ko-fi Direct
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5E5B).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☕", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ko-fi Page", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.settings_kofi_desc), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val url = "https://ko-fi.com/quakecast"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E5B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.settings_kofi_button), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 3: COMMUNITY & ENGAGEMENT SYSTEM
            Text(
                text = stringResource(R.string.settings_engagement_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_engagement_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_telemetry_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_install_duration), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(stringResource(R.string.telemetry_days_format, engagementState.installDurationDays), style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_manual_refreshes), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(
                            text = stringResource(R.string.telemetry_refreshes_needed_format, engagementState.manualRefreshCount),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (engagementState.manualRefreshCount >= 5) SafeGreen else PulseCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_operations), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(stringResource(R.string.telemetry_actions_format, engagementState.successfulOperations), style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_prompt_cooldown), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(stringResource(R.string.telemetry_cooldown_val), style = MaterialTheme.typography.bodySmall.copy(color = PulseCyan, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_play_rating), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(
                            text = if (engagementState.hasRated) stringResource(R.string.telemetry_rated) else stringResource(R.string.telemetry_not_rated),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (engagementState.hasRated) SafeGreen else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.telemetry_shared), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                        Text(
                            text = if (engagementState.hasShared) stringResource(R.string.telemetry_already_shared) else stringResource(R.string.telemetry_not_shared),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (engagementState.hasShared) SafeGreen else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.settings_preview_title),
                        style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { engagementManager.triggerPrompt(ActivePromptType.UPDATE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_update_dialog_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan.copy(alpha = 0.4f))
                        ) {
                            Text(stringResource(R.string.btn_preview_update), color = PulseCyan, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { engagementManager.triggerPrompt(ActivePromptType.RATE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_rate_dialog_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                        ) {
                            Text(stringResource(R.string.btn_preview_rate), color = Color(0xFFFFD700), fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { engagementManager.triggerPrompt(ActivePromptType.SHARE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_share_dialog_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f))
                        ) {
                            Text(stringResource(R.string.btn_preview_share), color = SafeGreen, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // APP METADATA & PRIVACY COMMITMENT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.app_version_name), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Thank you celebration alert dialog
        if (showThankYouDialog && lastPurchasedTier != null) {
            AlertDialog(
                onDismissRequest = { showThankYouDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(lastPurchasedTier!!.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.thank_you_title), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = stringResource(R.string.thank_you_desc, lastPurchasedTier!!.title),
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showThankYouDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Text(stringResource(R.string.btn_awesome), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
