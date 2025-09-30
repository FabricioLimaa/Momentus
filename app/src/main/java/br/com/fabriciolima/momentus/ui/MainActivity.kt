// ARQUIVO: ui/MainActivity.kt (CÓDIGO COMPLETO E SIMPLIFICADO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.components.RotinaListItem
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                // A MainActivity agora só chama o Composable da sua própria tela.
                RoutinesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() } // Botão de voltar fecha a tela
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
    val editorRotinaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // A atualização da lista já é gerenciada pelo LiveData.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Rotinas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, EditorRotinaComposeActivity::class.java)
                editorRotinaLauncher.launch(intent)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Rotina")
            }
        }
    ) { paddingValues ->
        val rotinasComMetas by viewModel.rotinas.observeAsState(initial = emptyList())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (rotinasComMetas.isEmpty()) {
                // Estado Vazio
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ... (UI do estado vazio)
                }
            } else {
                // Lista de Rotinas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rotinasComMetas, key = { it.rotina.id }) { rotinaComMeta ->
                        RotinaListItem(
                            rotinaComMeta = rotinaComMeta,
                            onItemClicked = {
                                val intent = Intent(context, EditorRotinaComposeActivity::class.java)
                                intent.putExtra("ROTINA_PARA_EDITAR", it.rotina)
                                editorRotinaLauncher.launch(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}