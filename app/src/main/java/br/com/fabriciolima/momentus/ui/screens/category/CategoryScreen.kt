package br.com.fabriciolima.momentus.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.ui.components.EditCategoryDialog
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryUiState
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

@Composable
fun CategoryRoute(
    onNavigateUp: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoryScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onShowCreateDialog = viewModel::onShowCreateDialog,
        onShowEditDialog = viewModel::onShowEditDialog,
        onShowConfirmDeleteDialog = viewModel::onShowConfirmDeleteDialog,
        onDialogDismiss = viewModel::onDialogDismiss,
        onUpsertCategory = viewModel::upsertCategory,
        onDeleteCategory = viewModel::deleteCategory,
        onErrorShown = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    uiState: CategoryUiState,
    onNavigateUp: () -> Unit,
    onShowCreateDialog: () -> Unit,
    onShowEditDialog: (Category) -> Unit,
    onShowConfirmDeleteDialog: (Category) -> Unit,
    onDialogDismiss: () -> Unit,
    onUpsertCategory: (String?, String, String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onErrorShown: () -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { appError ->
            val message = appError.message ?: appError.messageResId?.let { context.getString(it) } ?: "Erro desconhecido"
            scope.launch {
                snackbarHostState.showSnackbar(message)
                onErrorShown()
            }
        }
    }

    when (val dialogState = uiState.dialogState) {
        is CategoryDialogState.CreateNew -> {
            EditCategoryDialog(
                category = null,
                onDismiss = onDialogDismiss,
                onConfirm = { id, name, color -> onUpsertCategory(id, name, color) }
            )
        }
        is CategoryDialogState.Edit -> {
            EditCategoryDialog(
                category = dialogState.category,
                onDismiss = onDialogDismiss,
                onConfirm = { id, name, color -> onUpsertCategory(id, name, color) }
            )
        }
        is CategoryDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = onDialogDismiss,
                title = { Text("Excluir Categoria") },
                text = { Text("Tem certeza que deseja excluir a categoria \"${dialogState.category.nome}\"?") },
                confirmButton = {
                    TextButton(onClick = { onDeleteCategory(dialogState.category) }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDialogDismiss) { Text("Cancelar") }
                }
            )
        }
        is CategoryDialogState.Hidden -> {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Categorias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Categorias", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Crie suas categorias para melhor organização",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onShowCreateDialog,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nova Categoria", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.categories) { category ->
                    CategoryListItem(
                        category = category,
                        onEditClick = { onShowEditDialog(category) },
                        onDeleteClick = { onShowConfirmDeleteDialog(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(android.graphics.Color.parseColor(category.cor)), CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(category.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Categoria", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Deletar Categoria", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
