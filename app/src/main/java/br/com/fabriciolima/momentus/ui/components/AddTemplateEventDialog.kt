package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.model.Rotina
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateEventDialog(
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime, Rotina) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var horarioInicioStr by remember { mutableStateOf("09:00") }
    var horarioTerminoStr by remember { mutableStateOf("10:00") }

    val daysOfWeek = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    var dayOfWeekExpanded by remember { mutableStateOf(false) }
    var selectedDayOfWeek by remember { mutableStateOf(daysOfWeek.first()) }

    var rotinaExpanded by remember { mutableStateOf(false) }
    var selectedRotina by remember { mutableStateOf(rotinas.firstOrNull()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Adicionar Evento ao Template", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição (Opcional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = dayOfWeekExpanded, onExpandedChange = { dayOfWeekExpanded = !dayOfWeekExpanded }) {
                    OutlinedTextField(value = selectedDayOfWeek, onValueChange = {}, readOnly = true, label = { Text("Dia da Semana") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayOfWeekExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = dayOfWeekExpanded, onDismissRequest = { dayOfWeekExpanded = false }) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(text = { Text(day) }, onClick = { selectedDayOfWeek = day; dayOfWeekExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = horarioInicioStr, onValueChange = { horarioInicioStr = it }, label = { Text("Início") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = horarioTerminoStr, onValueChange = { horarioTerminoStr = it }, label = { Text("Término") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (rotinas.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = rotinaExpanded, onExpandedChange = { rotinaExpanded = !rotinaExpanded }) {
                        OutlinedTextField(value = selectedRotina?.nome ?: "Selecione", onValueChange = {}, readOnly = true, label = { Text("Categoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rotinaExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = rotinaExpanded, onDismissRequest = { rotinaExpanded = false }) {
                            rotinas.forEach { rotina ->
                                DropdownMenuItem(text = { Text(rotina.nome) }, onClick = { selectedRotina = rotina; rotinaExpanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val inicio = LocalTime.parse(horarioInicioStr)
                                val fim = LocalTime.parse(horarioTerminoStr)
                                if (selectedRotina != null && titulo.isNotBlank()) {
                                    onConfirm(titulo, descricao, selectedDayOfWeek, inicio, fim, selectedRotina!!)
                                }
                            } catch (e: DateTimeParseException) {
                                // Opcional: Mostrar um Toast ou erro para o usuário sobre o formato da hora
                            }
                        },
                        enabled = selectedRotina != null && titulo.isNotBlank()
                    ) { Text("Adicionar") }
                }
            }
        }
    }
}
