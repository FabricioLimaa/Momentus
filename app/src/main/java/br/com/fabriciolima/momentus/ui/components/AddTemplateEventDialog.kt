// ARQUIVO: ui/components/AddTemplateEventDialog.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.Rotina
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateEventDialog(
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (titulo: String, desc: String, dia: String, inicio: LocalTime, fim: LocalTime, categoria: Rotina) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var horarioInicio by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var horarioTermino by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var categoriaSelecionada by remember { mutableStateOf(rotinas.firstOrNull()) }

    val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
    var diaSelecionado by remember { mutableStateOf(diasDaSemana.first()) }

    // ... (Lógica e Composable do TimePickerDialog, como já criamos antes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Evento ao Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título do Evento") })

                // Seletor para o Dia da Semana
                var diaExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = diaExpanded, onExpandedChange = { diaExpanded = !diaExpanded }) {
                    OutlinedTextField(
                        value = diaSelecionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dia da Semana") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = diaExpanded, onDismissRequest = { diaExpanded = false }) {
                        diasDaSemana.forEach { dia ->
                            DropdownMenuItem(text = { Text(dia) }, onClick = { diaSelecionado = dia; diaExpanded = false })
                        }
                    }
                }

                // Seletores de Horário (simplificado)
                // TODO: Adicionar TimePickers como no NewEventDialog
                OutlinedTextField(value = horarioInicio.toString(), onValueChange = { /*TODO*/ }, label = { Text("Início (HH:mm)") })
                OutlinedTextField(value = horarioTermino.toString(), onValueChange = { /*TODO*/ }, label = { Text("Término (HH:mm)") })

                // Seletor de Categoria
                CategorySelector(rotinas = rotinas, selecionada = categoriaSelecionada, onSelected = { categoriaSelecionada = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                categoriaSelecionada?.let {
                    onConfirm(titulo, descricao, diaSelecionado, horarioInicio, horarioTermino, it)
                }
            }) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}