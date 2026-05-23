package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.ui.util.getIconForMarketItem

@Composable
fun CategoryListItem(item: CategoryWithMeta, onEdit: (CategoryWithMeta) -> Unit, onDelete: (CategoryWithMeta) -> Unit) {
    val meta = item.meta?.metaMinutosSemanal ?: 0
    val categoryColor = remember(item.category.cor) {
        try {
            Color(android.graphics.Color.parseColor(item.category.cor))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ADICIONADO: Ícone circular com a cor da categoria e sticker (se houver)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(categoryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val stickerIcon = getIconForMarketItem(item.category.stickerId)
                        if (stickerIcon != null) {
                            Icon(
                                imageVector = stickerIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(item.category.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Categoria", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar Categoria", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (meta > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Meta Semanal: ${meta / 60}h ${meta % 60}m", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth()) // TODO: Calcular progresso real
            }
        }
    }
}
