package br.com.fabriciolima.momentus.ui.screens.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.ui.components.CategoryListItem
import br.com.fabriciolima.momentus.ui.components.EditCategoryDialog
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryDialogState
import br.com.fabriciolima.momentus.ui.viewmodel.CategoryViewModel
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.Dp
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    bottomBarPadding: Dp = 0.dp,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        LaunchedEffect(uiState.error, uiState.successMessage) {
            uiState.error?.let {
                val message = it.message ?: it.messageResId?.let { resId -> context.getString(resId) } ?: "Erro desconhecido"
                snackbarHostState.showSnackbar(message)
                viewModel.onMessageShown()
            }
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onMessageShown()
            }
        }

        when (val dialogState = uiState.dialogState) {
            is CategoryDialogState.CreateNew -> {
                EditCategoryDialog(
                    category = null,
                    ownedStickers = uiState.ownedStickers,
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = viewModel::upsertCategory
                )
            }
            is CategoryDialogState.Edit -> {
                EditCategoryDialog(
                    category = dialogState.category,
                    ownedStickers = uiState.ownedStickers,
                    onDismiss = viewModel::onDialogDismiss,
                    onConfirm = viewModel::upsertCategory
                )
            }
            is CategoryDialogState.ConfirmDelete -> {
                AlertDialog(
                    onDismissRequest = viewModel::onDialogDismiss,
                    title = { Text("Excluir Categoria", fontWeight = FontWeight.Black) },
                    text = { Text("Tem certeza que deseja excluir a categoria \"${dialogState.category.nome}\"?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.deleteCategory(dialogState.category) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Excluir", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::onDialogDismiss) { Text("Cancelar") }
                    },
                    shape = RoundedCornerShape(28.dp)
                )
            }
            is CategoryDialogState.Hidden -> {}
        }

        Scaffold(
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = bottomBarPadding)
                ) { data ->
                    PremiumSnackbar(data)
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Gerenciar Categorias", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp)) {
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Crie suas categorias do seu jeito",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onShowCreateDialog,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nova Categoria", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomBarPadding + 32.dp)
                ) {
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
}
