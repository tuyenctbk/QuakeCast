package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

enum class ShakeSensitivity(
    val label: String,
    val thresholdG: Float,
    val description: String
) {
    HIGH("High (Gentle Shake)", 1.8f, "Triggers with light wrist motion (~1.8g)"),
    MEDIUM("Medium (Standard)", 2.5f, "Standard shake motion required (~2.5g)"),
    LOW("Low (Vigorous Shake)", 3.5f, "Requires firm, deliberate shake (~3.5g)"),
    OFF("Disabled", 999f, "Shake-to-refresh disabled")
}

class ShakeDetector(
    context: Context,
    initialSensitivity: ShakeSensitivity = getSavedSensitivity(context),
    initialThresholdG: Float = getSavedThreshold(context),
    initialEnabled: Boolean = isShakeEnabled(context),
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime: Long = 0
    private var isListening = false

    var isEnabled: Boolean = initialEnabled && (initialSensitivity != ShakeSensitivity.OFF)
        set(value) {
            field = value
            if (!value) {
                stop()
            } else if (!isListening) {
                start()
            }
        }

    var thresholdG: Float = if (initialSensitivity != ShakeSensitivity.OFF) {
        initialSensitivity.thresholdG
    } else {
        initialThresholdG
    }

    var sensitivity: ShakeSensitivity = initialSensitivity
        set(value) {
            field = value
            if (value == ShakeSensitivity.OFF) {
                isEnabled = false
                stop()
            } else {
                thresholdG = value.thresholdG
                isEnabled = true
                if (!isListening) {
                    start()
                }
            }
        }

    companion object {
        private const val PREFS_NAME = "shake_detector_prefs"
        private const val KEY_SENSITIVITY = "shake_sensitivity"
        private const val KEY_THRESHOLD = "shake_threshold_g"
        private const val KEY_ENABLED = "shake_enabled"
        private const val SHAKE_SLOP_TIME_MS = 1500L

        const val MIN_THRESHOLD_G = 1.5f
        const val MAX_THRESHOLD_G = 4.5f
        const val DEFAULT_THRESHOLD_G = 2.5f

        fun getSavedSensitivity(context: Context): ShakeSensitivity {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_SENSITIVITY, ShakeSensitivity.MEDIUM.name) ?: ShakeSensitivity.MEDIUM.name
            return try {
                ShakeSensitivity.valueOf(name)
            } catch (_: Exception) {
                ShakeSensitivity.MEDIUM
            }
        }

        fun saveSavedSensitivity(context: Context, sensitivity: ShakeSensitivity) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SENSITIVITY, sensitivity.name)
                .putFloat(KEY_THRESHOLD, sensitivity.thresholdG)
                .putBoolean(KEY_ENABLED, sensitivity != ShakeSensitivity.OFF)
                .apply()
        }

        fun getSavedThreshold(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD_G)
        }

        fun saveSavedThreshold(context: Context, threshold: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nearestSensitivity = when {
                threshold <= 2.0f -> ShakeSensitivity.HIGH
                threshold <= 3.0f -> ShakeSensitivity.MEDIUM
                else -> ShakeSensitivity.LOW
            }
            prefs.edit()
                .putFloat(KEY_THRESHOLD, threshold)
                .putString(KEY_SENSITIVITY, nearestSensitivity.name)
                .apply()
        }

        fun isShakeEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, true)
        }

        fun saveShakeEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val sensitivityName = if (enabled) {
                val currentThreshold = prefs.getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD_G)
                when {
                    currentThreshold <= 2.0f -> ShakeSensitivity.HIGH.name
                    currentThreshold <= 3.0f -> ShakeSensitivity.MEDIUM.name
                    else -> ShakeSensitivity.LOW.name
                }
            } else {
                ShakeSensitivity.OFF.name
            }
            prefs.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_SENSITIVITY, sensitivityName)
                .apply()
        }
    }

    fun start() {
        if (!isListening && accelerometer != null && isEnabled) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Calculate total g-force magnitude
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        val activeThreshold = if (thresholdG > 0f) thresholdG else sensitivity.thresholdG

        if (gForce > activeThreshold) {
            val now = System.currentTimeMillis()
            // Ensure minimum time between shake triggers
            if (now - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                lastShakeTime = now
                Log.d("ShakeDetector", "Device physical tremor/shake detected! gForce=$gForce (threshold=$activeThreshold)")
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
