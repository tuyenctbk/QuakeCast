package com.example

import com.example.util.DistanceCalculator
import com.example.util.WaveArrivalEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeismicCalculationTest {

    @Test
    fun testHaversineDistanceCalculation() {
        // Distance between SF (37.7749, -122.4194) and LA (34.0522, -118.2437) ~ 559 km
        val distance = DistanceCalculator.calculateDistanceKm(37.7749, -122.4194, 34.0522, -118.2437)
        assertTrue(distance > 500.0 && distance < 600.0)
    }

    @Test
    fun testWaveArrivalEstimator() {
        // Distance 100km, Depth 10km
        val waveInfo = WaveArrivalEstimator.estimateWaveArrival(100.0, 10.0)
        // P-wave speed ~6.1 km/s => ~16.4s
        assertTrue(waveInfo.pWaveDelaySec > 14.0 && waveInfo.pWaveDelaySec < 19.0)
        // S-wave speed ~3.55 km/s => ~28.3s
        assertTrue(waveInfo.sWaveDelaySec > 25.0 && waveInfo.sWaveDelaySec < 32.0)
    }
}
