package com.example.util

import com.example.model.WaveArrivalInfo
import kotlin.math.sqrt

object WaveArrivalEstimator {

    /**
     * Estimates P-wave, S-wave, and Surface wave arrival times based on epicentral distance and hypocentral depth.
     * P-wave average speed: ~6.0 km/s in Earth's crust
     * S-wave average speed: ~3.5 km/s in Earth's crust
     * Surface wave speed: ~3.0 km/s
     */
    fun estimateWaveArrival(distanceKm: Double, depthKm: Double): WaveArrivalInfo {
        val slantDistanceKm = sqrt(distanceKm * distanceKm + depthKm * depthKm)
        
        val pWaveSpeedKmS = 6.1
        val sWaveSpeedKmS = 3.55
        val surfaceWaveSpeedKmS = 3.0

        val pWaveDelaySec = slantDistanceKm / pWaveSpeedKmS
        val sWaveDelaySec = slantDistanceKm / sWaveSpeedKmS
        val surfaceWaveDelaySec = distanceKm / surfaceWaveSpeedKmS

        return WaveArrivalInfo(
            distanceKm = distanceKm,
            pWaveDelaySec = pWaveDelaySec,
            sWaveDelaySec = sWaveDelaySec,
            surfaceWaveDelaySec = surfaceWaveDelaySec
        )
    }

    /**
     * Estimates approximate Modified Mercalli Intensity (MMI) based on magnitude and distance
     */
    fun estimateIntensityMmi(magnitude: Double, distanceKm: Double): String {
        // Simple empirical estimation formula: MMI approx = 1.5 * M - 3.5 * log10(R) + 3
        val distanceFactor = if (distanceKm < 10) 10.0 else distanceKm
        val mmiValue = 1.5 * magnitude - 3.2 * kotlin.math.log10(distanceFactor) + 3.0
        val clamped = mmiValue.coerceIn(1.0, 10.0)

        return when (clamped.toInt()) {
            1 -> "I (Not Felt)"
            2, 3 -> "II-III (Weak)"
            4 -> "IV (Light)"
            5 -> "V (Moderate)"
            6 -> "VI (Strong)"
            7 -> "VII (Very Strong)"
            8 -> "VIII (Severe)"
            9 -> "IX (Violent)"
            else -> "X+ (Extreme)"
        }
    }
}
