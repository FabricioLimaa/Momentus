// ARQUIVO: ui/EditorRotinaComposeActivity.kt (CÓDIGO COMPLETO E FUNCIONAL)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme

class EditorRotinaComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    // --- MODIFICAÇÃO 1: Precisamos do contexto para criar a Intent e fechar a tela ---
    val context = LocalContext.current as Activity

    var nome by remember { mutableStateOf("") }
    var duracao by remember { mutableStateOf("") }
    val cores = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF9E9E9E)
    )
    var corSelecionada by remember { mutableStateOf<Color?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Adicionar/Editar Rotina", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome da Rotina") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = duracao,
            onValueChange = { duracao = it },
            label = { Text("Duração (em minutos)") },
            modifier = Modifier.fillMaxWidth(),
            // Garante que o teclado seja apenas numérico
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Cor", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPicker(
            cores = cores,
            corSelecionada = corSelecionada,
            onColorSelected = { corSelecionada = it }
        )
        Spacer(modifier = Modifier.weight(1f))

        Button(
            // --- MODIFICAÇÃO 2: Adicionamos a lógica de salvar ao clique do botão ---
            onClick = {
                // Validação dos dados
                if (nome.isBlank() || duracao.isBlank() || corSelecionada == null) {
                    Toast.makeText(context, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                } else {
                    // Cria o objeto Rotina
                    val rotinaParaSalvar = Rotina(
                        nome = nome,
                        duracaoPadraoMinutos = duracao.toInt(),
                        // Converte a cor do Compose para um código hexadecimal String
                        cor = String.format("#%06X", (0xFFFFFF and corSelecionada!!.toArgb()))
                    )

                    // Cria a Intent para devolver o resultado
                    val resultIntent = Intent()
                    resultIntent.putExtra("ROTINA_SALVA", rotinaParaSalvar)
                    context.setResult(Activity.RESULT_OK, resultIntent)

                    // Fecha a tela
                    context.finish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
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
        EditorScreen()
    }
}