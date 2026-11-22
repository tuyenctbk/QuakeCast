package com.example.ui.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Earthquake
import com.example.ui.globe.Globe3DView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.util.TimeUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TvScreenSaverScreen(
    significantQuakes: List<Earthquake>,
    currentIndex: Int,
    selectedQuake: Earthquake?,
    isPaused: Boolean = false,
    isLoading: Boolean = false,
    onTogglePause: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSelectQuake: (Earthquake) -> Unit,
    onExitTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val tickerListState = rememberLazyListState()

    val currentQuake = selectedQuake ?: significantQuakes.getOrNull(currentIndex)

    // Pulsing live indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "tv_live_pulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )
    val livePulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_scale"
    )

    // Auto-scroll bottom ticker so the active earthquake stays visible
    LaunchedEffect(currentIndex, significantQuakes.size) {
        if (currentIndex in significantQuakes.indices) {
            tickerListState.animateScrollToItem(currentIndex)
        }
    }

    // Live Clock timer
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val utcTimeStr = remember(currentTimeMs) {
        val sdf = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.format(Date(currentTimeMs))
    }

    val localTimeStr = remember(currentTimeMs) {
        val sdf = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        sdf.format(Date(currentTimeMs))
    }

    // Auto-cycle timer progress indicator (8s cycle) with smooth animation
    var cycleTimerTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentIndex, isPaused) {
        cycleTimerTick = 0
        if (!isPaused) {
            for (i in 1..80) {
                delay(100)
                cycleTimerTick = i
            }
        }
    }
    val smoothProgress by animateFloatAsState(
        targetValue = cycleTimerTick / 80f,
        animationSpec = tween(100, easing = LinearEasing),
        label = "cycle_progress"
    )

    // Request TV focus automatically
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onPrevious()
                            true
                        }
                        Key.DirectionRight -> {
                            onNext()
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            if (currentQuake != null) {
                                showDetailDialog = true
                            } else {
                                onRefresh()
                            }
                            true
                        }
                        Key.Spacebar, Key.MediaPlayPause -> {
                            onTogglePause()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            if (showDetailDialog) {
                                showDetailDialog = false
                            } else {
                                onExitTvMode()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Background 3D Rotating Globe
        Globe3DView(
            earthquakes = significantQuakes,
            selectedQuake = currentQuake,
            onSelectQuake = onSelectQuake,
            isAutoRotate = !isPaused,
            isTvMode = true
        )

        // Top Header Bar for TV
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title + Animated Live Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(if (isPaused) 1f else livePulseScale)
                        .alpha(if (isPaused) 0.5f else livePulseAlpha)
                        .clip(CircleShape)
                        .background(SeismicRed)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "QUAKECAST TV",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = PulseCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "LIVE SEISMIC SCREEN SAVER",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PulseCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (isPaused) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = Color(0xFFFF9100).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "PAUSED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFF9100),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Right Clock, Pause Toggle & Exit Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = utcTimeStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Local: $localTimeStr",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume Auto-Cycle" else "Pause Auto-Cycle",
                        tint = PulseCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onExitTvMode,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Earthquake Telemetry Card Overlay (Bottom Left) with Smooth AnimatedContent
        if (currentQuake != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 100.dp)
                    .width(460.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    AnimatedContent(
                        targetState = currentQuake,
                        transitionSpec = {
                            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 })
                                .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 6 })
                        },
                        label = "tv_quake_card"
                    ) { quake ->
                        Column {
                            // Magnitude & Tsunami badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = when {
                                        quake.magnitude >= 6.0 -> SeismicRed
                                        quake.magnitude >= 4.5 -> Color(0xFFFF9100)
                                        else -> PulseCyan
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "M ${String.format(Locale.US, "%.1f", quake.magnitude)}",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                }

                                if (quake.hasTsunamiAlert) {
                                    Surface(
                                        color = SeismicRed.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = SeismicRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "TSUNAMI ALERT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SeismicRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = quake.place,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Depth: ${String.format(Locale.US, "%.1f", quake.depth)} km (${quake.depthCategory.label})",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                                )
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                                )
                                Text(
                                    text = TimeUtils.formatRelativeTime(quake.time),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = PulseCyan, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Smooth progress bar for auto-cycling
                    if (!isPaused) {
                        LinearProgressIndicator(
                            progress = { smoothProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = PulseCyan,
                            trackColor = Color(0xFF1E2D4A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // D-Pad Remote Instructions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous", tint = PulseCyan)
                            }
                            Text(
                                text = "${currentIndex + 1} of ${significantQuakes.size}",
                                style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next", tint = PulseCyan)
                            }
                        }

                        Text(
                            text = "◄ ► Switch · OK Details · ⏯ Pause",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
            }
        } else {
            // Loading or Offline Empty State Card for TV
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 100.dp)
                    .width(440.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PulseCyan,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = "Syncing USGS Live Feed…",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Fetching global seismic telemetry",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Seismic Data Available",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Check network connection and press OK to retry.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                            )
                        }
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Strip: Auto-Scrolling Horizontal Ticker of Significant Quakes
        LazyRow(
            state = tickerListState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(significantQuakes) { _, quake ->
                val isSelected = quake.id == currentQuake?.id
                Card(
                    modifier = Modifier
                        .clickable { onSelectQuake(quake) }
                        .width(185.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PulseCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PulseCyan) else null
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when {
                                quake.magnitude >= 6.0 -> SeismicRed
                                quake.magnitude >= 4.5 -> Color(0xFFFF9100)
                                else -> PulseCyan
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "M ${String.format(Locale.US, "%.1f", quake.magnitude)}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = quake.place,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", quake.depth)} km",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp)
                            )
                        }
                    }
                }
            }
        }

        // TV Detail Modal Dialog (When Enter / OK is pressed)
        if (showDetailDialog && currentQuake != null) {
            AlertDialog(
                onDismissRequest = { showDetailDialog = false },
                title = {
                    Text(
                        text = currentQuake.title,
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Magnitude: M ${String.format(Locale.US, "%.1f", currentQuake.magnitude)} (${currentQuake.magType.uppercase()})",
                            color = Color.White
                        )
                        Text(
                            "Depth: ${String.format(Locale.US, "%.1f", currentQuake.depth)} km (${currentQuake.depthCategory.label})",
                            color = Color.White
                        )
                        Text(
                            "Coordinates: ${String.format(Locale.US, "%.4f°, %.4f°", currentQuake.latitude, currentQuake.longitude)}",
                            color = Color.White
                        )
                        Text("Recorded Time: ${TimeUtils.formatDateTime(currentQuake.time)}", color = Color.White)
                        if (currentQuake.felt != null) {
                            Text("Felt Reports: ${currentQuake.felt} community observations", color = Color.White)
                        }
                        Text(
                            "Energy Released: ${String.format(Locale.US, "%.2e", currentQuake.energyJoules)} Joules (~${String.format(Locale.US, "%.1f", currentQuake.tntTons)} Tons TNT Equivalent)",
                            color = PulseCyan
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDetailDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
                    ) {
                        Text("Dismiss (Back)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
