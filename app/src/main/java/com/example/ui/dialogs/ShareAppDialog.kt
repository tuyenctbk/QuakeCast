package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Share
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
fun ShareAppDialog(
    onShared: () -> Unit,
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
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.share_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.share_dialog_subtitle),
                    style = MaterialTheme.typography.labelSmall.copy(color = PulseCyan)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.share_dialog_description),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.share_dialog_bullet_points),
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontSize = 12.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val packageName = context.packageName
                    val shareText = context.getString(R.string.share_app_message, packageName)

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.share_dialog_confirm_button))
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    onShared()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("share_app_confirm_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.share_dialog_confirm_button), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("share_app_later_button")
            ) {
                Text(stringResource(R.string.remind_me_later), color = Color.Gray)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("share_app_dialog")
    )
}
