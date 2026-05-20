package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.ui.theme.*
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
import androidx.compose.foundation.shape.RoundedCornerShape

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

    // Lógica para determinar o status dinâmico
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
            .padding(horizontal = 20.dp, vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. Horário (Mockup: Texto menor e cinza)
        Text(
            text = item.horarioInicio.format(formatter),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp).padding(top = 14.dp)
        )

        // 2. Timeline Vertical Conectada (Mockup: Linhas finas e pontos coloridos)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp).height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(if (isFirst) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
            )

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .minHeight(30.dp)
                    .background(if (isLast) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }

        // 3. Card de Conteúdo (Mockup: Título em destaque e categoria em chip)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 10.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Chip de Categoria Minimalista
                Surface(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = category.nome,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )
        }

        // 4. Status Icon (Check) ou Checkbox de seleção
        Box(
            modifier = Modifier
                .padding(top = 12.dp, start = 8.dp)
                .size(24.dp)
                .clickable { if (!isSelectionMode) onCheckedChange(!isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            } else if (isChecked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.dp, TextSecondaryDark.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}


private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp) = this.defaultMinSize(minHeight = height)
