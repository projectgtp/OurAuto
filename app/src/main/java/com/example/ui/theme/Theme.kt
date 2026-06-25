package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    themeAccent: String = "Violet",
    content: @Composable () -> Unit,
) {
    val primaryDark = when (themeAccent) {
        "Emerald" -> Color(0xFF00C9A7)
        "Amber"   -> Color(0xFFFFD166)
        "Crimson" -> Color(0xFFFF5370)
        "Indigo"  -> Color(0xFF8AB4F8)
        else      -> Color(0xFF7C6FFF) // Violet default
    }

    val primaryLight = when (themeAccent) {
        "Emerald" -> Color(0xFF007A6A)
        "Amber"   -> Color(0xFFB07000)
        "Crimson" -> Color(0xFFB5003C)
        "Indigo"  -> Color(0xFF1565C0)
        else      -> Color(0xFF5A4FD3)
    }

    val colorScheme = if (themeMode == "LIGHT") {
        lightColorScheme(
            primary            = primaryLight,
            onPrimary          = Color.White,
            primaryContainer   = primaryLight.copy(alpha = 0.12f),
            onPrimaryContainer = primaryLight,
            secondary          = MintLight,
            onSecondary        = Color.White,
            tertiary           = CoralLight,
            onTertiary         = Color.White,
            background         = LightBg,
            onBackground       = DarkText,
            surface            = LightSurface,
            onSurface          = DarkText,
            surfaceVariant     = LightSurfaceCard,
            onSurfaceVariant   = MutedText,
            outline            = LightBorder,
            outlineVariant     = LightBorder.copy(alpha = 0.6f),
            error              = LightError,
            onError            = Color.White
        )
    } else {
        darkColorScheme(
            primary            = primaryDark,
            onPrimary          = DarkBg,
            primaryContainer   = primaryDark.copy(alpha = 0.18f),
            onPrimaryContainer = primaryDark,
            secondary          = MintSecondary,
            onSecondary        = DarkBg,
            tertiary           = CoralTertiary,
            onTertiary         = DarkBg,
            background         = DarkBg,
            onBackground       = IceText,
            surface            = DarkSurface,
            onSurface          = IceText,
            surfaceVariant     = DarkSurfaceCard,
            onSurfaceVariant   = FogText,
            outline            = DarkBorder,
            outlineVariant     = DarkBorder.copy(alpha = 0.6f),
            error              = DarkError,
            onError            = DarkBg
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
