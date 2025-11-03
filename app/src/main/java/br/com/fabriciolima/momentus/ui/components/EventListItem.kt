package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.time.format.DateTimeFormatter

@Composable
fun EventListItem(
    item: ItemCronograma,
    category: Category,
    modifier: Modifier = Modifier,
    isChecked: Boolean = false,
    isEnabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null // Parâmetro opcional
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val categoryColor = Color(android.graphics.Color.parseColor(category.cor))
    
    val contentAlpha = if (isChecked && onCheckedChange != null) 0.6f else 1f
    val textDecoration = if (isChecked && onCheckedChange != null) TextDecoration.LineThrough else TextDecoration.None

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .alpha(contentAlpha)
        ) {
            val (checkbox, contentColumn, categoryTag) = createRefs()

            if (onCheckedChange != null) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    enabled = isEnabled,
                    modifier = Modifier.constrainAs(checkbox) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(categoryColor, shape = CircleShape)
                        .constrainAs(checkbox) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                )
            }

            Column(modifier = Modifier.constrainAs(contentColumn) {
                start.linkTo(checkbox.end, margin = 16.dp)
                top.linkTo(parent.top)
            }) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = textDecoration
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Horário",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = textDecoration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.descricao?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.descricao,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = textDecoration
                    )
                }
            }

            Box(
                modifier = Modifier
                    .constrainAs(categoryTag) {
                        end.linkTo(parent.end)
                        top.linkTo(checkbox.top)
                        bottom.linkTo(checkbox.bottom)
                    }
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
}
