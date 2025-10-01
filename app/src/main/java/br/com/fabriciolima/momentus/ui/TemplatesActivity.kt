// ARQUIVO: ui/TemplatesActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.ui.components.TemplateListItem
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.viewmodel.TemplateViewModelFactory

class TemplatesActivity : ComponentActivity() {

    private val viewModel: TemplateViewModel by viewModels {
        TemplateViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                TemplatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplateViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val templates by viewModel.todosOsTemplates.observeAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates de Rotina") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // --- MODIFICAÇÃO: Removemos o diálogo antigo e abrimos a nova Activity ---
                context.startActivity(Intent(context, CreateTemplateActivity::class.java))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Template")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates, key = { it.template.id }) { templateComItens ->
                TemplateListItem(
                    templateComItens = templateComItens,
                    onApply = { template ->
                        viewModel.loadTemplate(template)
                        Toast.makeText(context, "Template '${template.nome}' aplicado!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { template ->
                        viewModel.deleteTemplate(template)
                        Toast.makeText(context, "Template deletado.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}