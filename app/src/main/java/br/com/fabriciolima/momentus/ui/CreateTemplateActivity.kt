// ARQUIVO: ui/CreateTemplateActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment // Import que faltava
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.Rotina // Import que faltava
import br.com.fabriciolima.momentus.data.TemplateEvent
import br.com.fabriciolima.momentus.ui.components.AddTemplateEventDialog
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.CreateTemplateViewModel
import br.com.fabriciolima.momentus.viewmodel.CreateTemplateViewModelFactory

class CreateTemplateActivity : ComponentActivity() {
    private val viewModel: CreateTemplateViewModel by viewModels {
        CreateTemplateViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                val templateName by viewModel.templateName.observeAsState("")
                val events by viewModel.events.observeAsState(emptyList())
                val allRoutines by viewModel.todasAsRotinas.observeAsState(emptyList())

                CreateTemplateScreen(
                    templateName = templateName,
                    onTemplateNameChange = { viewModel.onTemplateNameChange(it) },
                    events = events,
                    allRoutines = allRoutines,
                    onAddEvent = { viewModel.addEvent(it) },
                    onRemoveEvent = { viewModel.removeEvent(it) },
                    onNavigateBack = { finish() },
                    onSaveTemplate = {
                        viewModel.saveTemplate {
                            Toast.makeText(this, "Template salvo!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    templateName: String,
    onTemplateNameChange: (String) -> Unit,
    events: List<TemplateEvent>,
    allRoutines: List<Rotina>,
    onAddEvent: (TemplateEvent) -> Unit,
    onRemoveEvent: (TemplateEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    var showAddEventDialog by remember { mutableStateOf(false) }

    if (showAddEventDialog) {
        AddTemplateEventDialog(
            rotinas = allRoutines,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { titulo, desc, dia, inicio, fim, categoria ->
                val newEvent = TemplateEvent(titulo, desc, inicio.toString(), fim.toString(), categoria)
                onAddEvent(newEvent)
                showAddEventDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Novo Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSaveTemplate) {
                Icon(painterResource(id = R.drawable.ic_template), contentDescription = "Salvar Template")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = onTemplateNameChange,
                    label = { Text("Nome do Template") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedButton(
                    onClick = { showAddEventDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Evento")
                }
            }

            items(events) { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(event.titulo, style = MaterialTheme.typography.titleMedium)
                            Text("${event.horarioInicio} - ${event.horarioTermino} (${event.categoria.nome})")
                        }
                        IconButton(onClick = { onRemoveEvent(event) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover Evento")
                        }
                    }
                }
            }
        }
    }
}