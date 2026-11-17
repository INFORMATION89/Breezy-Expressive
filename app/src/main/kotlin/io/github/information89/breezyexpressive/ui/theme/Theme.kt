package io.github.information89.breezyexpressive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.information89.breezyexpressive.data.store.Appearance
import io.github.information89.breezyexpressive.data.store.ThemeMode

/** The current appearance settings, for components that react to them (grain, blur, animation). */
val LocalAppearance = staticCompositionLocalOf { Appearance() }

@Composable
fun Appearance.isDarkTheme(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

// Used only when wallpaper colors are turned off. Warm, slightly sunny palette.
private val FallbackLight = lightColorScheme(
    primary = Color(0xFF8F4C2E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF390C00),
    secondary = Color(0xFF6F5B40),
    secondaryContainer = Color(0xFFFADEBC),
    tertiary = Color(0xFF56633B),
    tertiaryContainer = Color(0xFFD9E8B6),
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFFFB597),
    onPrimary = Color(0xFF561F06),
    primaryContainer = Color(0xFF723519),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFDDC2A1),
    secondaryContainer = Color(0xFF56442B),
    tertiary = Color(0xFFBDCC9C),
    tertiaryContainer = Color(0xFF3F4B26),
)

@Composable
fun BreezyTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = appearance.isDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        appearance.dynamicColor && dark -> dynamicDarkColorScheme(context)
        appearance.dynamicColor -> dynamicLightColorScheme(context)
        dark -> FallbackDark
        else -> FallbackLight
    }

    CompositionLocalProvider(
        LocalAppearance provides appearance,
        LocalTypeVoices provides DefaultTypeVoices,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = DataTypography,
            content = content,
        )
    }
}

/**
 * Colors taken from the current conditions instead of the wallpaper: neutral surfaces and text,
 * and a single accent (gold sun, moon silver, rain blue…) for highlights.
 */
fun conditionColorScheme(accent: Color, dark: Boolean): ColorScheme {
    val onAccent = if (dark) Color(0xFF111318) else Color.White
    val base = if (dark) {
        darkColorScheme(
            background = Color(0xFF0E0F12),
            surface = Color(0xFF0E0F12),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xB8FFFFFF), // translucent white, reads on any sky
            surfaceContainerLowest = Color(0xFF0A0B0D),
            surfaceContainerLow = Color(0xFF15171B),
            surfaceContainer = Color(0xFF1A1C21),
            surfaceContainerHigh = Color(0xFF22252B),
            surfaceContainerHighest = Color(0xFF2B2E35),
            outline = Color(0xFF6B717C),
            outlineVariant = Color(0xFF3A3E46),
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF6F7F9),
            surface = Color(0xFFF6F7F9),
            onSurface = Color(0xFF15171B),
            onSurfaceVariant = Color(0xFF545A65),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF0F2F5),
            surfaceContainer = Color(0xFFEAEDF1),
            surfaceContainerHigh = Color(0xFFE3E6EB),
            surfaceContainerHighest = Color(0xFFDCE0E6),
            outline = Color(0xFF8A909B),
            outlineVariant = Color(0xFFC8CDD5),
        )
    }
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accent,
        onSecondaryContainer = onAccent,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accent,
        onTertiaryContainer = onAccent,
        surfaceTint = accent,
    )
}
