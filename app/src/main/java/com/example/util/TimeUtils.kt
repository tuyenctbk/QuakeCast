package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    fun formatRelativeTime(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timeMs

        if (diffMs < 0) return "Just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "${seconds}s ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                sdf.format(Date(timeMs))
            }
        }
    }

    fun formatDateTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy · HH:mm:ss 'UTC'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timeMs))
    }

    fun formatLocalTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }
}
