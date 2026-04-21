package br.com.fabriciolima.momentus.ui.screens.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotesScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novidades da Versão") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("O que há de novo?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Versão 1.2.2 (Abril de 2026)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nesta versão, o Momentus vai além da organização e foca no seu prazer de realizar metas.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Text("📊 Dashboard de Estatísticas Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sua evolução agora está mais clara e bonita. Implementamos novos cartões de insights (Total, Média e Destaque) e uma visualização de Consistência Semanal.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("🎭 O Ciclo de Dopamina (Animações e Som)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tornamos a conclusão de tarefas mais gratificante. Agora, ao finalizar um hábito, você será recebido com animações vibrantes, efeitos sonoros satisfatórios e uma resposta tátil (vibração) que reforça seu sucesso.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("🏆 Celebração de Conquistas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Ganhar uma conquista agora é um evento especial. Desbloqueamos um novo diálogo festivo com animações exclusivas em tela cheia para celebrar cada marco importante da sua jornada.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("🚀 Robustez e Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Finalizamos a unificação técnica do nosso motor de agenda. O app está mais rápido, consome menos bateria e sua sincronização com a nuvem está 100% resiliente.",
                style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Momentus - Viva o presente, planeje o futuro.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
