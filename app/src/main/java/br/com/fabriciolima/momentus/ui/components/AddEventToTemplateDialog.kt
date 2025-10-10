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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var selectedRotina by remember { mutableStateOf<Rotina?>(null) }
    var showDropdown by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(rotinas) {
        if (selectedRotina == null && rotinas.isNotEmpty()) {
            selectedRotina = rotinas.firstOrNull()
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Hora de Início",
            initialTime = horarioInicio,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { newTime ->
                horarioInicio = newTime
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
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())) {
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
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartTimePicker = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
                    )
                    OutlinedTextField(
                        value = horarioTermino.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Término") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndTimePicker = true },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    OutlinedTextField(
                        value = selectedRotina?.nome ?: "Selecione uma categoria",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().clickable { showDropdown = true },
                        leadingIcon = {
                            selectedRotina?.cor?.let {
                                val color = try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { Color.Gray }
                                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                            }
                        }
                    )
                     DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                        rotinas.forEach { rotina ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val color = try { Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { Color.Gray }
                                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(rotina.nome)
                                    }
                                },
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
