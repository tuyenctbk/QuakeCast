package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String, val description: String) {
    SYSTEM("System Default", "Matches system dark/light mode preference"),
    DARK("Dark Theme", "Seismic dark surface palette for night & OLED screens"),
    LIGHT("Light Theme", "Clean high-contrast light background")
}

object ThemeManager {
    private const val PREFS_NAME = "quakecast_theme_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"

    fun getSavedThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = SeismicRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D0C11),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = PulseCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = SeismicOrange,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC00018),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF006875),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF97F0FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF904D00),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F)
)

@Composable
fun QuakeCastTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    QuakeCastTheme(themeMode = AppThemeMode.SYSTEM, content = content)
}
