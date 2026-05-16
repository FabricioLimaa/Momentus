package br.com.fabriciolima.momentus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Paleta Oficial Momentus Premium "Unicorn"
val DeepNavyBackground = Color(0xFF0F172A) // Background Escuro Principal
val DeepNavySurface = Color(0xFF1E293B)    // Cards Escuros
val DeepNavyOutline = Color(0xFF334155)    // Bordas e Elementos

val EmeraldGreen = Color(0xFF10B981)       // Verde Secundário/Ação
val EmeraldNeon = Color(0xFF34D399)        // Verde Principal/Destaque
val EmeraldDeep = Color(0xFF065F46)        // Primária Principal

val TextPrimaryDark = Color(0xFFF8FAFC)    // Texto Claro
val TextSecondaryDark = Color(0xFF94A3B8)  // Texto Secundário

// Glassmorphism Colors (Transparências)
val GlassWhite = Color.White.copy(alpha = 0.1f)
val GlassDark = DeepNavySurface.copy(alpha = 0.7f)
val GlassBorder = Color.White.copy(alpha = 0.15f)

// Cores do Tema Claro (Light Theme - Startup Style)
val md_theme_light_primary = EmeraldDeep
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD1FAE5)
val md_theme_light_onPrimaryContainer = EmeraldDeep
val md_theme_light_secondary = DeepNavyBackground
val md_theme_light_onSecondary = Color.White
val md_theme_light_error = Color(0xFFEF4444)
val md_theme_light_background = Color(0xFFF8FAFC) // Branco suave
val md_theme_light_onBackground = DeepNavyBackground
val md_theme_light_surface = Color.White
val md_theme_light_onSurface = DeepNavyBackground
val md_theme_light_surfaceVariant = Color(0xFFF1F5F9) // Cards ultra claros
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFFE2E8F0) // Bordas discretas

// Cores do Tema Escuro (Dark Theme - Deep Navy)
val md_theme_dark_primary = EmeraldNeon
val md_theme_dark_onPrimary = Color(0xFF064E3B)
val md_theme_dark_primaryContainer = EmeraldDeep.copy(alpha = 0.4f)
val md_theme_dark_onPrimaryContainer = EmeraldNeon
val md_theme_dark_secondary = Color(0xFF38BDF8)
val md_theme_dark_onSecondary = Color(0xFF0C4A6E)
val md_theme_dark_error = Color(0xFFFCA5A5)
val md_theme_dark_background = DeepNavyBackground
val md_theme_dark_onBackground = TextPrimaryDark
val md_theme_dark_surface = DeepNavySurface
val md_theme_dark_onSurface = TextPrimaryDark
val md_theme_dark_surfaceVariant = Color(0xFF1E293B)
val md_theme_dark_onSurfaceVariant = TextSecondaryDark
val md_theme_dark_outline = DeepNavyOutline


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
