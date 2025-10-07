package br.com.fabriciolima.momentus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.TemplateComEventos
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import br.com.fabriciolima.momentus.viewmodel.TemplateUiState
import br.com.fabriciolima.momentus.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.viewmodel.ViewModelFactory
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class EventFormData(
    val id: UUID = UUID.randomUUID(),
    var titulo: String = "",
    var descricao: String = "",
    var selectedRotina: Rotina? = null,
    var horarioInicio: LocalTime = LocalTime.of(9, 0),
    var horarioTermino: LocalTime = LocalTime.of(10, 0)
)

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

@Composable
fun TemplatesScreen(viewModel: TemplateViewModel) {
    val uiState by viewModel.uiState.observeAsState(TemplateUiState())
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<TemplateComEventos?>(null) }
    var showApplyDialog by remember { mutableStateOf<TemplateComEventos?>(null) }
    val context = LocalContext.current

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
                }) { Text("Deletar") }
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

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Templates de Rotina", style = MaterialTheme.typography.headlineMedium)
        Text("Crie rotinas e aplique em múltiplos dias", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Novo Template")
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(uiState.templates) { templateComEventos ->
                TemplateCard(
                    templateComEventos = templateComEventos,
                    rotinasMap = uiState.rotinasMap,
                    onClick = { 
                        val intent = Intent(context, TemplateDetailActivity::class.java).apply {
                            putExtra("TEMPLATE_ID", templateComEventos.template.id)
                        }
                        context.startActivity(intent)
                    },
                    onDeleteClick = { showDeleteDialog = templateComEventos },
                    onApplyClick = { showApplyDialog = templateComEventos }
                )
            }
        }
    }
}

@Composable
fun TemplateCard(
    templateComEventos: TemplateComEventos, 
    rotinasMap: Map<String, Rotina>,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = templateComEventos.template.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "${templateComEventos.eventos.size} eventos", style = MaterialTheme.typography.bodyMedium)
            
            Row {
                TextButton(onClick = onApplyClick) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Aplicar Template", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Aplicar")
                }
                TextButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Deletar Template", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Deletar")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                templateComEventos.eventos.sortedBy { it.horarioInicio }.forEach { evento ->
                    rotinasMap[evento.rotinaId]?.let {
                        TemplateEventItem(item = evento)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateEventItem(item: ItemCronograma) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.titulo, fontWeight = FontWeight.SemiBold)
            if (item.descricao?.isNotBlank() == true) {
                Text(item.descricao, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            text = "${item.horarioInicio.format(timeFormatter)} - ${item.horarioTermino.format(timeFormatter)}",
            style = MaterialTheme.typography.bodySmall
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
        Card(modifier = Modifier.padding(vertical = 32.dp)) {
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
