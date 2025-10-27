package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.TemplateEvent
import br.com.fabriciolima.momentus.ui.components.AddTemplateEventDialog
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.CreateTemplateViewModel
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import java.util.UUID

@AndroidEntryPoint
class CreateTemplateActivity : ComponentActivity() {
    private val viewModel: CreateTemplateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                val templateName by viewModel.templateName.collectAsStateWithLifecycle()
                val events by viewModel.events.collectAsStateWithLifecycle()
                val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

                CreateTemplateScreen(
                    templateName = templateName,
                    onTemplateNameChange = { viewModel.onTemplateNameChange(it) },
                    events = events,
                    allCategories = allCategories,
                    onAddEvent = { viewModel.addEvent(it) },
                    onRemoveEvent = { viewModel.removeEvent(it) },
                    onNavigateBack = { finish() },
                    onSaveTemplate = {
                        viewModel.saveTemplate { result ->
                            when (result) {
                                is Result.Success -> {
                                    Toast.makeText(this, "Template salvo!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                is Result.Error -> {
                                    Toast.makeText(this, result.exception.message, Toast.LENGTH_LONG).show()
                                }
                            }
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
    allCategories: List<Category>,
    onAddEvent: (TemplateEvent) -> Unit,
    onRemoveEvent: (TemplateEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    var showAddEventDialog by remember { mutableStateOf(false) }

    if (showAddEventDialog) {
        AddTemplateEventDialog(
            categories = allCategories,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { titulo, desc, dia, inicio, fim, categoria ->
                val newEvent = TemplateEvent(
                    id = UUID.randomUUID(),
                    titulo = titulo,
                    descricao = desc ?: "",
                    horarioInicio = inicio.toString(),
                    horarioTermino = fim.toString(),
                    categoria = categoria
                )
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

            if (events.isNotEmpty()) {
                 item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSaveTemplate, modifier = Modifier.fillMaxWidth()) {
                        Text("Salvar Template")
                    }
                }
            }
        }
    }
}
