package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta

@Composable
fun CategoryListItem(item: CategoryWithMeta, onEdit: (CategoryWithMeta) -> Unit, onDelete: (CategoryWithMeta) -> Unit) {
    val meta = item.meta?.metaMinutosSemanal ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                LinearProgressIndicator(progress = 0.5f, modifier = Modifier.fillMaxWidth()) // TODO: Calcular progresso real
            }
        }
    }
}
