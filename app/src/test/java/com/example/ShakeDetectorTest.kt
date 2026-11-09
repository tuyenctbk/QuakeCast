package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ShakeDetector
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShakeDetectorTest {

    @Test
    fun testShakeDetectorInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var shakeTriggered = false

        val detector = ShakeDetector(context) {
            shakeTriggered = true
        }

        assertNotNull(detector)
        detector.start()
        detector.stop()
    }
}
