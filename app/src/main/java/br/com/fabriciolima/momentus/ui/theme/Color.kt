package br.com.fabriciolima.momentus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Paleta Premium Dark (Inspirada na referência)
val PremiumBackground = Color(0xFF000000) // Preto Absoluto
val PremiumSurface = Color(0xFF121212)    // Cinza Profundo para Cards
val PremiumOutline = Color(0xFF252525)    // Borda sutil para camadas

// Cores de Identidade
val DarkBlue = Color(0xFF0A1A4A)
val GreenVibrant = Color(0xFF3DDC84)
val GreenDark = Color(0xFF2A9371)
val OffWhite = Color(0xFFE6E6E6)

// Cores do Tema Claro (Light Theme)
val md_theme_light_primary = GreenVibrant
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFB9FBC9)
val md_theme_light_secondary = DarkBlue
val md_theme_light_onSecondary = Color.White
val md_theme_light_tertiary = GreenDark
val md_theme_light_onTertiary = Color.White
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color.White
val md_theme_light_background = OffWhite
val md_theme_light_onBackground = DarkBlue
val md_theme_light_surface = Color.White
val md_theme_light_onSurface = DarkBlue
val md_theme_light_surfaceVariant = Color(0xFFE0E2EC)
val md_theme_light_onSurfaceVariant = Color(0xFF44474F)
val md_theme_light_outline = Color(0xFFD1D1D6)

// Cores do Tema Escuro (Dark Theme - Ponto 3: Estética Layers)
val md_theme_dark_primary = GreenVibrant
val md_theme_dark_onPrimary = Color.Black
val md_theme_dark_primaryContainer = Color(0xFF1A3326) // Verde muito escuro
val md_theme_dark_onPrimaryContainer = GreenVibrant
val md_theme_dark_secondary = Color(0xFFADC6FF)
val md_theme_dark_onSecondary = Color(0xFF002E69)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_background = PremiumBackground
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = PremiumSurface
val md_theme_dark_onSurface = Color(0xFFE2E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF1E1E1E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_outline = PremiumOutline


// --- Paleta de Cores do Google Agenda ---

private fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

val googleCalendarColors: Map<String, Color> = mapOf(
    "1" to Color(0xFF7986CB),
    "2" to Color(0xFF33B679),
    "3" to Color(0xFF8E24AA),
    "4" to Color(0xFFE67C73),
    "5" to Color(0xFFF6BF26),
    "6" to Color(0xFFF4511E),
    "7" to Color(0xFF039BE5),
    "8" to Color(0xFF616161),
    "9" to Color(0xFF3F51B5),
    "10" to Color(0xFF0B8043),
    "11" to Color(0xFFD50000)
)

fun getGoogleColorId(colorHex: String): String? {
    val targetColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
        return null
    }
    val entry = googleCalendarColors.entries.find { it.value.toArgb() == targetColor.toArgb() }
    return entry?.key
}
