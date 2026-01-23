package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.time.format.DateTimeFormatter

@Composable
fun EventListItem(
    item: ItemCronograma,
    category: Category,
    isChecked: Boolean, // For completion state
    showCheckbox: Boolean, // For selection mode
    isSelected: Boolean,   // For multiple selection state
    onCheckedChange: (Boolean) -> Unit,
    onCardClicked: () -> Unit, // For selection toggling
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val fallbackColor = MaterialTheme.colorScheme.secondary
    val categoryColor = remember(category.cor) {
        try {
            Color(android.graphics.Color.parseColor(category.cor))
        } catch (e: Exception) {
            fallbackColor
        }
    }

    val contentAlpha = if (isChecked && !showCheckbox) 0.6f else 1f
    val textDecoration = if (isChecked && !showCheckbox) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = if (showCheckbox) isSelected else isChecked,
            onCheckedChange = {
                if (showCheckbox) {
                    onCardClicked()
                } else {
                    onCheckedChange(!isChecked)
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = textDecoration,
                modifier = Modifier.alpha(contentAlpha)
            )
            Spacer(modifier = Modifier.padding(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Horário",
                    modifier = Modifier.size(16.dp).alpha(contentAlpha),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = textDecoration,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.alpha(contentAlpha)
                )
            }
        }

        Box(
            modifier = Modifier
                .background(categoryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = category.nome,
                color = categoryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
