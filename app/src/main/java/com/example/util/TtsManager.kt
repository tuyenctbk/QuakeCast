package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.Earthquake
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Language US is not supported or missing data")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("TtsManager", "TextToSpeech initialization failed with status $status")
        }
    }

    fun speakAlert(quake: Earthquake, distanceKm: Double, userLocationName: String) {
        if (!isInitialized) return

        val magText = String.format("%.1f", quake.magnitude)
        val distText = String.format("%.0f", distanceKm)
        
        val warningSpeech = buildString {
            append("Earthquake warning! ")
            append("A magnitude $magText tremor occurred $distText kilometers from $userLocationName. ")
            if (quake.hasTsunamiAlert) {
                append("Tsunami warning is in effect. ")
            }
            append("If shaking is felt, drop, cover, and hold on.")
        }

        speakText(warningSpeech)
    }

    fun speakText(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "QuakeCastAlert_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        }
    }
}
