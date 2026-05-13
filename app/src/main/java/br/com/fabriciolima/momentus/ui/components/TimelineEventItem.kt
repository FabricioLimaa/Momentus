package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.combinedClickable

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TimelineEventItem(
    item: ItemCronograma,
    category: Category,
    isChecked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val categoryColor = remember(category.cor) {
        try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { Color.Gray }
    }

    // Lógica para determinar o status dinâmico (Passo 1 do plano)
    val now = LocalTime.now()
    val isOngoing = remember(item.horarioInicio, item.horarioTermino, now) {
        now.isAfter(item.horarioInicio) && now.isBefore(item.horarioTermino)
    }

    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. Horário (Esquerda)
        Text(
            text = item.horarioInicio.format(formatter),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp).padding(top = 14.dp)
        )

        // 2. Indicador Vertical (Timeline)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp).height(IntrinsicSize.Min)
        ) {
            // Linha superior
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(
                        if (isFirst) Color.Transparent 
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
            )

            // Ponto colorido
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            // Linha inferior
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .minHeight(30.dp)
                    .background(
                        if (isLast) Color.Transparent 
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
            )
        }

        // 3. Conteúdo (Título)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant 
                        else if (isOngoing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(if (isChecked) 0.6f else 1f)
            )
            item.descricao?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // 4. Status Icon Dinâmico (Lógica conforme Mockup)
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(24.dp)
                .clickable { onCheckedChange(!isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            } else {
                when {
                    isChecked -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Concluído",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isOngoing -> {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Acontecendo agora",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp) = this.defaultMinSize(minHeight = height)
