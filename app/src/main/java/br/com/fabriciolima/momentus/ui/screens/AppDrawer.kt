package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.ui.Screen
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerContent(
    userData: UserData?,
    account: GoogleSignInAccount?,
    showUpdateBadge: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onUpdatesClicked: () -> Unit
) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    
    val displayName = userData?.displayName 
        ?: firebaseUser?.displayName 
        ?: account?.displayName 
        ?: "Usuário"
        
    val email = userData?.email 
        ?: firebaseUser?.email 
        ?: account?.email 
        ?: ""
        
    val photoUrl = account?.photoUrl 
        ?: firebaseUser?.photoUrl

    ModalDrawerSheet(
        // Removendo padding do ModalDrawerSheet para o fundo preencher tudo
        windowInsets = WindowInsets(0, 0, 0, 0) 
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                // Aplicamos o padding de segurança APENAS no conteúdo interno
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(text = "Minha Agenda", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Organize seu tempo", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "NAVEGAÇÃO", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
                NavigationDrawerItem(
                    label = { Text("Calendário") },
                    selected = false,
                    onClick = { onNavigate(Screen.Calendar.route) },
                    icon = { Icon(Icons.Default.CalendarViewMonth, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Templates") },
                    selected = false,
                    onClick = { onNavigate(Screen.Templates.route) },
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Categorias") },
                    selected = false,
                    onClick = { onNavigate(Screen.Categories.route) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Estatísticas") },
                    selected = false,
                    onClick = { onNavigate(Screen.Stats.route) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Conquistas") },
                    selected = false,
                    onClick = { onNavigate(Screen.Achievements.route) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Novidades") },
                    selected = false,
                    onClick = {
                        onUpdatesClicked()
                        onNavigate(Screen.Updates.route)
                    },
                    icon = { Icon(Icons.Default.NewReleases, contentDescription = "Novidades") },
                    badge = {
                        if (showUpdateBadge) {
                            Badge()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Informações Legais") },
                    selected = false,
                    onClick = { onNavigate(Screen.Legal.route) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "USUÁRIO", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(displayName = displayName, photoUrl = photoUrl?.toString(), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(text = displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.clickable(onClick = onLogout).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sair",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    displayName: String,
    photoUrl: String?,
    modifier: Modifier = Modifier
) {
    if (photoUrl != null && photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Foto do Perfil",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = displayName
                .split(' ')
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
                .ifEmpty { "U" }
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
