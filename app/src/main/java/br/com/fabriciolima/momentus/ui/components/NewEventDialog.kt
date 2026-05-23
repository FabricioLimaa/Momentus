package br.com.fabriciolima.momentus.ui.components

import android.util.Log
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.ui.theme.EmeraldGreen
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.fabriciolima.momentus.ui.theme.DeepNavyBackground
import br.com.fabriciolima.momentus.ui.theme.DeepNavySurface
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "NewEventDialog"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewEventContent(
    eventoParaEditar: ItemCronograma? = null,
    selectedDate: LocalDate,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (ItemCronograma?, String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit
) {
    val isEditMode = eventoParaEditar != null
    val focusManager = LocalFocusManager.current

    var titulo by remember { mutableStateOf(eventoParaEditar?.titulo ?: "") }
    var descricao by remember { mutableStateOf(eventoParaEditar?.descricao ?: "") }
    var dataSelecionada by remember { mutableStateOf(eventoParaEditar?.data?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: selectedDate) }
    var horarioInicio by remember { mutableStateOf(eventoParaEditar?.horarioInicio ?: LocalTime.now().withMinute(0).withSecond(0)) }
    var horarioTermino by remember { mutableStateOf(eventoParaEditar?.horarioTermino ?: LocalTime.now().withMinute(0).withSecond(0).plusHours(1)) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var salvarNoGoogle by remember { mutableStateOf(eventoParaEditar?.googleCalendarEventId != null) }

    val isTimeInvalid by remember { derivedStateOf { horarioTermino.isBefore(horarioInicio) || horarioTermino == horarioInicio } }
    val isFormValid by remember { derivedStateOf { titulo.isNotBlank() && selectedCategory != null && !isTimeInvalid } }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    LaunchedEffect(eventoParaEditar) {
        if (!isEditMode) {
            horarioInicio = LocalTime.now().withSecond(0).withNano(0)
            horarioTermino = horarioInicio.plusHours(1)
        }
    }

    LaunchedEffect(categories, eventoParaEditar) {
        if (categories.isNotEmpty()) {
            selectedCategory = if (isEditMode) {
                categories.find { it.id == eventoParaEditar?.categoryId }
            } else {
                categories.find { it.nome.equals("Outros", ignoreCase = true) } ?: categories.firstOrNull()
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dataSelecionada.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dataSelecionada = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isEditMode) "Editar Rotina" else "Nova Rotina", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("Nome da rotina", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            placeholder = { Text("Ex: Estudo de Redes") },
            isError = titulo.isBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        Text("Horário", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = horarioInicio.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Início") },
                    isError = isTimeInvalid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                )
                Box(modifier = Modifier.matchParentSize().clickable { showStartTimePicker = true })
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = horarioTermino.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fim") },
                    isError = isTimeInvalid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                )
                Box(modifier = Modifier.matchParentSize().clickable { showEndTimePicker = true })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Categoria", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory?.id == category.id
                val categoryColor = try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { Color.Gray }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category.nome) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = categoryColor.copy(alpha = 0.2f),
                        selectedLabelColor = categoryColor,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLeadingIconColor = categoryColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        selectedBorderColor = categoryColor,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Descrição (opcional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            placeholder = { Text("Adicione uma descrição...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sincronizar com Google Agenda", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = salvarNoGoogle,
                onCheckedChange = { salvarNoGoogle = it },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isFormValid) {
                    onConfirm(eventoParaEditar, titulo, descricao, dataSelecionada, horarioInicio, horarioTermino, selectedCategory!!, salvarNoGoogle)
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text(if (isEditMode) "Salvar Alterações" else "Salvar Rotina", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun NewEventDialog(
    eventoParaEditar: ItemCronograma? = null,
    selectedDate: LocalDate,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (ItemCronograma?, String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Alterado de DeepNavyBackground para ser dinâmico
        ) {
            NewEventContent(
                eventoParaEditar = eventoParaEditar,
                selectedDate = selectedDate,
                categories = categories,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
    }
}
