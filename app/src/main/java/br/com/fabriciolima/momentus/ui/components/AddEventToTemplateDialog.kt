package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventToTemplateDialog(
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalTime, LocalTime, Rotina) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var horarioInicio by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var horarioTermino by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var selectedRotina by remember { mutableStateOf(rotinas.firstOrNull()) }
    var showDropdown by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = horarioInicio.hour, initialMinute = horarioInicio.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                horarioInicio = LocalTime.of(state.hour, state.minute)
                showStartTimePicker = false
            }
        ) { TimePicker(state = state) }
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = horarioTermino.hour, initialMinute = horarioTermino.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                horarioTermino = LocalTime.of(state.hour, state.minute)
                showEndTimePicker = false
            }
        ) { TimePicker(state = state) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Adicionar Evento", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fechar") }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = horarioInicio.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Início") },
                        modifier = Modifier.weight(1f).clickable { showStartTimePicker = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
                    )
                    OutlinedTextField(
                        value = horarioTermino.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Término") },
                        modifier = Modifier.weight(1f).clickable { showEndTimePicker = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    OutlinedTextField(
                        value = selectedRotina?.nome ?: "Selecione uma categoria",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().clickable { showDropdown = true },
                        leadingIcon = {
                            selectedRotina?.cor?.let {
                                Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(it)), CircleShape))
                            }
                        }
                    )
                    DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                        rotinas.forEach { rotina ->
                            DropdownMenuItem(
                                text = { Text(rotina.nome) },
                                onClick = { 
                                    selectedRotina = rotina
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (selectedRotina != null && titulo.isNotBlank()) {
                            onConfirm(titulo, descricao, horarioInicio, horarioTermino, selectedRotina!!)
                        }
                    }) { Text("Adicionar") }
                }
            }
        }
    }
}
