package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.mobile.MobileMainScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.QuakeCastTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.tv.TvScreenSaverScreen
import com.example.viewmodel.QuakeCastViewModel

class MainActivity : ComponentActivity() {

    private var viewModelRef: QuakeCastViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: QuakeCastViewModel = viewModel()
            viewModelRef = viewModel
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            var appThemeMode by remember {
                mutableStateOf(ThemeManager.getSavedThemeMode(this@MainActivity))
            }

            QuakeCastTheme(themeMode = appThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.isTvMode) {
                        TvScreenSaverScreen(
                            significantQuakes = uiState.significantQuakes,
                            currentIndex = uiState.tvIndex,
                            selectedQuake = uiState.selectedQuake,
                            onNext = { viewModel.navigateTvNext() },
                            onPrevious = { viewModel.navigateTvPrevious() },
                            onSelectQuake = { viewModel.selectQuake(it) },
                            onExitTvMode = { viewModel.setTvMode(false) }
                        )
                    } else {
                        MobileMainScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            appThemeMode = appThemeMode,
                            onUpdateThemeMode = { newMode ->
                                appThemeMode = newMode
                                ThemeManager.saveThemeMode(this@MainActivity, newMode)
                            },
                            onToggleTvMode = { viewModel.setTvMode(true) }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        viewModelRef?.let { vm ->
            if (vm.uiState.value.isTvMode) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        vm.navigateTvNext()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> {
                        vm.navigateTvPrevious()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                        vm.toggleAutoRotate()
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
