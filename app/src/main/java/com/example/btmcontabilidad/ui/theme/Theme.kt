package com.example.btmcontabilidad.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = SecondaryNavy,
    onPrimaryContainer = Color.White,
    secondary = TertiaryBlue,
    onSecondary = Color.White,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = DeepNavyDark,
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = DeepNavy,
    onPrimaryContainer = Color.White,
    secondary = SecondaryNavy,
    onSecondary = Color.White,
    tertiary = TertiaryBlue,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onBackground = OnSurfaceDarkText,
    onSurface = OnSurfaceDarkText,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SubtitleGray,
    outline = OutlineLight
)

@Composable
fun BTMContabilidadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
