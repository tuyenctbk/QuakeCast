package com.example.ui.mobile

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.donation.DonationBottomSheet
import com.example.donation.DonationManager
import com.example.donation.DonationState
import com.example.donation.ValueRealizationCard
import com.example.model.Earthquake
import com.example.model.FeedTimeframe
import com.example.model.MagFilter
import com.example.ui.alerts.RecentAlertsView
import com.example.ui.chart.SeismicAnalyticsDashboard
import com.example.ui.chart.SeismicFrequencyChart
import com.example.ui.globe.Globe3DView
import com.example.ui.settings.SupportDevelopmentScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.util.ShakeDetector
import com.example.util.ShakeSensitivity
import com.example.util.TtsManager
import com.example.viewmodel.QuakeCastViewModel
import com.example.viewmodel.QuakeUiState
import com.example.engagement.ActivePromptType
import com.example.engagement.UserEngagementManager
import com.example.ui.dialogs.RateAppDialog
import com.example.ui.dialogs.ShareAppDialog
import com.example.ui.dialogs.UpdateAppDialog
import com.example.ui.theme.AppThemeMode

enum class MobileTab(val label: String) {
    GLOBE("3D Globe"),
    LIST("Event Feed"),
    ALERTS("Recent Alerts"),
    SAFETY("Safety Guide")
}

enum class TabletRightTab(val label: String) {
    GLOBE("3D Globe"),
    RECENT_ALERTS("Recent Alerts"),
    CHART_30_DAYS("30-Day Frequency Chart")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileMainScreen(
    viewModel: QuakeCastViewModel,
    uiState: QuakeUiState,
    appThemeMode: AppThemeMode,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onToggleTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var selectedTab by remember { mutableStateOf(MobileTab.GLOBE) }
    var tabletRightTab by remember { mutableStateOf(TabletRightTab.GLOBE) }
    var showDetailBottomSheet by remember { mutableStateOf(false) }
    var showProximitySettings by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showDonationSheet by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Shake Sensitivity state stored persistently in SharedPreferences
    var shakeSensitivity by remember {
        mutableStateOf(ShakeDetector.getSavedSensitivity(context))
    }

    // Initialize Room Database cache
    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
    }

    // Donation Engagement Engine
    val donationManager = remember { DonationManager(context) }
    val donationState by donationManager.donationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Engagement Traffic Controller (Update, Rate, Share Dialogs)
    val engagementManager = remember { UserEngagementManager(context) }
    val engagementState by engagementManager.state.collectAsState()

    // Auto-evaluate automated engagement prompts (Update > Rate > Share) after calm idle period
    LaunchedEffect(uiState.proximityAlerts.size, donationState.isSupporter) {
        val hasActiveEmergency = uiState.proximityAlerts.any { it.second < 150.0 }
        kotlinx.coroutines.delay(3500L)
        engagementManager.evaluateAutomatedPrompts(
            hasActiveEmergency = hasActiveEmergency,
            isSupporter = donationState.isSupporter
        )
    }

    // Text-to-Speech Accessibility Manager
    val ttsManager = remember { TtsManager(context) }

