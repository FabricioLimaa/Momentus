package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// CORREÇÃO: Importando Card e outros componentes do Material 3
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.TemplateComEventos

@Composable
fun TemplateListItem(
    templateComEventos: TemplateComEventos,
    onClick: () -> Unit
) {
    // CORREÇÃO: Usando o Card do Material 3
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        // CORREÇÃO: A elevação no M3 é passada através de CardDefaults
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // CORREÇÃO: Usando a tipografia do Material 3 (h6 -> titleLarge)
            Text(
                text = templateComEventos.template.nome,
                style = MaterialTheme.typography.titleLarge
            )
            // CORREÇÃO: Usando a tipografia do Material 3 (body2 -> bodyMedium)
            Text(
                text = "${templateComEventos.eventos.size} eventos",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
