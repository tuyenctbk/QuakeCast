package com.example.model

import java.io.Serializable

enum class FeedTimeframe(val label: String, val endpoint: String) {
    PAST_HOUR("Past Hour", "all_hour.geojson"),
    PAST_DAY("Past 24 Hours", "all_day.geojson"),
    PAST_DAY_SIGNIFICANT("24h Significant (M4.5+)", "4.5_day.geojson"),
    PAST_WEEK("Past 7 Days", "4.5_week.geojson"),
    PAST_MONTH("Past 30 Days", "significant_month.geojson")
}

enum class MagFilter(val minMag: Double, val label: String) {
    ALL(0.0, "All M1.0+"),
    M25(2.5, "M2.5+"),
    M45(4.5, "M4.5+"),
    M60(6.0, "M6.0+ Major")
}

data class Earthquake(
    val id: String,
    val magnitude: Double,
    val place: String,
    val time: Long,
    val url: String,
    val detailUrl: String,
    val felt: Int? = null,
    val cdi: Double? = null,
    val mmi: Double? = null,
    val alert: String? = null, // "green", "yellow", "orange", "red"
    val status: String = "reviewed",
    val tsunami: Int = 0,
    val sig: Int = 0,
    val net: String = "us",
    val code: String = "",
    val latitude: Double,
    val longitude: Double,
    val depth: Double, // in kilometers
    val title: String,
    val magType: String = "mw"
) : Serializable {
    val depthCategory: DepthCategory
        get() = when {
            depth < 30.0 -> DepthCategory.SHALLOW
            depth <= 70.0 -> DepthCategory.MID
            else -> DepthCategory.DEEP
        }

    val isSignificant: Boolean
        get() = magnitude >= 4.5 || sig >= 500

    val hasTsunamiAlert: Boolean
        get() = tsunami == 1

    // Energy calculation in Joules & TNT equivalent (10^(1.5*M + 4.8))
    val energyJoules: Double
        get() = Math.pow(10.0, 1.5 * magnitude + 4.8)

    val tntTons: Double
        get() = energyJoules / 4.184e9
}

enum class DepthCategory(val label: String, val depthRange: String) {
    SHALLOW("Shallow", "< 30 km"),
    MID("Intermediate", "30 - 70 km"),
    DEEP("Deep", "> 70 km")
}

data class UserLocation(
    val name: String = "San Francisco, CA",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val radiusKm: Double = 500.0
)

data class WaveArrivalInfo(
    val distanceKm: Double,
    val pWaveDelaySec: Double,
    val sWaveDelaySec: Double,
    val surfaceWaveDelaySec: Double
)
