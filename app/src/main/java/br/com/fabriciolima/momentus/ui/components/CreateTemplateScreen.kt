// ARQUIVO: ui/components/CreateTemplateScreen.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import br.com.fabriciolima.momentus.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    onNavigateBack: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    // A lista de eventos que o usuário está adicionando
    val events = remember { mutableStateListOf<Unit>() } // Placeholder

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
                Icon(painterResource(id = R.drawable.ic_templates), contentDescription = "Salvar Template")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo para o nome do Template
            item {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Nome do Template") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Botão para adicionar um novo evento
            item {
                OutlinedButton(
                    onClick = { /* TODO: Abrir diálogo para adicionar evento */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Evento")
                }
            }

            // Lista dos eventos adicionados
            items(events) {
                // TODO: Criar um Composable para exibir o 'TemplateEvent'
                Text("Evento adicionado (placeholder)")
            }
        }
    }
}