package br.com.fabriciolima.momentus.ui.screens.template

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.ui.theme.*
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.ui.components.TimelineEventItem
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import br.com.fabriciolima.momentus.ui.theme.EmeraldNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        LaunchedEffect(uiState.error) {
            uiState.error?.let { error ->
                val message = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro desconhecido"
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                    viewModel.onErrorShown()
                }
            }
        }

        when (val dialogState = uiState.dialogState) {
            is TemplateDialogState.CreateNew -> {
                CreateTemplateDialog(
                    categories = uiState.categoriesMap.values.toList(),
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = { id, name, description, events ->
                        viewModel.salvarTemplateCompleto(id, name, description, events) { result ->
                            when (result) {
                                is Result.Success -> {
                                    scope.launch { snackbarHostState.showSnackbar("Template salvo com sucesso!") }
                                }
                                is Result.Error -> {
                                    val error = result.error
                                    val message = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro ao salvar"
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
            is TemplateDialogState.Import -> {
                ImportTemplateDialog(
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = { jsonString ->
                        viewModel.importTemplateFromJson(jsonString) { result ->
                            when (result) {
                                is Result.Success -> {
                                    scope.launch { snackbarHostState.showSnackbar("Template importado com sucesso!") }
                                }
                                is Result.Error -> {
                                    val error = result.error
                                    val message = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro ao importar"
                                    scope.launch { snackbarHostState.showSnackbar(message) }
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
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = { id, name, description, events ->
                        viewModel.salvarTemplateCompleto(id, name, description, events) { result ->
                            when (result) {
                                is Result.Success -> {
                                    scope.launch { snackbarHostState.showSnackbar("Template atualizado com sucesso!") }
                                }
                                is Result.Error -> {
                                    val error = result.error
                                    val message = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro ao salvar"
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
            is TemplateDialogState.ConfirmDelete -> {
                AlertDialog(
                    onDismissRequest = viewModel::onDialogDismiss,
                    icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso") },
                    title = { Text("Deletar Template") },
                    text = { Text("Você tem certeza que quer deletar o template \"${dialogState.template.template.nome}\"? Essa ação não pode ser desfeita.") },
                    confirmButton = {
                        Button(onClick = { viewModel.deleteTemplate(dialogState.template.template) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELETAR")
                        }
                    },
                    dismissButton = { TextButton(onClick = viewModel::onDialogDismiss) { Text("Cancelar") } }
                )
            }
            is TemplateDialogState.ApplyTemplate -> {
                ApplyTemplateDialog(
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = { dates, saveToGoogle ->
                        viewModel.applyTemplateToDates(dialogState.template.template.id, dates, saveToGoogle) { result ->
                            when (result) {
                                is Result.Success -> {
                                    scope.launch { snackbarHostState.showSnackbar("Template aplicado com sucesso!") }
                                }
                                is Result.Error -> {
                                    val error = result.error
                                    val message = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro ao aplicar"
                                    scope.launch { snackbarHostState.showSnackbar(message) }
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
                    title = { Text("Meus Templates", fontWeight = FontWeight.ExtraBold) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::onShowCreateDialog,
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Novo Template")
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::onShowImportDialog) {
                        Icon(Icons.Default.ContentPasteGo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar Template")
                    }
                }

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
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.templates) { templateComEventos ->
                            TemplateCardPremium(
                                templateComEventos = templateComEventos,
                                categoriesMap = uiState.categoriesMap,
                                onEditClick = { viewModel.onShowEditDialog(templateComEventos) },
                                onDeleteClick = { viewModel.onShowDeleteDialog(templateComEventos) },
                                onApplyClick = { viewModel.onShowApplyDialog(templateComEventos) },
                                onShareClick = { 
                                    val shareableJson = viewModel.getShareableJsonForTemplate(templateComEventos.template.id)
                                    if (shareableJson != null) {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareableJson)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Template")
                                        context.startActivity(shareIntent)
                                    }
                                 }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCardPremium(
    templateComEventos: TemplateComEventos,
    categoriesMap: Map<String, Category>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onApplyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = templateComEventos.template.nome,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    templateComEventos.template.descricao?.let {
                        if (it.isNotBlank()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "${templateComEventos.eventos.size} atividades configuradas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                
                Row {
                    IconButton(onClick = onShareClick) { Icon(Icons.Default.Share, "Compartilhar", modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Deletar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateStatChip(Icons.Default.History, "Novo template", Modifier.weight(1f))
                
                val complexity = when {
                    templateComEventos.eventos.size > 8 -> "Alta"
                    templateComEventos.eventos.size > 4 -> "Média"
                    else -> "Leve"
                }
                TemplateStatChip(Icons.Default.AutoGraph, "Foco $complexity", Modifier.weight(1.2f))
                
                TemplateStatChip(Icons.Default.Star, "Eficaz", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(20.dp))

            Column {
                templateComEventos.eventos.sortedBy { it.horarioInicio }.take(4).forEachIndexed { index, evento ->
                    val category = categoriesMap[evento.categoryId]
                    if (category != null) {
                        TimelineEventItem(
                            item = evento,
                            category = category,
                            isChecked = false, 
                            isFirst = index == 0,
                            isLast = index == templateComEventos.eventos.size - 1 || index == 3,
                            onCheckedChange = {},
                            onClick = {},
                            onLongClick = {}
                        )
                    }
                }
                if (templateComEventos.eventos.size > 4) {
                    Text(
                        text = "+ ${templateComEventos.eventos.size - 4} atividades...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black)
            ) {
                Text("Aplicar Template", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TemplateStatChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, maxLines = 1)
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
    onShareClick: () -> Unit,
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
                    IconButton(onClick = onShareClick) {
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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
    onConfirm: (id: String?, name: String, description: String?, events: List<EventFormData>) -> Unit
) {
    val isEditMode = templateToEdit != null
    val focusManager = LocalFocusManager.current
    var templateName by remember { mutableStateOf(templateToEdit?.template?.nome ?: "") }
    var templateDescricao by remember { mutableStateOf(templateToEdit?.template?.descricao ?: "") }

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
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if(isEditMode) "Editar Template" else "Criar Template", 
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Defina uma rotina com múltiplos eventos", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Nome do Template") },
                    placeholder = { Text("ex: Dia de Trabalho, Fim de Semana") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = templateName.isBlank()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = templateDescricao,
                    onValueChange = { templateDescricao = it },
                    label = { Text("Descrição do Template (opcional)") },
                    placeholder = { Text("Ex: Rotina focada em estudos...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            Text(
                                text = "Eventos da Rotina", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        itemsIndexed(eventForms) { index, eventData ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Evento ${index + 1}", 
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (eventForms.size > 1) {
                                            IconButton(onClick = { eventForms = eventForms.toMutableList().also { it.removeAt(index) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
                                }
                            }
                        }
                        
                        item {
                            OutlinedButton(
                                onClick = { eventForms = eventForms + EventFormData(selectedCategory = defaultCategory) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adicionar Evento")
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onConfirm(templateToEdit?.template?.id, templateName, templateDescricao.ifBlank { null }, eventForms) },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black)
                ) {
                    Text(
                        text = if(isEditMode) "Salvar Alterações" else "Criar Template", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventTemplateForm(
    eventData: EventFormData,
    categories: List<Category>,
    onDataChange: (EventFormData) -> Unit
) {
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

    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = eventData.titulo,
            onValueChange = { onDataChange(eventData.copy(titulo = it)) },
            label = { Text("Título") },
            placeholder = { Text("Ex: Exercício Físico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = eventData.titulo.isBlank()
        )

        OutlinedTextField(
            value = eventData.descricao,
            onValueChange = { onDataChange(eventData.copy(descricao = it)) },
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = eventData.horarioInicio.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Início") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    isError = isTimeInvalid
                )
                Box(modifier = Modifier.matchParentSize().clickable { showStartTimePicker = true })
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = eventData.horarioTermino.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fim") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    isError = isTimeInvalid
                )
                Box(modifier = Modifier.matchParentSize().clickable { showEndTimePicker = true })
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category.id == eventData.selectedCategory?.id
                val categoryColor = try { Color(android.graphics.Color.parseColor(category.cor)) } catch (e: Exception) { Color.Gray }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onDataChange(eventData.copy(selectedCategory = category)) },
                    label = { Text(category.nome, fontSize = 11.sp) },
                    leadingIcon = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(categoryColor))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = categoryColor.copy(alpha = 0.2f),
                        selectedLabelColor = categoryColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        selectedBorderColor = categoryColor,
                        selectedBorderWidth = 2.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
