package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme

class UpdateNotesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Novidades da Versão") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                                }
                            }
                        )
                    }
                ) {
                    UpdateNotesScreen(modifier = Modifier.padding(it))
                }
            }
        }
    }
}

@Composable
fun UpdateNotesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("O que há de novo?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Versão 0.9.832-beta (12 de Dezembro de 2024)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Esta atualização foca em estabilidade e correções críticas:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("✅ Correção de Incompatibilidade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Resolvemos um erro crítico que impedia o aplicativo de iniciar em alguns dispositivos devido a conflitos de versão em componentes internos.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("✨ Melhorias Gerais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Melhoramos a estabilidade geral e a performance para garantir uma experiência mais fluida.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Recursos Anteriores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("- Indicador de progresso ao baixar novas atualizações.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("- Nova seção 'Novidades' para você ficar por dentro de tudo.", style = MaterialTheme.typography.bodyLarge)
    }
}
