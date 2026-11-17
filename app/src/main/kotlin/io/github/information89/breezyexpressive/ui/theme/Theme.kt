package io.github.information89.breezyexpressive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