    // Physical Tremor / Shake Sensor Detector for Manual Check Refresh
    val shakeDetector = remember(shakeSensitivity) {
        ShakeDetector(context, initialSensitivity = shakeSensitivity) {
            viewModel.loadEarthquakes()
            donationManager.recordSuccessfulOperation()
            engagementManager.recordManualRefresh()

            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(250)
                    }
                }
            } catch (_: Exception) {}

            android.widget.Toast.makeText(
                context,
                context.getString(R.string.toast_shake_detected),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(shakeSensitivity) {
        shakeDetector.start()
        onDispose {
            shakeDetector.stop()
            ttsManager.shutdown()
        }
    }

    val closestAlert = uiState.proximityAlerts.firstOrNull()
    var lastVocalizedQuakeId by remember { mutableStateOf<String?>(null) }

    // Auto-vocalize & play alert tone notification when a new closest earthquake is detected
    LaunchedEffect(closestAlert?.first?.id, uiState.soundAlertsEnabled) {
        val alert = closestAlert
        if (alert != null && uiState.soundAlertsEnabled && alert.first.id != lastVocalizedQuakeId) {
            lastVocalizedQuakeId = alert.first.id
            ttsManager.speakAlert(alert.first, alert.second, uiState.userLocation.name)

            // Trigger system alert tone & heads-up lockscreen notification
            com.example.util.SeismicNotificationManager.triggerProximityAlertNotification(
                context = context,
                quake = alert.first,
                distanceKm = alert.second,
                userLocationName = uiState.userLocation.name
            )

            // Trigger haptic vibration for safety tremor alert
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 400), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Milestone Toast Notification trigger (10 or 25 lookups)
    LaunchedEffect(donationState.pendingMilestone) {
        val milestone = donationState.pendingMilestone
        if (milestone != null) {
            val result = snackbarHostState.showSnackbar(
                message = "🎉 Milestone Reached: ${milestone}th Seismic Feed Lookup! QuakeCast is 100% ad-free & independent.",
                actionLabel = "Support Dev",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                showSettingsScreen = true
            }
            donationManager.clearPendingMilestone()
        }
    }

    if (showSettingsScreen) {
        SupportDevelopmentScreen(
            donationState = donationState,
            donationManager = donationManager,
            engagementManager = engagementManager,
            shakeSensitivity = shakeSensitivity,
            onUpdateShakeSensitivity = { newSens ->
                shakeSensitivity = newSens
                ShakeDetector.saveSavedSensitivity(context, newSens)
            },
            appThemeMode = appThemeMode,
            onUpdateThemeMode = onUpdateThemeMode,
            onBack = { showSettingsScreen = false },
            modifier = modifier
        )
    } else {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            containerColor = DarkSurface,
                            contentColor = Color.White,
                            actionColor = PulseCyan,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )
            },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = PulseCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "QuakeCast",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (uiState.isOfflineMode) {
                            Surface(
                                color = Color(0xFFFF9100).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_room_cache),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFF9100),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            Surface(
                                color = SeismicRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_usgs_live),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SeismicRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Supporter Badge or Support Action Icon
                    if (donationState.isSupporter) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { showDonationSheet = true }
                                .padding(end = 4.dp)
                                .testTag("active_supporter_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💛", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.badge_supporter),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showDonationSheet = true },
                            modifier = Modifier.testTag("action_donation_tip")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.support_dev_tip_cd),
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    // Search toggle
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_content_description),
                            tint = Color.White
                        )
                    }

                    // Alert Radius / Location Settings Button
                    IconButton(
                        onClick = { showProximitySettings = true },
                        modifier = Modifier.testTag("open_proximity_settings")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.proximity_settings_cd), tint = PulseCyan)
                    }

                    // TTS Audio Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleSoundAlerts() },
                        modifier = Modifier.testTag("toggle_sound_alerts")
                    ) {
                        Icon(
                            imageVector = if (uiState.soundAlertsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = stringResource(R.string.toggle_audio_alerts_cd),
                            tint = if (uiState.soundAlertsEnabled) PulseCyan else Color.Gray
                        )
                    }

                    // Timeframe Menu Dropdown
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.timeframe_cd), tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        FeedTimeframe.values().forEach { tf ->
                            DropdownMenuItem(
                                text = { Text(tf.label) },
                                onClick = {
                                    viewModel.loadEarthquakes(tf)
                                    engagementManager.recordOperation()
                                    menuExpanded = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = {
                        viewModel.loadEarthquakes()
                        engagementManager.recordManualRefresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_feed_cd), tint = Color.White)
                    }

                    // Settings & Support Development Screen Button
                    IconButton(
                        onClick = { showSettingsScreen = true },
                        modifier = Modifier.testTag("open_settings_menu")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_cd), tint = Color.White)
                    }

                    // TV Mode Button
                    Button(
                        onClick = onToggleTvMode,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_tv_mode), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            if (!isTablet) {
                NavigationBar(containerColor = DarkSurface) {
                    NavigationBarItem(
                        selected = selectedTab == MobileTab.GLOBE,
                        onClick = { selectedTab = MobileTab.GLOBE },
                        icon = { Icon(Icons.Default.Public, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_globe)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MobileTab.LIST,
                        onClick = { selectedTab = MobileTab.LIST },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_list)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MobileTab.ALERTS,
                        onClick = { selectedTab = MobileTab.ALERTS },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_alerts)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MobileTab.SAFETY,
                        onClick = { selectedTab = MobileTab.SAFETY },
                        icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_safety)) }
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->

        // Proximity Settings Bottom Sheet Modal
        if (showProximitySettings) {
            ProximitySettingsDialog(
                userLocation = uiState.userLocation,
                allQuakes = uiState.quakes,
                soundAlertsEnabled = uiState.soundAlertsEnabled,
                onToggleSoundAlerts = { viewModel.toggleSoundAlerts() },
                onSaveLocation = { name, lat, lon, radius ->
                    viewModel.updateUserLocation(name, lat, lon, radius)
                    donationManager.recordSuccessfulOperation()
                    engagementManager.recordOperation()
                },
                onDismiss = { showProximitySettings = false }
            )
        }

        // Multi-Channel Donation Modal Sheet
        if (showDonationSheet) {
            DonationBottomSheet(
                donationState = donationState,
                onSupporterPurchased = { tier ->
                    donationManager.markAsSupporter(tier)
                },
                onDismiss = { showDonationSheet = false }
            )
        }

        if (isTablet) {
            // DUAL-PANE TABLET LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left Pane: Filters, Search & Real-time Event List (Width: 430dp)
                Column(
                    modifier = Modifier
                        .width(430.dp)
                        .fillMaxHeight()
                        .background(DarkSurface)
                ) {
                    // Search Bar if expanded
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PulseCyan) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search), tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Proximity Warning Banner if available
                    if (closestAlert != null) {
                        ProximityAlertBanner(
                            closestQuake = closestAlert.first,
                            distanceKm = closestAlert.second,
                            userLocation = uiState.userLocation,
                            onViewQuakeDetails = { quake ->
                                viewModel.selectQuake(quake)
                            },
                            onVocalizeAlert = { quake, dist ->
                                ttsManager.speakAlert(quake, dist, uiState.userLocation.name)
                            },
                            onOpenSettings = { showProximitySettings = true }
                        )
                    }

                    // Filter Chips Bar
                    FilterChipsRow(
                        selectedFilter = uiState.magFilter,
                        onSelectFilter = { viewModel.setMagFilter(it) },
                        tsunamiOnly = uiState.tsunamiOnly,
                        onToggleTsunami = { viewModel.setTsunamiOnly(it) }
                    )

                    // Subtle Value Realization Banner (shown after verified utility, never to new users, 14-day cooldown)
                    if (donationState.canShowValuePrompt) {
                        ValueRealizationCard(
                            onOpenDonationDialog = { showDonationSheet = true },
                            onDismiss = { donationManager.dismissPrompt() }
                        )
                    }

                    // Event Feed Count & Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.feed_count_format, uiState.filteredQuakes.size),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.feed_radius_format, uiState.userLocation.radiusKm.toInt()),
                                style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan)
                            )
                        }
                    }

                    // Quake List or Empty State
                    if (uiState.filteredQuakes.isEmpty()) {
                        EmptyQuakesView(onResetFilters = {
                            viewModel.setMagFilter(MagFilter.ALL)
                            viewModel.setTsunamiOnly(false)
                            viewModel.setSearchQuery("")
                        })
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.filteredQuakes, key = { it.id }) { quake ->
                                EarthquakeListItem(
                                    quake = quake,
                                    userLocation = uiState.userLocation,
                                    isSelected = uiState.selectedQuake?.id == quake.id,
                                    onClick = { viewModel.selectQuake(quake) }
                                )
                            }
                        }
                    }
                }

                VerticalDivider(color = Color.White.copy(alpha = 0.12f))

                // Right Pane: Interactive 3D Globe / 30-Day Frequency Chart & Detailed Inspector
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Segmented Tab Selector Bar
                    TabRow(
                        selectedTabIndex = tabletRightTab.ordinal,
                        containerColor = DarkBackground,
                        contentColor = PulseCyan
                    ) {
                        TabletRightTab.values().forEach { tab ->
                            val icon = when (tab) {
                                TabletRightTab.GLOBE -> Icons.Default.Public
                                TabletRightTab.RECENT_ALERTS -> Icons.Default.NotificationsActive
                                TabletRightTab.CHART_30_DAYS -> Icons.Default.BarChart
                            }
                            Tab(
                                selected = tabletRightTab == tab,
                                onClick = { tabletRightTab = tab },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tab.label, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (tabletRightTab) {
                            TabletRightTab.GLOBE -> {
                                Globe3DView(
                                    earthquakes = uiState.filteredQuakes,
                                    selectedQuake = uiState.selectedQuake,
                                    onSelectQuake = { viewModel.selectQuake(it) },
                                    isAutoRotate = uiState.isAutoRotate,
                                    isTvMode = false,
                                    onToggleAutoRotate = { viewModel.toggleAutoRotate() }
                                )
                            }
                            TabletRightTab.RECENT_ALERTS -> {
                                RecentAlertsView(
                                    proximityAlerts = uiState.proximityAlerts,
                                    userLocation = uiState.userLocation,
                                    selectedQuake = uiState.selectedQuake,
                                    onSelectQuake = { viewModel.selectQuake(it) },
                                    onVocalizeAlert = { quake, dist ->
                                        ttsManager.speakAlert(quake, dist, uiState.userLocation.name)
                                    },
                                    onOpenProximitySettings = { showProximitySettings = true }
                                )
                            }
                            TabletRightTab.CHART_30_DAYS -> {
                                SeismicAnalyticsDashboard(
                                    earthquakes = uiState.quakes,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Bottom Drawer Inspector for Selected Quake
                        if (uiState.selectedQuake != null) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(300.dp),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                EarthquakeDetailSheet(
                                    quake = uiState.selectedQuake,
                                    userLocation = uiState.userLocation
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // COMPACT MOBILE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Optional Search Bar for Mobile
                if (showSearchBar) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PulseCyan) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search), tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Proximity Banner if present
                if (closestAlert != null) {
                    ProximityAlertBanner(
                        closestQuake = closestAlert.first,
                        distanceKm = closestAlert.second,
                        userLocation = uiState.userLocation,
                        onViewQuakeDetails = { quake ->
                            viewModel.selectQuake(quake)
                            showDetailBottomSheet = true
                        },
                        onVocalizeAlert = { quake, dist ->
                            ttsManager.speakAlert(quake, dist, uiState.userLocation.name)
                        },
                        onOpenSettings = { showProximitySettings = true }
                    )
                }

                when (selectedTab) {
                    MobileTab.GLOBE -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Globe3DView(
                                earthquakes = uiState.filteredQuakes,
                                selectedQuake = uiState.selectedQuake,
                                onSelectQuake = { quake ->
                                    viewModel.selectQuake(quake)
                                    showDetailBottomSheet = true
                                },
                                isAutoRotate = uiState.isAutoRotate,
                                isTvMode = false,
                                onToggleAutoRotate = { viewModel.toggleAutoRotate() }
                            )

                            // Quick Selected Quake Pill Banner at bottom
                            if (uiState.selectedQuake != null) {
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = { showDetailBottomSheet = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "M ${uiState.selectedQuake.magnitude} — ${uiState.selectedQuake.place}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                            )
                                            Text(
                                                text = stringResource(R.string.tap_to_view_details),
                                                style = MaterialTheme.typography.bodySmall.copy(color = PulseCyan)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    MobileTab.LIST -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            FilterChipsRow(
                                selectedFilter = uiState.magFilter,
                                onSelectFilter = { viewModel.setMagFilter(it) },
                                tsunamiOnly = uiState.tsunamiOnly,
                                onToggleTsunami = { viewModel.setTsunamiOnly(it) }
                            )

                            // Subtle Value Realization Banner
                            if (donationState.canShowValuePrompt) {
                                ValueRealizationCard(
                                    onOpenDonationDialog = { showDonationSheet = true },
                                    onDismiss = { donationManager.dismissPrompt() }
                                )
                            }

                            if (uiState.filteredQuakes.isEmpty()) {
                                EmptyQuakesView(onResetFilters = {
                                    viewModel.setMagFilter(MagFilter.ALL)
                                    viewModel.setTsunamiOnly(false)
                                    viewModel.setSearchQuery("")
                                })
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(uiState.filteredQuakes, key = { it.id }) { quake ->
                                        EarthquakeListItem(
                                            quake = quake,
                                            userLocation = uiState.userLocation,
                                            isSelected = uiState.selectedQuake?.id == quake.id,
                                            onClick = {
                                                viewModel.selectQuake(quake)
                                                showDetailBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MobileTab.ALERTS -> {
                        RecentAlertsView(
                            proximityAlerts = uiState.proximityAlerts,
                            userLocation = uiState.userLocation,
                            selectedQuake = uiState.selectedQuake,
                            onSelectQuake = { quake ->
                                viewModel.selectQuake(quake)
                                showDetailBottomSheet = true
                            },
                            onVocalizeAlert = { quake, dist ->
                                ttsManager.speakAlert(quake, dist, uiState.userLocation.name)
                            },
                            onOpenProximitySettings = { showProximitySettings = true }
                        )
                    }
                    MobileTab.SAFETY -> {
                        SafetyGuideScreen(
                            isSupporter = donationState.isSupporter,
                            onOpenDonation = { showDonationSheet = true },
                            onMilestoneAchieved = {
                                donationManager.recordSuccessfulOperation()
                                engagementManager.recordOperation()
                            }
                        )
                    }
                }
            }

            // Bottom Sheet Modal for Mobile Quake Details
            if (showDetailBottomSheet && uiState.selectedQuake != null) {
                ModalBottomSheet(
                    onDismissRequest = { showDetailBottomSheet = false },
                    containerColor = DarkSurface
                ) {
                    EarthquakeDetailSheet(
                        quake = uiState.selectedQuake,
                        userLocation = uiState.userLocation,
                        modifier = Modifier.heightIn(max = 550.dp)
                    )
            }
        }
    }
}
}

    // Coordinated Engagement & Community Dialogs (Update, Rate, Share)
    when (engagementState.activePrompt) {
        ActivePromptType.UPDATE -> {
            UpdateAppDialog(
                onDismiss = { engagementManager.dismissPrompt(ActivePromptType.UPDATE) }
            )
        }
        ActivePromptType.RATE -> {
            RateAppDialog(
                onRated = { engagementManager.markRatingCompleted() },
                onFeedback = { engagementManager.dismissPrompt(ActivePromptType.RATE) },
                onDismiss = { engagementManager.dismissPrompt(ActivePromptType.RATE) }
            )
        }
        ActivePromptType.SHARE -> {
            ShareAppDialog(
                onShared = { engagementManager.markShareCompleted() },
                onDismiss = { engagementManager.dismissPrompt(ActivePromptType.SHARE) }
            )
        }
        ActivePromptType.NONE -> { /* No prompt active */ }
    }
}

@Composable
private fun EmptyQuakesView(onResetFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FilterAltOff,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_quakes_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_quakes_desc),
            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onResetFilters,
            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
        ) {
            Text(stringResource(R.string.btn_reset_filters), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: MagFilter,
    onSelectFilter: (MagFilter) -> Unit,
    tsunamiOnly: Boolean,
    onToggleTsunami: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MagFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SeismicRed,
                    selectedLabelColor = Color.White
                )
            )
        }

        FilterChip(
            selected = tsunamiOnly,
            onClick = { onToggleTsunami(!tsunamiOnly) },
            label = { Text(stringResource(R.string.filter_tsunami)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (tsunamiOnly) Color.White else SeismicRed
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SeismicRed,
                selectedLabelColor = Color.White
            )
        )
    }
}
