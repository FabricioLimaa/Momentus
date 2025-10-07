package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.ui.components.EditCategoryDialog
import br.com.fabriciolima.momentus.viewmodel.CategoryViewModel
import br.com.fabriciolima.momentus.viewmodel.ViewModelFactory

class CategoryActivity : ComponentActivity() {

    private val viewModel: CategoryViewModel by viewModels { 
        ViewModelFactory(AppDatabase.getDatabase(this), application) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CategoryScreen(viewModel = viewModel, onNavigateUp = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, onNavigateUp: () -> Unit) {
    val categories by viewModel.allRotinas.observeAsState(emptyList())
    var editingCategory by remember { mutableStateOf<Rotina?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<Rotina?>(null) }

    if (showEditDialog) {
        EditCategoryDialog(
            category = editingCategory,
            onDismiss = { showEditDialog = false },
            onConfirm = { id, name, color ->
                viewModel.upsertRotina(id, name, color)
                showEditDialog = false
            }
        )
    }

    deletingCategory?.let { categoryToDelete ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Excluir Categoria") },
            text = { Text("Tem certeza que deseja excluir a categoria \"${categoryToDelete.nome}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRotina(categoryToDelete)
                        deletingCategory = null
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
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
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Button(
                onClick = { 
                    editingCategory = null
                    showEditDialog = true 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nova Categoria")
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    CategoryListItem(
                        category = category,
                        onEditClick = { 
                            editingCategory = category
                            showEditDialog = true
                        },
                        onDeleteClick = { deletingCategory = category }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryListItem(
    category: Rotina,
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
                    Icon(Icons.Default.Edit, contentDescription = "Editar Categoria")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Deletar Categoria")
                }
            }
        }
    }
}
