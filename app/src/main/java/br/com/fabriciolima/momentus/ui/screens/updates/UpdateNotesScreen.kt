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

            Text("Versão 1.3.8 (Abril de 2026)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Esta atualização eleva o patamar de navegação e identidade visual do Momentus.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Text("🚀 Navegação de Elite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Diga adeus aos labirintos! Implementamos um menu no celular para acesso instantâneo. No Tablet, o menu lateral agora está perfeitamente centralizado e completo.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("👤 Perfil 'Você' Redesenhado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sua jornada em destaque: um novo dashboard com avatar circular, progresso de nível (XP) e acesso rápido a Ajustes e Novidades.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("🏅 Medalhas com Identidade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Cada conquista agora tem um ícone único e representativo! Explore suas medalhas clicando nelas para ver detalhes, descrição e a data da sua glória.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("🌑 Estética Layers (Premium Dark)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Introduzimos profundidade visual com a paleta 'Layers'. Fundo preto absoluto e cards em cinza profundo criam uma hierarquia clara e moderna, economizando bateria e reduzindo o cansaço visual.",
                style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Momentus - Viva o presente, planeje o futuro.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
