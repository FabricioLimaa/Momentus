package br.com.fabriciolima.momentus.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extensão de Modifier para aplicar o efeito Glassmorphism (efeito vítreo).
 * Ideal para cards flutuantes no tema escuro.
 */
fun Modifier.glassmorphism(
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 12.dp,
    isDark: Boolean = true
): Modifier = this
    .blur(blurRadius)
    .background(
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.4f)
                )
            }
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.2f),
                Color.Transparent
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Um Container pré-configurado com efeito Glassmorphism adaptativo.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            surfaceColor.copy(alpha = 0.7f),
                            surfaceColor.copy(alpha = 0.4f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.15f else 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}
