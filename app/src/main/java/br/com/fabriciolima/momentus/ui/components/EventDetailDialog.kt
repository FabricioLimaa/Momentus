package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    showCloseButton: Boolean = false // Mantido para compatibilidade, mas ignorado no novo design
) {
    val eventColor = try {
        Color(android.graphics.Color.parseColor(category.cor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondary
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // 1. Header: Ícone Info + "Detalhes"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detalhes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        // 2. Título da Tarefa
        Text(
            text = event.titulo,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 3. Categoria com Ponto de Cor
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category.nome,
                style = MaterialTheme.typography.bodyLarge,
                color = eventColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 4. Horário com Ícone de Relógio
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${event.horarioInicio.format(timeFormatter)} - ${event.horarioTermino.format(timeFormatter)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // 5. Descrição (Se houver)
        if (!event.descricao.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = event.descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 6. Ações (Rodapé - Conforme mockup)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = onCloseClick) {
                Text(
                    text = "Fechar",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface, // Respeita modo claro/escuro
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            EventDetailContent(
                event = event,
                category = category,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onCloseClick = onDismiss
            )
        }
    }
}
