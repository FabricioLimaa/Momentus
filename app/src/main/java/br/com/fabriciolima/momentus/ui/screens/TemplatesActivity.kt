package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.theme.TimePickerDialog
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateUiState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class TemplatesActivity : ComponentActivity() {

    private val viewModel: TemplateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                TemplatesScreen(
                    uiState = uiState,
                    onNavigateUp = { finish() },
                    onShowCreateDialog = viewModel::onShowCreateDialog,
                    onShowImportDialog = viewModel::onShowImportDialog, 
                    onShowEditDialog = viewModel::onShowEditDialog,
                    onShowDeleteDialog = viewModel::onShowDeleteDialog,
                    onShowApplyDialog = viewModel::onShowApplyDialog,
                    onDialogDismiss = viewModel::onDialogDismiss,
                    onSaveTemplate = viewModel::salvarTemplateCompleto,
                    onImportTemplate = viewModel::importTemplateFromJson, 
                    onDeleteTemplate = viewModel::deleteTemplate,
                    onApplyTemplate = viewModel::applyTemplateToDates,
                    getShareableJson = viewModel::getShareableJsonForTemplate, // Adicionado
                    onErrorShown = viewModel::onErrorShown
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    uiState: TemplateUiState,
    onNavigateUp: () -> Unit,
    onShowCreateDialog: () -> Unit,
    onShowImportDialog: () -> Unit,
    onShowEditDialog: (TemplateComEventos) -> Unit,
    onShowDeleteDialog: (TemplateComEventos) -> Unit,
    onShowApplyDialog: (TemplateComEventos) -> Unit,
    onDialogDismiss: () -> Unit,
    onSaveTemplate: (String?, String, List<EventFormData>, (Result<Unit>) -> Unit) -> Unit,
    onImportTemplate: (String, (Result<Unit>) -> Unit) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onApplyTemplate: (String, List<LocalDate>, Boolean, (Result<Unit>) -> Unit) -> Unit,
    getShareableJson: (String) -> String?, // Adicionado
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    when (val dialogState = uiState.dialogState) {
        is TemplateDialogState.CreateNew -> {
            CreateTemplateDialog(
                categories = uiState.categoriesMap.values.toList(),
                onDismiss = onDialogDismiss,
                onConfirm = { id, name, events ->
                    onSaveTemplate(id, name, events) { result ->
                        when (result) {
                            is Result.Success -> {
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
        is TemplateDialogState.Import -> { 
            ImportTemplateDialog(
                onDismiss = onDialogDismiss,
                onConfirm = { jsonString ->
                    onImportTemplate(jsonString) { result ->
                        when (result) {
                            is Result.Success -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Template importado com sucesso!")
                                }
                            }
                            is Result.Error -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(result.exception.message ?: "Erro desconhecido")
                                }
                            }
                        }
                    }
                }
            )
        }
        is TemplateDialogState.Edit -> {
            CreateTemplateDialog(
                templateToEdit = dialogState.template,
                categories = uiState.categoriesMap.values.toList(),
                onDismiss = onDialogDismiss,
                onConfirm = { id, name, events ->
                    onSaveTemplate(id, name, events) { result ->
                        when (result) {
                            is Result.Success -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Template atualizado com sucesso!")
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
        is TemplateDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = onDialogDismiss,
                icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso") },
                title = { Text("Deletar Template") },
                text = { Text("Você tem certeza que quer deletar o template \"${dialogState.template.template.nome}\"? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = { onDeleteTemplate(dialogState.template.template) }) { 
                        Icon(Icons.Default.Delete, contentDescription = "Deletar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETAR") 
                    }
                },
                dismissButton = { TextButton(onClick = onDialogDismiss) { Text("Cancelar") } }
            )
        }
        is TemplateDialogState.ApplyTemplate -> {
            ApplyTemplateDialog(
                onDismiss = onDialogDismiss,
                onConfirm = { dates, saveToGoogle ->
                    onApplyTemplate(dialogState.template.template.id, dates, saveToGoogle) { result ->
                        if (result is Result.Success) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Template aplicado com sucesso!")
                            }
                        }
                    }
                }
            )
        }
        is TemplateDialogState.Hidden -> {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meus Templates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Voltar") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Templates de Rotina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Crie e aplique rotinas em vários dias", 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShowCreateDialog,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Template")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Novo")
                }
                OutlinedButton(
                    onClick = onShowImportDialog, 
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.ContentPasteGo, contentDescription = "Importar Template")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.templates.isEmpty() && !uiState.isSyncing) {
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
                            categoriesMap = uiState.categoriesMap,
                            isSyncing = uiState.isSyncing,
                            onShareClick = { // Adicionado
                                val shareableJson = getShareableJson(templateComEventos.template.id)
                                if (shareableJson != null) {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareableJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Template")
                                    context.startActivity(shareIntent)
                                }
                            },
                            onEditClick = { onShowEditDialog(templateComEventos) },
                            onDeleteClick = { onShowDeleteDialog(templateComEventos) },
                            onApplyClick = { onShowApplyDialog(templateComEventos) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImportTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var jsonString by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar Template") },
        text = {
            Column {
                Text("Cole o código do template compartilhado abaixo para adicioná-lo à sua lista.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = jsonString,
                    onValueChange = { jsonString = it },
                    label = { Text("Código do Template (JSON)") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(jsonString) }, enabled = jsonString.isNotBlank()) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TemplateCard(
    templateComEventos: TemplateComEventos, 
    categoriesMap: Map<String, Category>,
    isSyncing: Boolean,
    onShareClick: () -> Unit, // Adicionado
    onEditClick: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = templateComEventos.template.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (isSyncing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(text = "${templateComEventos.eventos.size} eventos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = onApplyClick) {
                        Icon(Icons.Default.ContentPasteGo, contentDescription = "Aplicar Template", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onShareClick) { // Adicionado
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar Template")
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Template")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar Template", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            if (!isSyncing) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    templateComEventos.eventos.sortedBy { it.horarioInicio }.forEach { evento ->
                        categoriesMap[evento.categoryId]?.let { category ->
                            TemplateEventItem(item = evento, category = category)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateEventItem(item: ItemCronograma, category: Category) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val categoryColor = try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(categoryColor, CircleShape))
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
    templateToEdit: TemplateComEventos? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (id: String?, name: String, events: List<EventFormData>) -> Unit
) {
    val isEditMode = templateToEdit != null
    var templateName by remember { mutableStateOf(templateToEdit?.template?.nome ?: "") }

    val defaultCategory = remember { categories.find { it.nome.equals("Outros", ignoreCase = true) } }

    var eventForms by remember { mutableStateOf(
        templateToEdit?.eventos?.map { EventFormData.fromItemCronograma(it, categories) } 
            ?: listOf(EventFormData(selectedCategory = defaultCategory))
    ) }

    val isFormValid by remember(templateName, eventForms) {
        derivedStateOf {
            templateName.isNotBlank() && eventForms.isNotEmpty() && eventForms.all {
                it.titulo.isNotBlank() &&
                it.selectedCategory != null &&
                !it.horarioTermino.isBefore(it.horarioInicio) &&
                it.horarioTermino != it.horarioInicio
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(if(isEditMode) "Editar Template" else "Criar Template de Rotina", style = MaterialTheme.typography.titleLarge)
                        Text(if(isEditMode) "Ajuste os detalhes do seu template" else "Defina uma rotina com múltiplos eventos", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Nome do Template") },
                    placeholder = { Text("ex: Dia de trabalho, Fim de semana") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = templateName.isBlank()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    LazyColumn() {
                        item {
                            Text("Eventos da Rotina", style = MaterialTheme.typography.titleMedium)
                        }
                        itemsIndexed(eventForms) { index, eventData ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Evento ${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                if (eventForms.size > 1) { 
                                    IconButton(onClick = { eventForms = eventForms.toMutableList().also { it.removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover Evento", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            EventTemplateForm(
                                eventData = eventData,
                                categories = categories,
                                onDataChange = { updatedData ->
                                    eventForms = eventForms.toMutableList().also { it[index] = updatedData }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (index < eventForms.lastIndex) {
                                Divider()
                            }
                        }
                        item {
                            TextButton(
                                onClick = { eventForms = eventForms + EventFormData(selectedCategory = defaultCategory) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento ao Template")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar Evento")
                            }
                        }
                    }
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
                    Button(
                        onClick = { onConfirm(templateToEdit?.template?.id, templateName, eventForms) },
                        enabled = isFormValid
                    ) {
                        Icon(Icons.Default.Check, contentDescription = if(isEditMode) "Salvar Alterações" else "Criar Template")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if(isEditMode) "Salvar Alterações" else "Criar Template")
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
    categories: List<Category>,
    onDataChange: (EventFormData) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val isTimeInvalid by remember(eventData.horarioInicio, eventData.horarioTermino) {
        derivedStateOf {
            eventData.horarioTermino.isBefore(eventData.horarioInicio) || eventData.horarioTermino == eventData.horarioInicio
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Hora de Início",
            initialTime = eventData.horarioInicio,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { 
                onDataChange(eventData.copy(horarioInicio = it))
                showStartTimePicker = false 
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Hora de Término",
            initialTime = eventData.horarioTermino,
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { 
                onDataChange(eventData.copy(horarioTermino = it))
                showEndTimePicker = false
            }
        )
    }

    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = eventData.titulo,
            onValueChange = { onDataChange(eventData.copy(titulo = it)) },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth(),
            isError = eventData.titulo.isBlank()
        )
        OutlinedTextField(
            value = eventData.descricao,
            onValueChange = { onDataChange(eventData.copy(descricao = it)) },
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = eventData.horarioInicio.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Início") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") },
                    isError = isTimeInvalid
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showStartTimePicker = true }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = eventData.horarioTermino.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Término") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") },
                    isError = isTimeInvalid
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

        ExposedDropdownMenuBox(
            expanded = showDropdown,
            onExpandedChange = { showDropdown = !showDropdown },
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                value = eventData.selectedCategory?.nome ?: "",
                onValueChange = {},
                label = { Text("Categoria") },
                isError = eventData.selectedCategory == null,
                leadingIcon = {
                    eventData.selectedCategory?.cor?.let {
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
                categories.forEach { category ->
                    val isSelected = category == eventData.selectedCategory
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
                                val color = try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { Color.Gray }
                                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(category.nome)
                            }
                        },
                        onClick = {
                            onDataChange(eventData.copy(selectedCategory = category))
                            showDropdown = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
