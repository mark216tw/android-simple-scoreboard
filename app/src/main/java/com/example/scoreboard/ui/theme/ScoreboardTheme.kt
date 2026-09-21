package com.example.scoreboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.scoreboard.data.AppTheme

@Immutable
data class ScoreboardColors(
    val home: Color,
    val away: Color,
    val onHome: Color,
    val onAway: Color,
)

data class ThemeDefinition(
    val light: ColorScheme,
    val dark: ColorScheme,
    val lightTeams: ScoreboardColors,
    val darkTeams: ScoreboardColors,
)

private fun definition(
    primary: Color,
    secondary: Color,
    lightBackground: Color,
    darkBackground: Color,
    home: Color,
    away: Color,
    darkHome: Color,
    darkAway: Color,
) = ThemeDefinition(
    light = lightColorScheme(
        primary = primary,
        secondary = secondary,
        background = lightBackground,
        surface = lightBackground,
        surfaceVariant = Color.White.copy(alpha = 0.72f),
        onBackground = Color(0xFF202124),
        onSurface = Color(0xFF202124),
    ),
    dark = darkColorScheme(
        primary = primary,
        secondary = secondary,
        background = darkBackground,
        surface = darkBackground,
        surfaceVariant = Color(0xFF26272D),
        onBackground = Color(0xFFF5F2ED),
        onSurface = Color(0xFFF5F2ED),
    ),
    lightTeams = ScoreboardColors(
        home = home,
        away = away,
        onHome = contentColorFor(home),
        onAway = contentColorFor(away),
    ),
    darkTeams = ScoreboardColors(
        home = darkHome,
        away = darkAway,
        onHome = contentColorFor(darkHome),
        onAway = contentColorFor(darkAway),
    ),
)

private val themes = mapOf(
    AppTheme.ENERGY to definition(
        primary = Color(0xFFE94F37), secondary = Color(0xFFFFB000),
        lightBackground = Color(0xFFFFF7E8), darkBackground = Color(0xFF211A18),
        home = Color(0xFFE94F37), away = Color(0xFF1976D2),
        darkHome = Color(0xFF9E382B), darkAway = Color(0xFF155B96),
    ),
    AppTheme.OCEAN to definition(
        primary = Color(0xFF0077B6), secondary = Color(0xFF00B4D8),
        lightBackground = Color(0xFFEAF8FF), darkBackground = Color(0xFF101D24),
        home = Color(0xFF0077B6), away = Color(0xFF00A896),
        darkHome = Color(0xFF07577C), darkAway = Color(0xFF087568),
    ),
    AppTheme.MINT to definition(
        primary = Color(0xFF168A72), secondary = Color(0xFF70C1B3),
        lightBackground = Color(0xFFF0FFF8), darkBackground = Color(0xFF13211E),
        home = Color(0xFF168A72), away = Color(0xFF5B7C2A),
        darkHome = Color(0xFF116453), darkAway = Color(0xFF465F23),
    ),
    AppTheme.GRAPE to definition(
        primary = Color(0xFF7B2CBF), secondary = Color(0xFFE056A7),
        lightBackground = Color(0xFFFBF2FF), darkBackground = Color(0xFF201724),
        home = Color(0xFF7B2CBF), away = Color(0xFFC23B8B),
        darkHome = Color(0xFF592182), darkAway = Color(0xFF862A61),
    ),
    AppTheme.LEMON to definition(
        primary = Color(0xFFB06B00), secondary = Color(0xFFF4B400),
        lightBackground = Color(0xFFFFFBE5), darkBackground = Color(0xFF211F14),
        home = Color(0xFFD97706), away = Color(0xFF6D7F20),
        darkHome = Color(0xFF9B5608), darkAway = Color(0xFF56641D),
    ),
    AppTheme.MATCH to definition(
        primary = Color(0xFFD62828), secondary = Color(0xFF1565C0),
        lightBackground = Color(0xFFF7F7F7), darkBackground = Color(0xFF191919),
        home = Color(0xFFD62828), away = Color(0xFF1565C0),
        darkHome = Color(0xFF941F1F), darkAway = Color(0xFF124C8D),
    ),
)

@Composable
fun ScoreboardTheme(
    appTheme: AppTheme,
    customHue: Float,
    darkTheme: Boolean,
    content: @Composable (ScoreboardColors) -> Unit,
) {
    val definition = if (appTheme == AppTheme.CUSTOM) {
        customDefinition(customHue)
    } else {
        themes.getValue(appTheme)
    }
    MaterialTheme(colorScheme = if (darkTheme) definition.dark else definition.light) {
        content(if (darkTheme) definition.darkTeams else definition.lightTeams)
    }
}

fun previewColor(theme: AppTheme, customHue: Float = 24f): Color =
    if (theme == AppTheme.CUSTOM) customDefinition(customHue).light.primary
    else themes.getValue(theme).light.primary

private fun customDefinition(hue: Float): ThemeDefinition {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    val awayHue = (normalizedHue + 180f) % 360f
    val primary = Color.hsv(normalizedHue, 0.78f, 0.82f)
    val home = Color.hsv(normalizedHue, 0.76f, 0.78f)
    val away = Color.hsv(awayHue, 0.76f, 0.78f)
    val darkHome = Color.hsv(normalizedHue, 0.68f, 0.58f)
    val darkAway = Color.hsv(awayHue, 0.68f, 0.58f)
    val lightBackground = Color.hsv(normalizedHue, 0.08f, 0.99f)
    val darkBackground = Color.hsv(normalizedHue, 0.16f, 0.13f)

    return ThemeDefinition(
        light = lightColorScheme(
            primary = primary,
            onPrimary = contentColorFor(primary),
            secondary = Color.hsv(awayHue, 0.68f, 0.72f),
            background = lightBackground,
            surface = lightBackground,
            surfaceVariant = Color.White.copy(alpha = 0.76f),
            onBackground = Color(0xFF202124),
            onSurface = Color(0xFF202124),
        ),
        dark = darkColorScheme(
            primary = Color.hsv(normalizedHue, 0.62f, 0.84f),
            onPrimary = contentColorFor(Color.hsv(normalizedHue, 0.62f, 0.84f)),
            secondary = Color.hsv(awayHue, 0.58f, 0.78f),
            background = darkBackground,
            surface = darkBackground,
            surfaceVariant = Color.hsv(normalizedHue, 0.12f, 0.19f),
            onBackground = Color(0xFFF5F2ED),
            onSurface = Color(0xFFF5F2ED),
        ),
        lightTeams = ScoreboardColors(
            home = home,
            away = away,
            onHome = contentColorFor(home),
            onAway = contentColorFor(away),
        ),
        darkTeams = ScoreboardColors(
            home = darkHome,
            away = darkAway,
            onHome = contentColorFor(darkHome),
            onAway = contentColorFor(darkAway),
        ),
    )
}

private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.179f) Color.Black else Color.White
