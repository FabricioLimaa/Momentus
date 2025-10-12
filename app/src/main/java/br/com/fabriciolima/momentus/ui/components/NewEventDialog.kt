package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventDialog(
    eventoParaEditar: ItemCronograma? = null,
    selectedDate: LocalDate,
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (ItemCronograma?, String, String?, LocalDate, LocalTime, LocalTime, Rotina, Boolean) -> Unit
) {
    val isEditMode = eventoParaEditar != null
    val focusManager = LocalFocusManager.current

    var titulo by remember { mutableStateOf(eventoParaEditar?.titulo ?: "") }
    var descricao by remember { mutableStateOf(eventoParaEditar?.descricao ?: "") }
    var dataSelecionada by remember { mutableStateOf(eventoParaEditar?.data?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: selectedDate) }
    var horarioInicio by remember { mutableStateOf(eventoParaEditar?.horarioInicio ?: LocalTime.now().withMinute(0).withSecond(0)) }
    var horarioTermino by remember { mutableStateOf(eventoParaEditar?.horarioTermino ?: LocalTime.now().withMinute(0).withSecond(0).plusHours(1)) }
    var selectedRotina by remember { mutableStateOf<Rotina?>(null) }
    var salvarNoGoogle by remember { mutableStateOf(eventoParaEditar?.googleCalendarEventId != null) }

    val isTimeInvalid by remember { derivedStateOf { horarioTermino.isBefore(horarioInicio) || horarioTermino == horarioInicio } }
    val isFormValid by remember { derivedStateOf { titulo.isNotBlank() && selectedRotina != null && !isTimeInvalid } }

    var showDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    LaunchedEffect(rotinas, eventoParaEditar) {
        if (rotinas.isNotEmpty()) {
            selectedRotina = if (isEditMode) {
                rotinas.find { it.id == eventoParaEditar?.rotinaId }
            } else {
                rotinas.find { it.nome.equals("Outros", ignoreCase = true) } ?: rotinas.firstOrNull()
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dataSelecionada.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dataSelecionada = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Hora de Início",
            initialTime = horarioInicio,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { newTime ->
                horarioInicio = newTime
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Hora de Término",
            initialTime = horarioTermino,
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { newTime ->
                horarioTermino = newTime
                showEndTimePicker = false
            }
        )
    }

    Dialog(onDismissRequest = {
        focusManager.clearFocus()
        onDismiss()
    }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isEditMode) "Editar Evento" else "Novo Evento", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    isError = titulo.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = dataSelecionada.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar Data") }
                    )
                     Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = horarioInicio.format(timeFormatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Início") },
                            isError = isTimeInvalid,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartTimePicker = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = horarioTermino.format(timeFormatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Término") },
                            isError = isTimeInvalid,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showEndTimePicker = true }
                        )
                    }
                }
                if (isTimeInvalid) {
                    Text(
                        text = "O horário de término deve ser depois do início",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !showDropdown },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedRotina?.nome ?: "",
                        onValueChange = {},
                        label = { Text("Categoria") },
                        isError = selectedRotina == null,
                        leadingIcon = {
                            selectedRotina?.cor?.let {
                                val color = try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { Color.Gray }
                                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                    )
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                    ) {
                        rotinas.forEach { rotina ->
                            val isSelected = rotina == selectedRotina
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.width(24.dp)) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selecionado",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        val color = try { Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { Color.Gray }
                                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(rotina.nome)
                                    }
                                },
                                onClick = {
                                    selectedRotina = rotina
                                    showDropdown = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Salvar na Agenda do Google")
                    Switch(
                        checked = salvarNoGoogle,
                        onCheckedChange = { salvarNoGoogle = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isFormValid) {
                            onConfirm(
                                eventoParaEditar,
                                titulo,
                                descricao,
                                dataSelecionada,
                                horarioInicio,
                                horarioTermino,
                                selectedRotina!!,
                                salvarNoGoogle
                            )
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isEditMode) "Salvar Alterações" else "Criar Evento")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar")
                }
            }
        }
    }
}
