package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.domain.model.UserLevel
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

    // Cálculo da progressão de nível
    val userLevel = remember(userData?.points) {
        UserLevel.fromPoints(userData?.points ?: 0)
    }

    ModalDrawerSheet(
        windowInsets = WindowInsets(0, 0, 0, 0) 
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(text = "Momentus", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Viva o presente, planeje o futuro", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "NAVEGAÇÃO", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
                NavigationDrawerItem(
                    label = { Text("Agenda") },
                    selected = false,
                    onClick = { onNavigate(Screen.Calendar.route) },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
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
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = { onNavigate(Screen.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
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

            // --- SEÇÃO DO USUÁRIO COM PROGRESSÃO DE NÍVEL ---
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Informações Legais") },
                    selected = false,
                    onClick = { onNavigate(Screen.Legal.route) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(displayName = displayName, photoUrl = photoUrl?.toString(), modifier = Modifier.size(56.dp))
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
                
                // Barra de Nível e Rank
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nível ${userLevel.level}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = userLevel.rankColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = userLevel.rankName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = userLevel.rankColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { userLevel.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${userLevel.currentXp} / ${userLevel.nextLevelXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Item de saída (Sair)
                NavigationDrawerItem(
                    label = { Text("Sair")},
                    selected = false,
                    onClick = onLogout,
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    modifier = Modifier.height(30.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    )
                )
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
