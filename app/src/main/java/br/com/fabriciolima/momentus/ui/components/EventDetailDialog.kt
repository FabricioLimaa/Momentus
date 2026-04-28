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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.time.format.DateTimeFormatter

@Composable
fun EventDetailContent(
    event: ItemCronograma,
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCloseClick: () -> Unit,
    showCloseButton: Boolean = false
) {
    val eventColor = try {
        Color(android.graphics.Color.parseColor(category.cor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondary
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Detalhes", style = MaterialTheme.typography.titleMedium)
            }
            if (showCloseButton) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = event.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(12.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(category.nome, style = MaterialTheme.typography.bodyLarge, color = eventColor, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${event.horarioInicio.format(timeFormatter)} - ${event.horarioTermino.format(timeFormatter)}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (!event.descricao.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Descrição", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(event.descricao, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
            if (!showCloseButton) {
                TextButton(onClick = onCloseClick) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: ItemCronograma,
    category: Category,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            EventDetailContent(
                event = event,
                category = category,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onCloseClick = onDismiss
            )
        }
    )
}
