package com.example.util

import kotlin.math.*

object DistanceCalculator {

    /**
     * Calculates distance between two coordinates in kilometers using Haversine formula
     */
    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Earth's mean radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates cardinal direction from origin (lat1, lon1) to target (lat2, lon2)
     */
    fun calculateBearingDirection(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): String {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360) % 360

        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing + 22.5) / 45).toInt() % 8
        return directions[index]
    }
}
