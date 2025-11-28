package br.com.fabriciolima.momentus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun UpdateAvailableDialog(
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Ícone de atualização") },
        title = { Text("Atualização Disponível") },
        text = { Text("Uma nova versão do Momentus está disponível com melhorias e correções. Deseja atualizar agora?") },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text("Atualizar Agora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Depois")
            }
        }
    )
}
