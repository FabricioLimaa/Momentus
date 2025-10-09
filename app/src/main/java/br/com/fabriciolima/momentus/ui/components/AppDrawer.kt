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
import coil.compose.AsyncImage
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
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Momentus",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Minha Agenda", style = MaterialTheme.typography.titleLarge)
        }
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("NAVEGAÇÃO", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_empty_list), contentDescription = null) },
            label = { Text("Minhas Categorias") },
            selected = false,
            onClick = { onRoutinesClicked(); onCloseDrawer() }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_schedule), contentDescription = null) },
            label = { Text("Calendário") },
            selected = false,
            onClick = { onCloseDrawer() }, 
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(id = R.drawable.ic_templates), contentDescription = null) },
            label = { Text("Templates") },
            selected = false,
            onClick = { onTemplatesClicked(); onCloseDrawer() }
        )

        Spacer(modifier = Modifier.weight(1f))

        Divider()
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USUÁRIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = googleAccount?.photoUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
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