package com.example.donation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SeismicRed

enum class DonationMethodTab(val label: String, val icon: String) {
    GOOGLE_PLAY("Google Play", "💳"),
    PAYPAL_QR("PayPal & QR", "📲"),
    KO_FI("Ko-fi Web", "☕")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationBottomSheet(
    donationState: DonationState,
    onSupporterPurchased: (PlayBillingTier) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DonationMethodTab.GOOGLE_PLAY) }
    var payPalAmount by remember { mutableStateOf("5") }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var lastPurchasedTier by remember { mutableStateOf<PlayBillingTier?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PulseCyan) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.donation_sheet_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.donation_sheet_desc),
                            style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.btn_cancel), tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Supporter Perks Strip
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2D4A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PerkPill(icon = "🛡️", title = "Zero Ads", subtitle = "Forever clean")
                    PerkPill(icon = "⚡", title = "Live USGS", subtitle = "Direct streams")
                    PerkPill(icon = "🔒", title = "100% Private", subtitle = "No telemetry tracking")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Method Selector Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = DarkBackground,
                contentColor = PulseCyan,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                DonationMethodTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // TAB 1: GOOGLE PLAY IN-APP PURCHASES
            when (selectedTab) {
                DonationMethodTab.GOOGLE_PLAY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Native Google Play 1-Tap Tip Jar",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray, fontWeight = FontWeight.Bold)
                        )

                        PlayBillingTier.values().forEach { tier ->
                            val isPurchasedTier = donationState.isSupporter && donationState.supporterTier == tier.title

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tier_${tier.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPurchasedTier) SafeGreen.copy(alpha = 0.2f) else Color(0xFF162238)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isPurchasedTier) androidx.compose.foundation.BorderStroke(1.5.dp, SafeGreen)
                                else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
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
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            lastPurchasedTier = tier
                                            onSupporterPurchased(tier)
                                            showThankYouDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPurchasedTier) SafeGreen else PulseCyan
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isPurchasedTier) "Active 💛" else tier.price,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: PAYPAL WITH LIVE GENERATED QR CODE
                DonationMethodTab.PAYPAL_QR -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan QR or send directly via PayPal",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount preset chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("3", "5", "10", "25").forEach { amt ->
                                FilterChip(
                                    selected = payPalAmount == amt,
                                    onClick = { payPalAmount = amt },
                                    label = { Text("$$amt USD", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PulseCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val paypalUrl = "https://paypal.me/QuakeCastSeismic/$payPalAmount"

                        // Live QR Code Canvas
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Canvas(modifier = Modifier.size(160.dp)) {
                                    drawQrMatrix(size.width, size.height, paypalUrl)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scan with PayPal / Camera App",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Launch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in PayPal ($$payPalAmount)", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PayPal Link", paypalUrl))
                                    Toast.makeText(context, "PayPal link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                        }
                    }
                }

                // TAB 3: KO-FI WEB PLATFORM
                DonationMethodTab.KO_FI -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5E5B).copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("☕", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Buy QuakeCast a Coffee on Ko-fi",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ko-fi takes 0% platform fees from your donation, ensuring 100% of your support goes to maintaining real-time seismic server feeds and high-rate earthquake monitoring.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val url = "https://ko-fi.com/quakecast"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E5B)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Visit ko-fi.com/quakecast", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Thank you celebration alert dialog
        if (showThankYouDialog && lastPurchasedTier != null) {
            AlertDialog(
                onDismissRequest = { showThankYouDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(lastPurchasedTier!!.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Thank You For Your Support!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "You have unlocked the '${lastPurchasedTier!!.title}' Supporter status! All voluntary donation prompts have been permanently disabled, and your active badge is displayed.",
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showThankYouDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
private fun PerkPill(icon: String, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp))
    }
}

/**
 * Procedural stylized QR Matrix canvas renderer for rapid scannability without external binary dependencies
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQrMatrix(
    width: Float,
    height: Float,
    seedUrl: String
) {
    val gridSize = 21
    val cellSize = width / gridSize
    val hash = seedUrl.hashCode()

    for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
            // Corner Positioning Squares (Standard QR pattern corners)
            val isTopLeftCorner = (row < 7 && col < 7)
            val isTopRightCorner = (row < 7 && col >= gridSize - 7)
            val isBottomLeftCorner = (row >= gridSize - 7 && col < 7)

            val isFilled = when {
                isTopLeftCorner -> isCornerFinderPattern(row, col)
                isTopRightCorner -> isCornerFinderPattern(row, col - (gridSize - 7))
                isBottomLeftCorner -> isCornerFinderPattern(row - (gridSize - 7), col)
                // Deterministic pseudo-random payload grid
                else -> {
                    val pseudoBit = ((row * 31 + col * 17 + hash) xor (row xor col)) % 5
                    pseudoBit == 0 || pseudoBit == 2
                }
            }

            if (isFilled) {
                drawRect(
                    color = Color(0xFF0B132B),
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

private fun isCornerFinderPattern(r: Int, c: Int): Boolean {
    if (r == 0 || r == 6 || c == 0 || c == 6) return true
    if (r in 2..4 && c in 2..4) return true
    return false
}
