package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Saffron500,
        onPrimary = Color.Black,
        primaryContainer = DarkSurfaceVariant,
        onPrimaryContainer = Saffron400,
        secondary = Cyan400,
        onSecondary = Color.Black,
        secondaryContainer = DarkSurfaceVariant,
        onSecondaryContainer = Cyan100,
        tertiary = Orange500,
        onTertiary = Color.White,
        background = DarkCanvas,
        onBackground = Slate50,
        surface = DarkSurface,
        onSurface = Slate50,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = Slate300,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = Red500,
        onError = Color.White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Saffron600,
        onPrimary = Color.White,
        primaryContainer = LightSurfaceVariant,
        onPrimaryContainer = Saffron700,
        secondary = Cyan600,
        onSecondary = Color.White,
        secondaryContainer = LightSurfaceVariant,
        onSecondaryContainer = Cyan700,
        tertiary = Orange500,
        onTertiary = Color.White,
        background = LightCanvas,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        error = Red600,
        onError = Color.White,
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
