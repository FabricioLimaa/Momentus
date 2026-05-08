package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.time.format.DateTimeFormatter

@Composable
fun TimelineEventItem(
    item: ItemCronograma,
    category: Category,
    isChecked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val categoryColor = remember(category.cor) {
        try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { Color.Gray }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. Horário (Esquerda)
        Text(
            text = item.horarioInicio.format(formatter),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(50.dp).padding(top = 2.dp)
        )

        // 2. Indicador Vertical (Timeline)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp).fillMaxHeight()
        ) {
            // Linha superior (se não for o primeiro)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ponto colorido
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            // Linha inferior (se não for o último)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .minHeight(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        // 3. Conteúdo (Direita)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = item.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(if (isChecked) 0.6f else 1f)
            )
            item.descricao?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.alpha(if (isChecked) 0.5f else 0.8f)
                )
            }
        }

        // 4. Status Icon (Extrema Direita)
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(28.dp)
                .clickable { onCheckedChange(!isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Concluído",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Se estiver no horário atual, poderia mostrar um PlayArrow. Por enquanto, círculo vazio.
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Pendente",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Extensão auxiliar para garantir altura mínima da linha
private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp) = this.defaultMinSize(minHeight = height)
