package com.example.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
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
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSelectQuake: (Earthquake) -> Unit,
    onExitTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val currentQuake = selectedQuake ?: significantQuakes.getOrNull(currentIndex)

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

    // Auto-cycle timer progress indicator (8s cycle)
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
                            showDetailDialog = true
                            true
                        }
                        Key.Spacebar, Key.MediaPlayPause -> {
                            isPaused = !isPaused
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
            // Left Title + Live Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
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
                        shape = RoundedCornerShape(6.dp)
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

            // Right Clock & Exit Action
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
                Spacer(modifier = Modifier.width(20.dp))
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

        // Main Earthquake Telemetry Card Overlay (Bottom Left)
        if (currentQuake != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 100.dp)
                    .width(460.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Magnitude & Tsunami badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = when {
                                currentQuake.magnitude >= 6.0 -> SeismicRed
                                currentQuake.magnitude >= 4.5 -> Color(0xFFFF9100)
                                else -> PulseCyan
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "M ${String.format("%.1f", currentQuake.magnitude)}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }

                        if (currentQuake.hasTsunamiAlert) {
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
                        text = currentQuake.place,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Depth: ${String.format("%.1f", currentQuake.depth)} km (${currentQuake.depthCategory.label})",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Text(
                            text = TimeUtils.formatRelativeTime(currentQuake.time),
                            style = MaterialTheme.typography.bodyMedium.copy(color = PulseCyan, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar for auto-cycling
                    if (!isPaused) {
                        LinearProgressIndicator(
                            progress = { cycleTimerTick / 80f },
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
                                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous", tint = PulseCyan)
                            }
                            Text(
                                text = "${currentIndex + 1} of ${significantQuakes.size}",
                                style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.NavigateNext, contentDescription = "Next", tint = PulseCyan)
                            }
                        }

                        Text(
                            text = "◄ ► Remote to Switch · OK Details",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
            }
        }

        // Bottom Strip: Horizontal Ticker of all Top 10 Significant Quakes
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(significantQuakes) { index, quake ->
                val isSelected = quake.id == currentQuake?.id
                Card(
                    modifier = Modifier
                        .clickable { onSelectQuake(quake) }
                        .width(180.dp),
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
                                text = "M ${String.format("%.1f", quake.magnitude)}",
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
                                maxLines = 1
                            )
                            Text(
                                text = "${String.format("%.1f", quake.depth)} km",
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
                        Text("Magnitude: M ${currentQuake.magnitude} (${currentQuake.magType.uppercase()})", color = Color.White)
                        Text("Depth: ${String.format("%.1f", currentQuake.depth)} km (${currentQuake.depthCategory.label})", color = Color.White)
                        Text("Coordinates: ${currentQuake.latitude}°, ${currentQuake.longitude}°", color = Color.White)
                        Text("Recorded Time: ${TimeUtils.formatDateTime(currentQuake.time)}", color = Color.White)
                        if (currentQuake.felt != null) {
                            Text("Felt Reports: ${currentQuake.felt} community observations", color = Color.White)
                        }
                        Text(
                            "Energy Released: ${String.format("%.2e", currentQuake.energyJoules)} Joules (~${String.format("%.1f", currentQuake.tntTons)} Tons TNT Equivalent)",
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
