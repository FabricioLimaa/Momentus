package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.repository.SyncStatus
import br.com.fabriciolima.momentus.ui.components.CategoryListItem
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.widget.OPEN_NEW_EVENT_DIALOG_KEY
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica se a Activity foi iniciada com a intenção de criar um novo evento (vinda do widget)
        if (intent.getBooleanExtra(OPEN_NEW_EVENT_DIALOG_KEY, false)) {
            // Navega para a tela de criação de evento
            val newEventIntent = Intent(this, CreateTemplateActivity::class.java).apply {
                // Limpa o extra para não entrar em loop caso o usuário volte
                removeExtra(OPEN_NEW_EVENT_DIALOG_KEY)
            }
            startActivity(newEventIntent)
            // Finaliza a MainActivity para não deixá-la na pilha de volta
            finish()
            return // Impede a execução do setContent abaixo
        }

        setContent {
            MomentusTheme {
                RoutinesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val editorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // A lista é atualizada automaticamente pelo LiveData
        }
    }

    val categoriesWithMetas by viewModel.categoriesWithMetas.observeAsState(initial = emptyList())
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    SyncStatusIcon(status = syncStatus)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crie e gerencie as categorias.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(context, EditorRotinaComposeActivity::class.java)
                    editorLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Nova Categoria")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (categoriesWithMetas.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Ocupa o espaço restante
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ListAlt,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Nenhuma categoria cadastrada.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        "Clique no botão acima para criar sua primeira categoria.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // Ocupa o espaço restante
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoriesWithMetas, key = { it.category.id }) { categoryWithMeta ->
                        CategoryListItem(
                            item = categoryWithMeta,
                            onEdit = { item ->
                                val intent = Intent(context, EditorRotinaComposeActivity::class.java).apply {
                                    putExtra("CATEGORY_TO_EDIT", item.category)
                                }
                                editorLauncher.launch(intent)
                            },
                            onDelete = { item ->
                                viewModel.deleteCategory(item.category)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusIcon(status: SyncStatus) {
    val icon = when (status) {
        SyncStatus.CONNECTED -> Icons.Default.CloudDone
        SyncStatus.SYNCING -> Icons.Default.Sync
        SyncStatus.OFFLINE -> Icons.Default.CloudOff
    }
    val tint = when (status) {
        SyncStatus.CONNECTED -> Color(0xFF388E3C) // Verde
        SyncStatus.SYNCING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        SyncStatus.OFFLINE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Status da Sincronização: $status",
        tint = tint,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
