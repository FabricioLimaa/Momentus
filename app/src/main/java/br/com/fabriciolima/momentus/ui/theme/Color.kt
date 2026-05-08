package br.com.fabriciolima.momentus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Paleta Premium "Deep Navy" e "Emerald" (Baseada nas novas imagens)

// Cores de Fundo e Superfície (Tema Escuro)
val DeepNavyBackground = Color(0xFF0F172A) // Fundo Principal (Deep Navy)
val DeepNavySurface = Color(0xFF1E293B)    // Cards e Diálogos
val DeepNavyOutline = Color(0xFF334155)    // Bordas sutis

// Cores de Destaque
val EmeraldGreen = Color(0xFF10B981)       // Verde Esmeralda Premium (Ação principal)
val EmeraldLight = Color(0xFF34D399)       // Verde mais claro para estados ativos
val EmeraldDark = Color(0xFF065F46)        // Verde escuro para containers

// Cores de Texto e Utilitários
val TextPrimaryDark = Color(0xFFF8FAFC)    // Quase branco para texto principal
val TextSecondaryDark = Color(0xFF94A3B8)  // Cinza azulado para texto secundário

// Cores do Tema Claro (Light Theme)
val md_theme_light_primary = EmeraldGreen
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD1FAE5)
val md_theme_light_onPrimaryContainer = EmeraldDark
val md_theme_light_secondary = DeepNavyBackground
val md_theme_light_onSecondary = Color.White
val md_theme_light_error = Color(0xFFEF4444)
val md_theme_light_background = Color(0xFFF1F5F9) // Cinza azulado muito claro
val md_theme_light_onBackground = Color(0xFF0F172A)
val md_theme_light_surface = Color.White
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFE2E8F0)
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFFCBD5E1)

// Cores do Tema Escuro (Dark Theme)
val md_theme_dark_primary = EmeraldGreen
val md_theme_dark_onPrimary = Color(0xFF064E3B)
val md_theme_dark_primaryContainer = EmeraldDark.copy(alpha = 0.3f)
val md_theme_dark_onPrimaryContainer = EmeraldLight
val md_theme_dark_secondary = Color(0xFF38BDF8) // Azul claro para variação
val md_theme_dark_onSecondary = Color(0xFF0C4A6E)
val md_theme_dark_error = Color(0xFFFCA5A5)
val md_theme_dark_background = DeepNavyBackground
val md_theme_dark_onBackground = TextPrimaryDark
val md_theme_dark_surface = DeepNavySurface
val md_theme_dark_onSurface = TextPrimaryDark
val md_theme_dark_surfaceVariant = Color(0xFF334155)
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
