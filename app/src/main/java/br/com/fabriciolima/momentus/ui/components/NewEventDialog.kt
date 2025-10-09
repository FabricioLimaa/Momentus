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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.model.Rotina
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventDialog(
    selectedDate: LocalDate,
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalDate, LocalTime, LocalTime, Rotina, Boolean) -> Unit
) {
    val agora = LocalTime.now()
    val horaInicialSugerida = agora.withMinute(0).plusHours(1)

    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var horarioInicio by remember { mutableStateOf(horaInicialSugerida) }
    var horarioTermino by remember { mutableStateOf(horaInicialSugerida.plusHours(1)) }
    var selectedRotina by remember { mutableStateOf<Rotina?>(null) }
    var salvarNoGoogle by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Efeito para garantir que a rotina selecionada seja atualizada quando a lista mudar
    LaunchedEffect(rotinas) {
        if (selectedRotina == null || rotinas.find { it.id == selectedRotina?.id } == null) {
            selectedRotina = rotinas.firstOrNull()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- Seletor de Hora de Início ---
                if (showStartTimePicker) {
                    val state = rememberTimePickerState(initialHour = horarioInicio.hour, initialMinute = horarioInicio.minute, is24Hour = true)
                    Text("Hora de Início", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = state, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showStartTimePicker = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            horarioInicio = LocalTime.of(state.hour, state.minute)
                            showStartTimePicker = false
                        }) { Text("OK") }
                    }
                // --- Seletor de Hora de Término ---
                } else if (showEndTimePicker) {
                    val state = rememberTimePickerState(initialHour = horarioTermino.hour, initialMinute = horarioTermino.minute, is24Hour = true)
                    Text("Hora de Término", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = state, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEndTimePicker = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            horarioTermino = LocalTime.of(state.hour, state.minute)
                            showEndTimePicker = false
                        }) { Text("OK") }
                    }
                // --- Formulário Principal ---
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Novo Evento", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                    Text("Para ${selectedDate.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"))}")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- SELETOR DE CATEGORIA COM DROPDOWN ---
                    Box {
                        OutlinedTextField(
                            value = selectedRotina?.nome ?: "Selecione uma categoria",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                selectedRotina?.cor?.let {
                                    Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(it)), CircleShape))
                                }
                            }
                        )
                        // Box clicável para abrir o menu
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDropdown = true }
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

                    // --- BOTÕES DE AÇÃO ---
                    Button(
                        onClick = {
                            if (selectedRotina != null && titulo.isNotBlank()) {
                                onConfirm(
                                    titulo,
                                    descricao,
                                    selectedDate,
                                    horarioInicio,
                                    horarioTermino,
                                    selectedRotina!!,
                                    salvarNoGoogle
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Criar Evento")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
