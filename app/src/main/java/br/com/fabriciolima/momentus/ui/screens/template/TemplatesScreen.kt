package br.com.fabriciolima.momentus.ui.screens.template

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.ui.components.ApplyTemplateDialog
import br.com.fabriciolima.momentus.ui.screens.Routes
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateUiState
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TemplatesRoute(
    navController: NavController,
    onNavigateUp: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    TemplatesScreen(
        uiState = uiState,
        navController = navController,
        onNavigateUp = onNavigateUp,
        onShowImportDialog = viewModel::onShowImportDialog,
        onShowDeleteDialog = viewModel::onShowDeleteDialog,
        onShowApplyDialog = viewModel::onShowApplyDialog,
        onDialogDismiss = viewModel::onDialogDismiss,
        onImportTemplate = { json, callback ->
            viewModel.importTemplateFromJson(json, callback)
        },
        onDeleteTemplate = viewModel::deleteTemplate,
        onApplyTemplate = { id, dates, toGoogle, callback ->
            viewModel.applyTemplateToDates(id, dates, toGoogle, callback)
        },
        getShareableJson = viewModel::getShareableJsonForTemplate,
        onErrorShown = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    uiState: TemplateUiState,
    navController: NavController,
    onNavigateUp: () -> Unit,
    onShowImportDialog: () -> Unit,
    onShowDeleteDialog: (TemplateComEventos) -> Unit,
    onShowApplyDialog: (TemplateComEventos) -> Unit,
    onDialogDismiss: () -> Unit,
    onImportTemplate: (String, (Result<Unit>) -> Unit) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onApplyTemplate: (String, List<LocalDate>, Boolean, (Result<Unit>) -> Unit) -> Unit,
    getShareableJson: (String) -> String?,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    when (val dialogState = uiState.dialogState) {
        is TemplateDialogState.Import -> {
            ImportTemplateDialog(
                onDismiss = onDialogDismiss,
                onConfirm = { jsonString ->
                    onImportTemplate(jsonString) { result ->
                        scope.launch {
                            val message = when (result) {
                                is Result.Success -> "Template importado com sucesso!"
                                is Result.Error -> result.exception.message ?: "Erro desconhecido"
                            }
                            snackbarHostState.showSnackbar(message)
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
                    Button(onClick = {
                        onDeleteTemplate(dialogState.template.template)
                        onDialogDismiss()
                    }) {
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
        else -> {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meus Templates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
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
                    onClick = { navController.navigate(Routes.CREATE_TEMPLATE) },
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
                    items(uiState.templates, key = { it.template.id }) { templateComEventos ->
                        TemplateCard(
                            templateComEventos = templateComEventos,
                            categoriesMap = uiState.categoriesMap,
                            isSyncing = uiState.isSyncing,
                            onShareClick = {
                                val shareableJson = getShareableJson(templateComEventos.template.id)
                                if (shareableJson != null) {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareableJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Template")
                                    navController.context.startActivity(shareIntent)
                                }
                            },
                            onEditClick = { navController.navigate("${Routes.TEMPLATE_DETAIL}/${templateComEventos.template.id}") },
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
            text = "${item.horarioInicio?.format(timeFormatter)} - ${item.horarioTermino?.format(timeFormatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
