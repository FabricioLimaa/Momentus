package br.com.fabriciolima.momentus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Nova Paleta baseada na Logo
val DarkBlue = Color(0xFF0A1A4A)
val GreenVibrant = Color(0xFF3DDC84)
val GreenDark = Color(0xFF2A9371)
val OffWhite = Color(0xFFF7F9FF)

// Cores do Tema Claro (Light Theme)
val md_theme_light_primary = GreenVibrant
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFB9FBC9) // Verde claro para containers
val md_theme_light_secondary = DarkBlue
val md_theme_light_onSecondary = Color.White
val md_theme_light_tertiary = GreenDark
val md_theme_light_onTertiary = Color.White
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color.White
val md_theme_light_background = OffWhite
val md_theme_light_onBackground = DarkBlue
val md_theme_light_surface = OffWhite
val md_theme_light_onSurface = DarkBlue
val md_theme_light_surfaceVariant = Color(0xFFE0E2EC)
val md_theme_light_onSurfaceVariant = Color(0xFF44474F)
val md_theme_light_outline = Color(0xFF74777F)
val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
val md_theme_light_inverseSurface = Color(0xFF2F3033)
val md_theme_light_inversePrimary = Color(0xFF72DA96)
val md_theme_light_surfaceTint = md_theme_light_primary

// Cores do Tema Escuro (Dark Theme)
val md_theme_dark_primary = GreenVibrant
val md_theme_dark_onPrimary = DarkBlue
val md_theme_dark_primaryContainer = GreenDark
val md_theme_dark_secondary = Color(0xFFADC6FF) // Azul claro para detalhes
val md_theme_dark_onSecondary = Color(0xFF002E69)
val md_theme_dark_tertiary = Color(0xFF8ADAAE)
val md_theme_dark_onTertiary = Color(0xFF003823)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_background = DarkBlue
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = Color(0xFF1C2C5A) // Pouco mais claro que o fundo
val md_theme_dark_onSurface = Color(0xFFE2E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF44474F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_outline = Color(0xFF8E9099)
val md_theme_dark_inverseOnSurface = Color(0xFF1B1B1F)
val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
val md_theme_dark_inversePrimary = md_theme_light_primary
val md_theme_dark_surfaceTint = md_theme_dark_primary


// --- Paleta de Cores do Google Agenda ---

private fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

// Mapeia o colorId (String) do Google Calendar para a cor correspondente no Compose.
val googleCalendarColors: Map<String, Color> = mapOf(
    "1" to Color(0xFF7986CB), // Lavanda
    "2" to Color(0xFF33B679), // Sálvia
    "3" to Color(0xFF8E24AA), // Uva
    "4" to Color(0xFFE67C73), // Flamingo
    "5" to Color(0xFFF6BF26), // Banana
    "6" to Color(0xFFF4511E), // Tangerina
    "7" to Color(0xFF039BE5), // Pavão
    "8" to Color(0xFF616161), // Grafite
    "9" to Color(0xFF3F51B5), // Mirtilo
    "10" to Color(0xFF0B8043), // Manjericão
    "11" to Color(0xFFD50000)  // Tomate
)

/**
 * Encontra o `colorId` do Google Agenda a partir de uma string de cor hexadecimal.
 * @param colorHex A cor em formato hexadecimal (ex: "#33B679").
 * @return O `colorId` correspondente (ex: "2") ou null se não for encontrada.
 */
fun getGoogleColorId(colorHex: String): String? {
    val targetColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
        return null
    }

    // Compara os valores ARGB para garantir a correspondência exata.
    val entry = googleCalendarColors.entries.find { it.value.toArgb() == targetColor.toArgb() }
    return entry?.key
}
