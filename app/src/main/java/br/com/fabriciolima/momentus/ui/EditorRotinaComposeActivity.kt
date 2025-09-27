// ARQUIVO: ui/EditorRotinaComposeActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.viewmodel.MainViewModelFactory

class EditorRotinaComposeActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rotinaParaEditar = intent.getSerializableExtra("ROTINA_PARA_EDITAR") as? Rotina

        setContent {
            MomentusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        rotinaInicial = rotinaParaEditar,
                        viewModel = viewModel,
                        onSave = { rotinaSalva ->
                            val resultIntent = Intent().putExtra("ROTINA_SALVA", rotinaSalva)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    rotinaInicial: Rotina?,
    viewModel: MainViewModel?,
    onSave: (Rotina) -> Unit
) {
    val context = LocalContext.current

    var nome by remember { mutableStateOf(rotinaInicial?.nome ?: "") }
    // --- MODIFICAÇÃO: Adicionamos o estado para os novos campos ---
    var descricao by remember { mutableStateOf(rotinaInicial?.descricao ?: "") }
    var tag by remember { mutableStateOf(rotinaInicial?.tag ?: "") }
    var duracao by remember { mutableStateOf(rotinaInicial?.duracaoPadraoMinutos?.toString() ?: "") }

    val cores = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF9E9E9E)
    )
    val corInicial = rotinaInicial?.cor?.let { Color(android.graphics.Color.parseColor(it)) }
    var corSelecionada by remember { mutableStateOf(corInicial ?: cores.first()) }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Adicionar/Editar Rotina", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nome, onValueChange = { nome = it },
            label = { Text("Nome da Rotina") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- MODIFICAÇÃO: Adicionamos os novos TextFields ---
        OutlinedTextField(
            value = descricao, onValueChange = { descricao = it },
            label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = tag, onValueChange = { tag = it },
            label = { Text("Tag (ex: estudo)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        // --- FIM DA MODIFICAÇÃO ---

        OutlinedTextField(
            value = duracao, onValueChange = { duracao = it },
            label = { Text("Duração (em minutos)") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Cor", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ColorPicker(
            cores = cores, corSelecionada = corSelecionada, onColorSelected = { corSelecionada = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (nome.isBlank() || duracao.isBlank()) { /* ... (validação) ... */ return@Button }
                val duracaoMinutos = duracao.toIntOrNull()
                if (duracaoMinutos == null) { /* ... (validação) ... */ return@Button }

                val rotinaParaSalvar = Rotina(
                    id = rotinaInicial?.id ?: java.util.UUID.randomUUID().toString(),
                    nome = nome,
                    duracaoPadraoMinutos = duracao.toIntOrNull() ?: 0,
                    cor = String.format("#%06X", (0xFFFFFF and corSelecionada.toArgb())),
                    // --- MODIFICAÇÃO: Incluímos os novos campos no objeto a ser salvo ---
                    descricao = descricao.takeIf { it.isNotBlank() },
                    tag = tag.takeIf { it.isNotBlank() }
                )

                viewModel?.addRotina(rotinaParaSalvar)
                Toast.makeText(context, "Rotina salva!", Toast.LENGTH_SHORT).show()
                onSave(rotinaParaSalvar)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Salvar")
        }
    }
}

// Uma função Composable separada para o seletor de cores.
@Composable
fun ColorPicker(
    cores: List<Color>,
    corSelecionada: Color?,
    onColorSelected: (Color) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cores) { cor ->
            val isSelected = cor == corSelecionada
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cor)
                    .clickable { onColorSelected(cor) }
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MomentusTheme {
        EditorScreen(
            rotinaInicial = null, // Para a preview, simulamos a criação de uma nova rotina
            viewModel = null,
            onSave = {}
        )
    }
}