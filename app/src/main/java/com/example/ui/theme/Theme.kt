package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HealthDataManager

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant
)

private val HighContrastColorScheme = lightColorScheme(
    primary = high_contrast_primary,
    onPrimary = high_contrast_onPrimary,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF004D40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF14532D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBF7D0),
    onTertiaryContainer = Color(0xFF000000),
    error = Color(0xFF991B1B),
    errorContainer = Color(0xFFFCA5A5),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF000000),
    background = high_contrast_background,
    onBackground = high_contrast_onBackground,
    surface = high_contrast_surface,
    onSurface = high_contrast_onSurface,
    surfaceVariant = high_contrast_surfaceVariant,
    onSurfaceVariant = high_contrast_onSurfaceVariant,
    outline = high_contrast_outline,
    outlineVariant = Color(0xFF000000)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val accessibilityPrefs by HealthDataManager.accessibility.collectAsStateWithLifecycle()
    val colorScheme = if (accessibilityPrefs.highContrast) HighContrastColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * accessibilityPrefs.fontScale
    )

    CompositionLocalProvider(
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
