package br.com.fabriciolima.momentus.ui.screens.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.ui.components.CategoryListItem
import br.com.fabriciolima.momentus.ui.components.EditCategoryDialog
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            val message = it.message ?: it.messageResId?.let { resId -> context.getString(resId) } ?: "Erro desconhecido"
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.onErrorShown()
            }
        }
    }

    when (val dialogState = uiState.dialogState) {
        is CategoryDialogState.CreateNew -> {
            EditCategoryDialog(
                category = null,
                onDismiss = viewModel::onDialogDismiss,
                onConfirm = viewModel::upsertCategory
            )
        }
        is CategoryDialogState.Edit -> {
            EditCategoryDialog(
                category = dialogState.category,
                onDismiss = viewModel::onDialogDismiss,
                onConfirm = viewModel::upsertCategory
            )
        }
        is CategoryDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = viewModel::onDialogDismiss,
                title = { Text("Excluir Categoria") },
                text = { Text("Tem certeza que deseja excluir a categoria \"${dialogState.category.nome}\"?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteCategory(dialogState.category) }) 
                    { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDialogDismiss) { Text("Cancelar") }
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
                    IconButton(onClick = { navController.navigateUp() }) {
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
                onClick = viewModel::onShowCreateDialog,
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
                        item = CategoryWithMeta(category, null),
                        onEdit = { viewModel.onShowEditDialog(category) },
                        onDelete = { viewModel.onShowConfirmDeleteDialog(category) }
                    )
                }
            }
        }
    }
}
