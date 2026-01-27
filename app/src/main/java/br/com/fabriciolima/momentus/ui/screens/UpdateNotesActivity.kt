package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("O que há de novo?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Versão 1.1.01 (22 de Janeiro de 2026)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Esta atualização foca em usabilidade e polimento da interface, tornando a gestão da sua agenda ainda mais rápida e agradável.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("🗑️ Exclusão Múltipla de Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Agora você pode excluir vários eventos de uma só vez. Pressione e segure um item para iniciar o modo de seleção, escolha os eventos e limpe sua agenda com um único toque.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("🎨 Polimento Visual e de Usabilidade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "O checkbox para concluir e selecionar tarefas agora é o mesmo, localizado à esquerda, proporcionando uma experiência consistente. Além disso, a barra de status agora se adapta à cor do modo de seleção.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("🧹 Interface Mais Limpa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Para uma visualização mais focada, removemos a descrição do evento diretamente da lista principal. Os detalhes ainda estão a um clique de distância.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))


        Text("Recursos Anteriores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("🔔 Notificações de Início e Término com sons personalizados.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("🚀 Melhorias de estabilidade e performance no Widget.", style = MaterialTheme.typography.bodyLarge)
    }
}
