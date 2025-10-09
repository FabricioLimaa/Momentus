package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateUiState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.ViewModelFactory
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TemplatesActivity : ComponentActivity() {

    private val viewModel: TemplateViewModel by viewModels { 
        ViewModelFactory(AppDatabase.getDatabase(this), application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemplatesScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: TemplateViewModel) {
    val uiState by viewModel.uiState.observeAsState(TemplateUiState())
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<TemplateComEventos?>(null) }
    var showApplyDialog by remember { mutableStateOf<TemplateComEventos?>(null) }

    if (showCreateDialog) {
        CreateTemplateDialog(
            rotinas = uiState.rotinasMap.values.toList(),
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                name, events ->
                viewModel.salvarTemplateCompleto(name, events)
                showCreateDialog = false
            }
        )
    }

    showDeleteDialog?.let { templateToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Deletar Template") },
            text = { Text("Você tem certeza que quer deletar o template \"${templateToDelete.template.nome}\"? Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(templateToDelete.template.id)
                    showDeleteDialog = null
                }) { Text("DELETAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") } }
        )
    }

    showApplyDialog?.let { templateToApply ->
        ApplyTemplateDialog(
            onDismiss = { showApplyDialog = null },
            onConfirm = { dates ->
                viewModel.applyTemplateToDates(templateToApply.template.id, dates)
                showApplyDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Minha Agenda") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Templates de Rotina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Crie rotinas e aplique em múltiplos dias",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Template")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Novo Template", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            if(uiState.templates.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top=32.dp)){
                    Text("Nenhum template criado ainda.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(uiState.templates) { templateComEventos ->
                        TemplateCard(
                            templateComEventos = templateComEventos,
                            rotinasMap = uiState.rotinasMap, // Passando o mapa de rotinas
                            onDeleteClick = { showDeleteDialog = templateComEventos },
                            onApplyClick = { showApplyDialog = templateComEventos }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCard(
    templateComEventos: TemplateComEventos, 
    rotinasMap: Map<String, Rotina>,
    onDeleteClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = templateComEventos.template.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "${templateComEventos.eventos.size} eventos", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Row {
                     IconButton(onClick = onApplyClick) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Aplicar Template", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar Template", tint = Color.Red)
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                templateComEventos.eventos.sortedBy { it.horarioInicio }.forEach { evento ->
                    rotinasMap[evento.rotinaId]?.let { rotina ->
                        TemplateEventItem(item = evento, rotina = rotina)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateEventItem(item: ItemCronograma, rotina: Rotina) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val corDaRotina = try { Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { Color.Gray }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(corDaRotina, CircleShape))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.titulo, fontWeight = FontWeight.SemiBold)
            if (item.descricao?.isNotBlank() == true) {
                Text(item.descricao, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Text(
            text = "${item.horarioInicio.format(timeFormatter)} - ${item.horarioTermino.format(timeFormatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateDialog(
    rotinas: List<Rotina>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<EventFormData>) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var eventForms by remember { mutableStateOf(listOf(EventFormData())) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Criar Template de Rotina", style = MaterialTheme.typography.titleLarge)
                Text("Defina uma rotina com múltiplos eventos", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Nome do Template") },
                    placeholder = { Text("ex: Dia de trabalho, Fim de semana") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Eventos da Rotina", style = MaterialTheme.typography.titleMedium)
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(eventForms) { index, eventData ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Evento ${index + 1}", style = MaterialTheme.typography.labelMedium)
                        EventTemplateForm(
                            eventData = eventData,
                            rotinas = rotinas,
                            onDataChange = { updatedData ->
                                eventForms = eventForms.toMutableList().also { it[index] = updatedData }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if(index < eventForms.lastIndex) {
                            Divider()
                        }
                    }
                }

                TextButton(
                    onClick = { eventForms = eventForms + EventFormData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Adicionar Evento")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = { onConfirm(templateName, eventForms) }) {
                        Text("Criar Template")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTemplateForm(
    eventData: EventFormData,
    rotinas: List<Rotina>,
    onDataChange: (EventFormData) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = eventData.horarioInicio.hour, initialMinute = eventData.horarioInicio.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                onDataChange(eventData.copy(horarioInicio = LocalTime.of(state.hour, state.minute)))
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = eventData.horarioTermino.hour, initialMinute = eventData.horarioTermino.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                onDataChange(eventData.copy(horarioTermino = LocalTime.of(state.hour, state.minute)))
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = eventData.titulo,
            onValueChange = { onDataChange(eventData.copy(titulo = it)) },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = eventData.descricao,
            onValueChange = { onDataChange(eventData.copy(descricao = it)) },
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = eventData.horarioInicio.format(timeFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Início") },
                modifier = Modifier.weight(1f).clickable { showStartTimePicker = true },
                trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
            )
            OutlinedTextField(
                value = eventData.horarioTermino.format(timeFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Término") },
                modifier = Modifier.weight(1f).clickable { showEndTimePicker = true },
                trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
            )
        }

        Box {
            OutlinedTextField(
                value = eventData.selectedRotina?.nome ?: "Selecione uma categoria",
                onValueChange = { },
                readOnly = true,
                label = { Text("Categoria") },
                modifier = Modifier.fillMaxWidth().clickable { showDropdown = true }
            )
            DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }, modifier = Modifier.fillMaxWidth()) {
                rotinas.forEach { rotina ->
                    DropdownMenuItem(
                        text = { Text(rotina.nome) },
                        onClick = {
                            onDataChange(eventData.copy(selectedRotina = rotina))
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}
