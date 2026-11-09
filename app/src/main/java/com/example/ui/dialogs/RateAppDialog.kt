package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan

@Composable
fun RateAppDialog(
    onRated: () -> Unit,
    onFeedback: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConstructiveFeedbackStep by remember { mutableStateOf(false) }

    if (showConstructiveFeedbackStep) {
        // Constructive Feedback Sub-step (Prevents public 1-star reviews)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.feedback_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.feedback_dialog_description),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@quakecast.app?subject=QuakeCast%20App%20Feedback")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (_: Exception) {}
                        onFeedback()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.feedback_dialog_send_button), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.no_thanks), color = Color.Gray)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.testTag("rate_feedback_dialog")
        )
    } else {
        // Primary Soft Rating Question
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⭐️", fontSize = 26.sp)
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.rate_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.rate_dialog_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan)
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.rate_dialog_description),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val packageName = context.packageName
                            try {
                                val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(playIntent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(webIntent)
                            }
                            onRated()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_now_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.rate_dialog_confirm_button), color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showConstructiveFeedbackStep = true },
                            modifier = Modifier.testTag("rate_feedback_button")
                        ) {
                            Text(stringResource(R.string.rate_dialog_feedback_button), color = Color.LightGray, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("rate_later_button")
                        ) {
                            Text(stringResource(R.string.maybe_later), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.testTag("rate_app_dialog")
        )
    }
}
