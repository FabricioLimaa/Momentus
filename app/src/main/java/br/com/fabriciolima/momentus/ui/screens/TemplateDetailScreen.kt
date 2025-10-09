package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.ui.components.AddEventToTemplateDialog
import br.com.fabriciolima.momentus.ui.components.EventListItem
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDetailViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(templateId: String, viewModel: TemplateDetailViewModel, onNavigateUp: () -> Unit) {

    LaunchedEffect(templateId) {
        viewModel.loadTemplate(templateId)
    }

    val uiState by viewModel.uiState.observeAsState()
    val eventos = uiState?.template?.eventos ?: emptyList()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddEventToTemplateDialog(
            rotinas = uiState?.rotinasMap?.values?.toList() ?: emptyList(),
            onDismiss = { showDialog = false },
            onConfirm = { titulo, descricao, inicio, fim, rotina ->
                viewModel.addEventToTemplate(titulo, descricao, inicio, fim, rotina)
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState?.template?.template?.nome ?: "Carregando...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Eventos do Template", style = MaterialTheme.typography.headlineMedium)
            Text("Arraste para reordenar os eventos que compõem esta rotina.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Evento")
            }
            Spacer(modifier = Modifier.height(16.dp))

            val state = rememberReorderableLazyListState(onMove = { from, to ->
                (from.key as? String)?.let { fromId ->
                    (to.key as? String)?.let { toId ->
                        viewModel.reorderEventos(fromId, toId)
                    }
                }
            })

            LazyColumn(
                state = state.listState,
                modifier = Modifier.reorderable(state),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(eventos, key = { it.id }) { item ->
                    ReorderableItem(reorderableState = state, key = item.id) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Arrastar para reordenar",
                                modifier = Modifier
                                    .detectReorderAfterLongPress(state)
                                    .padding(end = 8.dp)
                            )
                            uiState?.rotinasMap?.get(item.rotinaId)?.let {
                                EventListItem(item = item, rotina = it)
                            }
                        }
                    }
                }
            }
        }
    }
}
