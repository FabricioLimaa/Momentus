package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateUiState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class TemplatesActivity : ComponentActivity() {

    private val viewModel: TemplateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                TemplatesScreen(viewModel = viewModel, onNavigateUp = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: TemplateViewModel, onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<TemplateComEventos?>(null) }
    var showApplyDialog by remember { mutableStateOf<TemplateComEventos?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    AnimatedVisibility(visible = showCreateDialog, enter = fadeIn(), exit = fadeOut()) {
        CreateTemplateDialog(
            rotinas = uiState.rotinasMap.values.toList(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, events ->
                viewModel.salvarTemplateCompleto(name, events) { result ->
                    when (result) {
                        is Result.Success -> {
                            showCreateDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Template salvo com sucesso!")
                            }
                        }
                        is Result.Error -> {
                            Toast.makeText(context, result.exception.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    AnimatedVisibility(visible = showDeleteDialog != null, enter = fadeIn(), exit = fadeOut()) {
        showDeleteDialog?.let { templateToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso") },
                title = { Text("Deletar Template") },
                text = { Text("Você tem certeza que quer deletar o template \"${templateToDelete.template.nome}\"? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteTemplate(templateToDelete.template.id)
                        showDeleteDialog = null
                    }) { 
                        Icon(Icons.Default.Delete, contentDescription = "Deletar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETAR") 
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") } }
            )
        }
    }

    AnimatedVisibility(visible = showApplyDialog != null, enter = fadeIn(), exit = fadeOut()) {
        showApplyDialog?.let { templateToApply ->
            ApplyTemplateDialog(
                onDismiss = { showApplyDialog = null },
                onConfirm = { dates ->
                    viewModel.applyTemplateToDates(templateToApply.template.id, dates)
                    showApplyDialog = null
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meus Templates") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Voltar") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Templates de Rotina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Crie rotinas e aplique em múltiplos dias",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            if (uiState.templates.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhum template", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Crie seu primeiro template para reutilizar rotinas facilmente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(uiState.templates) { templateComEventos ->
                        TemplateCard(
                            templateComEventos = templateComEventos,
                            rotinasMap = uiState.rotinasMap,
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
                    Text(text = "${templateComEventos.eventos.size} eventos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                     IconButton(onClick = onApplyClick) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Aplicar Template", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar Template", tint = MaterialTheme.colorScheme.error)
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
    val corDaRotina = try { Color(android.graphics.Color.parseColor(rotina.cor)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(corDaRotina, CircleShape))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.titulo, fontWeight = FontWeight.SemiBold)
            if (item.descricao?.isNotBlank() == true) {
                Text(item.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = "${item.horarioInicio.format(timeFormatter)} - ${item.horarioTermino.format(timeFormatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
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
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Evento ao Template")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar Evento")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { 
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar") 
                    }
                    Button(onClick = { onConfirm(templateName, eventForms) }) {
                        Icon(Icons.Default.Check, contentDescription = "Criar Template")
                        Spacer(modifier = Modifier.width(4.dp))
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
        TimePickerDialog(
            title = "Hora de Início",
            initialTime = eventData.horarioInicio,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { onDataChange(eventData.copy(horarioInicio = it)) }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Hora de Término",
            initialTime = eventData.horarioTermino,
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { onDataChange(eventData.copy(horarioTermino = it)) }
        )
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
                modifier = Modifier.fillMaxWidth().clickable { showDropdown = true },
                leadingIcon = {
                    eventData.selectedRotina?.cor?.let {
                        val color = try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { Color.Gray }
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    }
                }
            )
            DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }, modifier = Modifier.fillMaxWidth()) {
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
                            onDataChange(eventData.copy(selectedRotina = rotina))
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventListItem(item: ItemCronograma, rotina: Rotina, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        // ... (o restante do código permanece o mesmo)
    }
}
