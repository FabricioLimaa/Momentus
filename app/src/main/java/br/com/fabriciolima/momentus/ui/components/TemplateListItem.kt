// ARQUIVO: ui/components/TemplateListItem.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.data.TemplateComItens

@Composable
fun TemplateListItem(
    templateComItens: TemplateComItens,
    onApply: (Template) -> Unit,
    onDelete: (Template) -> Unit
) {
    val template = templateComItens.template

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho com Nome e Botões
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(template.nome, style = MaterialTheme.typography.titleLarge)
                    Text("${templateComItens.itens.size} eventos", style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    OutlinedButton(onClick = { onApply(template) }) {
                        Icon(painterResource(id = R.drawable.ic_template), contentDescription = "Aplicar", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplicar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDelete(template) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar")
                    }
                }
            }

            // Divisor e prévia dos eventos
            if (templateComItens.itens.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mostra uma prévia de até 3 eventos
                    templateComItens.itens.take(3).forEach { item ->
                        val rotina = templateComItens.rotinas.find { it.id == item.rotinaId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val cor = try { Color(android.graphics.Color.parseColor(rotina?.cor)) } catch (e: Exception) { Color.Gray }
                            Box(modifier = Modifier.size(8.dp).background(cor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(rotina?.nome ?: "Desconhecido", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(item.horarioInicio, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}