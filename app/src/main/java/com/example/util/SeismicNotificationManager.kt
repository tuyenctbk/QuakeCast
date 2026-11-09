package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.Earthquake

object SeismicNotificationManager {

    private const val CHANNEL_ID = "quakecast_proximity_alerts_v2"
    private const val CHANNEL_NAME = "Critical Seismic Proximity Alerts"
    private const val CHANNEL_DESC = "High-priority audible seismic warnings for nearby tremors"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 600, 200, 800)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun triggerProximityAlertNotification(
        context: Context,
        quake: Earthquake,
        distanceKm: Double,
        userLocationName: String
    ) {
        try {
            createNotificationChannel(context)

            // Acquire temporary WakeLock to ensure audio/vibration triggers even if screen is locked
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "QuakeCast:ProximityAlertWakeLock"
            )
            wakeLock?.acquire(3000L) // Hold wake lock for 3 seconds

            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Play custom alert sound tone
            try {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmSound)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = false
                }
                ringtone?.play()
            } catch (e: Exception) {
                Log.e("SeismicNotification", "Error playing ringtone tone", e)
            }

            // PendingIntent to launch main activity when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                quake.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val titleText = "🚨 M ${String.format("%.1f", quake.magnitude)} Seismic Alert (${distanceKm.toInt()} km)"
            val bodyText = "${quake.place} — ${String.format("%.0f", distanceKm)} km from $userLocationName. Tap to view live telemetry."

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(titleText)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(alarmSound)
                .setVibrate(longArrayOf(0, 400, 200, 600, 200, 800))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(quake.id.hashCode(), builder.build())
            } catch (e: SecurityException) {
                Log.w("SeismicNotification", "Missing POST_NOTIFICATIONS permission", e)
            }

        } catch (e: Exception) {
            Log.e("SeismicNotification", "Error triggering proximity notification", e)
        }
    }
}
