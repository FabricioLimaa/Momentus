// ARQUIVO: ui/components/AppDrawer.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

// Esta é a função que desenha todo o conteúdo do menu lateral
@Composable
fun AppDrawer(
    googleAccount: GoogleSignInAccount?, // Recebe a conta do Google para exibir os dados
    onCalendarClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        // Cabeçalho com o logo e nome do App
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Momentus",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Minha Agenda", style = MaterialTheme.typography.titleLarge)
        }
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        // Itens de Navegação
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_schedule), contentDescription = null) },
            label = { Text("Calendário") },
            selected = false, // Podemos adicionar lógica de seleção no futuro
            onClick = { onCalendarClicked(); onCloseDrawer() }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_templates), contentDescription = null) },
            label = { Text("Templates") },
            selected = false,
            onClick = { onTemplatesClicked(); onCloseDrawer() }
        )
        // Adicione aqui outros itens de navegação, como "Estatísticas", se desejar.

        // Espaçador para empurrar a seção do usuário para baixo
        Spacer(modifier = Modifier.weight(1f))

        // Seção do Usuário
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USUÁRIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (googleAccount != null) {
                Text(googleAccount.displayName ?: "", style = MaterialTheme.typography.bodyLarge)
                Text(googleAccount.email ?: "", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Não conectado", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onLogoutClicked(); onCloseDrawer() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painterResource(id = R.drawable.ic_logout), contentDescription = "Sair")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair")
            }
        }
    }
}