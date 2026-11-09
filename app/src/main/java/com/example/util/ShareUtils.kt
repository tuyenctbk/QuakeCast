package com.example.util

import android.content.Context
import android.content.Intent
import com.example.model.Earthquake

object ShareUtils {
    fun shareEarthquake(context: Context, quake: Earthquake, distanceKm: Double? = null) {
        val distanceStr = if (distanceKm != null) "Distance: ${String.format("%.0f", distanceKm)} km away\n" else ""
        val tsunamiStr = if (quake.hasTsunamiAlert) "⚠️ TSUNAMI WARNING ACTIVE\n" else ""

        val shareText = """
            🚨 EARTHQUAKE TELEMETRY ALERT — QuakeCast
            
            Magnitude: M ${String.format("%.1f", quake.magnitude)}
            Location: ${quake.place}
            Time: ${TimeUtils.formatDateTime(quake.time)}
            Focal Depth: ${quake.depth} km
            Coordinates: ${quake.latitude}, ${quake.longitude}
            $distanceStr$tsunamiStr
            USGS Deep Link & Live Bulletin:
            ${quake.url}
            
            Shared via QuakeCast Real-Time Seismic Telemetry
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "M ${String.format("%.1f", quake.magnitude)} Earthquake — ${quake.place}")
            type = "text/plain"
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share Earthquake Telemetry")
        context.startActivity(chooserIntent)
    }
}
