package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun UpdateAppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                color = PulseCyan.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.update_dialog_version_ready),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan, fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.update_dialog_description),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                )
                Spacer(modifier = Modifier.height(10.dp))
                UpdateFeatureRow(
                    emoji = "⚡",
                    title = stringResource(R.string.update_feature_feed_title),
                    desc = stringResource(R.string.update_feature_feed_desc)
                )
                UpdateFeatureRow(
                    emoji = "🌐",
                    title = stringResource(R.string.update_feature_globe_title),
                    desc = stringResource(R.string.update_feature_globe_desc)
                )
                UpdateFeatureRow(
                    emoji = "🔔",
                    title = stringResource(R.string.update_feature_proximity_title),
                    desc = stringResource(R.string.update_feature_proximity_desc)
                )
            }
        },
        confirmButton = {
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
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("update_now_button")
            ) {
                Text(stringResource(R.string.update_button_now), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("update_later_button")
            ) {
                Text(stringResource(R.string.remind_me_later), color = Color.Gray)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("update_app_dialog")
    )
}

@Composable
private fun UpdateFeatureRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 11.sp))
        }
    }
}
