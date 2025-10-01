// ARQUIVO: ui/components/AppDrawer.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import br.com.fabriciolima.momentus.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    googleAccount: GoogleSignInAccount?,
    onRoutinesClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier,
        // --- MODIFICAÇÃO: Cores do Drawer alinhadas ao tema ---
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Cabeçalho com o logo e nome do App
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Momentus",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary // Usa a cor primária do tema
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Minha Agenda", style = MaterialTheme.typography.titleLarge)
        }
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        // Itens de Navegação
        Text("NAVEGAÇÃO", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
        NavigationDrawerItem(
            // --- MODIFICAÇÃO: O primeiro item agora leva para a lista de Rotinas ---
            icon = { Icon(painterResource(id = R.drawable.ic_empty_list), contentDescription = null) },
            label = { Text("Minhas Categorias") },
            selected = false,
            onClick = { onRoutinesClicked(); onCloseDrawer() }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_schedule), contentDescription = null) },
            label = { Text("Calendário") },
            // O ideal é ter uma variável para controlar o item selecionado,
            // por enquanto deixaremos como 'true' para o Calendário.
            selected = false,
            onClick = { onCloseDrawer() }, // Já estamos na tela, então só fecha o drawer
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_templates), contentDescription = null) },
            label = { Text("Templates") },
            selected = false,
            onClick = { onTemplatesClicked(); onCloseDrawer() }
        )

        // Espaçador que empurra a seção do usuário para o final
        Spacer(modifier = Modifier.weight(1f))

        // Seção do Usuário
        Divider()
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USUÁRIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // --- MODIFICAÇÃO: Avatar do usuário com fundo ---
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = googleAccount?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(googleAccount?.displayName ?: "Usuário", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(googleAccount?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                }
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