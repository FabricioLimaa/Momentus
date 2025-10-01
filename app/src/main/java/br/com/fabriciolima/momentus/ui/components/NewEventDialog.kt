// ARQUIVO: ui/components/NewEventDialog.kt (CÓDIGO COMPLETO E CORRIGIDO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import br.com.fabriciolima.momentus.data.Rotina
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventDialog(
    dataSelecionada: LocalDate,
    todasAsRotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (titulo: String, desc: String, data: LocalDate, inicio: LocalTime, fim: LocalTime, categoria: Rotina) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(dataSelecionada) }
    var horarioInicio by remember { mutableStateOf(LocalTime.now().withMinute(0).withSecond(0)) }
    var horarioTermino by remember { mutableStateOf(horarioInicio.plusHours(1)) }
    var rotinaSelecionada by remember { mutableStateOf(todasAsRotinas.firstOrNull()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerTermino by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // --- Diálogo de Seleção de Data ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        data = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Diálogo de Seleção de Hora (Início) ---
    if (showTimePickerInicio) {
        // CORREÇÃO: Criamos o 'state' aqui, como 'remember'.
        val timePickerState = rememberTimePickerState(initialHour = horarioInicio.hour, initialMinute = horarioInicio.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTimePickerInicio = false },
            onConfirm = {
                // CORREÇÃO: Usamos o 'state' para pegar a hora e o minuto selecionados.
                horarioInicio = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showTimePickerInicio = false
            },
            state = timePickerState // Passamos o estado para o diálogo
        )
    }

    // --- Diálogo de Seleção de Hora (Término) ---
    if (showTimePickerTermino) {
        val timePickerState = rememberTimePickerState(initialHour = horarioTermino.hour, initialMinute = horarioTermino.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTimePickerTermino = false },
            onConfirm = {
                horarioTermino = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showTimePickerTermino = false
            },
            state = timePickerState
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = data.format(dateFormatter), onValueChange = {}, readOnly = true,
                    label = { Text("Data") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Selecionar Data") }
                )
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = horarioInicio.format(timeFormatter), onValueChange = {}, readOnly = true,
                        label = { Text("Início") }, modifier = Modifier.weight(1f).clickable { showTimePickerInicio = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
                    )
                    OutlinedTextField(
                        value = horarioTermino.format(timeFormatter), onValueChange = {}, readOnly = true,
                        label = { Text("Término") }, modifier = Modifier.weight(1f).clickable { showTimePickerTermino = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
                    )
                }
                CategorySelector(
                    rotinas = todasAsRotinas,
                    selecionada = rotinaSelecionada,
                    onSelected = { rotinaSelecionada = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                // Validação para garantir que uma categoria foi selecionada
                rotinaSelecionada?.let { categoria ->
                    onConfirm(titulo, descricao, data, horarioInicio, horarioTermino, categoria)
                }
            }) { Text("Criar Evento") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    rotinas: List<Rotina>,
    selecionada: Rotina?,
    onSelected: (Rotina) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selecionada?.nome ?: "Selecione a Categoria",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rotinas.forEach { rotina ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val cor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(12.dp).background(cor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(rotina.nome)
                        }
                    },
                    onClick = {
                        onSelected(rotina)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    state: TimePickerState
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Selecionar Horário", modifier = Modifier.padding(start = 24.dp, top = 24.dp)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onConfirm) { Text("OK") }
            }
        }
    )
}