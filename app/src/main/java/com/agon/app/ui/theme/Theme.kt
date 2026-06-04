package com.agon.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GoogleLightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = AccentBlueLight,
    onPrimaryContainer = GoogleBlueDark,
    secondary = GoogleBlue,
    onSecondary = TextOnPrimary,
    secondaryContainer = AccentBlueLight,
    onSecondaryContainer = GoogleBlueDark,
    tertiary = GoogleGreen,
    onTertiary = TextOnPrimary,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = BorderColor,
    error = ErrorRed,
    onError = TextOnPrimary,
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = AccentRed
)

private val GoogleDarkColorScheme = darkColorScheme(
    primary = GoogleBlueLight,
    onPrimary = Color(0xFF003C8F),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = GoogleBlueLight,
    onSecondary = Color(0xFF003C8F),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8EAED),
    error = Color(0xFFCF6679),
    onError = Color(0xFF121212)
)

@Composable
fun GoogleMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> GoogleDarkColorScheme
        else -> GoogleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GoogleTypography,
        content = content
    )
}