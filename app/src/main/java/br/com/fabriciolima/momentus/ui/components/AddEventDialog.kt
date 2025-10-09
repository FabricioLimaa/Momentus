package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.model.Rotina
import java.time.format.DateTimeFormatter

/**
 * Diálogo genérico para adicionar um novo evento.
 * Pode ser usado tanto para eventos únicos quanto para eventos de template.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String, String, Rotina) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var horarioInicio by remember { mutableStateOf("09:00") }
    var horarioTermino by remember { mutableStateOf("10:00") }
    var expanded by remember { mutableStateOf(false) }
    var selectedRotina by remember { mutableStateOf(rotinas.firstOrNull()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Adicionar Evento", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.height(100.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = horarioInicio, onValueChange = { horarioInicio = it }, label = { Text("Início") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = horarioTermino, onValueChange = { horarioTermino = it }, label = { Text("Término") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedRotina?.nome ?: "Selecione",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rotinas.forEach { rotina ->
                            DropdownMenuItem(
                                text = { Text(rotina.nome) },
                                onClick = { 
                                    selectedRotina = rotina
                                    expanded = false 
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (selectedRotina != null) {
                            onConfirm(titulo, descricao, horarioInicio, horarioTermino, selectedRotina!!)
                            onDismiss()
                        }
                    }) { Text("Adicionar") }
                }
            }
        }
    }
}
