// ARQUIVO: ui/components/RotinaListItem.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.RotinaComMeta

@Composable
fun RotinaListItem(
    rotinaComMeta: RotinaComMeta,
    onItemClicked: (RotinaComMeta) -> Unit
) {
    val rotina = rotinaComMeta.rotina
    val cor = try { Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { Color.Gray }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClicked(rotinaComMeta) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(cor, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rotina.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!rotina.descricao.isNullOrBlank()) {
                    Text(rotina.descricao, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Exibe a meta se ela existir
                rotinaComMeta.meta?.let {
                    if (it.metaMinutosSemanal > 0) {
                        Text(
                            "Meta: ${it.metaMinutosSemanal / 60}h/semana",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            // Exibe a tag se ela existir
            if (!rotina.tag.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = rotina.tag,
                    color = cor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}