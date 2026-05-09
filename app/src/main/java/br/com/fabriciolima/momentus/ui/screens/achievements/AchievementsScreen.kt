package br.com.fabriciolima.momentus.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.domain.model.UserLevel
import br.com.fabriciolima.momentus.ui.Screen
import br.com.fabriciolima.momentus.ui.screens.UserAvatar
import br.com.fabriciolima.momentus.ui.screens.getStreakColor
import br.com.fabriciolima.momentus.ui.viewmodel.AchievementUiInfo
import br.com.fabriciolima.momentus.ui.viewmodel.AchievementsViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

// --- BUSCA: MAPEAMENTO DE ÍCONES ÚNICOS PARA CADA CONQUISTA ---
@Composable
fun getIconForAchievement(achievementId: String): ImageVector {
    return when (achievementId) {
        "FIRST_HABIT" -> Icons.Default.RocketLaunch       // Foguete: Início
        "FIRST_TEMPLATE" -> Icons.Default.AutoAwesome      // Brilho: Organização
        "STREAK_3" -> Icons.Default.Bolt                 // Raio: Energia
        "STREAK_7" -> Icons.Default.Stars                // Estrelas: Consistência
        "STREAK_30" -> Icons.Default.EmojiEvents         // Troféu: Maestria
        "COMPLETED_10" -> Icons.Default.DoneAll           // Check Duplo: Progresso (Resolvido erro Filter10)
        "COMPLETED_50" -> Icons.Default.WorkspacePremium  // Selo: Premium
        "COMPLETED_100" -> Icons.Default.Diamond          // Diamante: Raro
        else -> Icons.Default.Star
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userLevel = UserLevel.fromPoints(uiState.points)

    // --- BUSCA: DIÁLOGO DE DETALHES DA MEDALHA ---
    uiState.selectedAchievement?.let {
        AchievementDetailDialog(
            info = it,
            onDismiss = { viewModel.onDialogDismiss() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas", fontWeight = FontWeight.ExtraBold) },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- BUSCA: HEADER COM NOME E AVATAR ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = firebaseUser?.displayName ?: "Usuário",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Nível ${userLevel.level}", 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(text = userLevel.rankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    UserAvatar(
                        displayName = firebaseUser?.displayName ?: "U",
                        photoUrl = firebaseUser?.photoUrl?.toString(),
                        modifier = Modifier.size(80.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            // --- BUSCA: STATUS DE SEQUÊNCIA E PONTOS ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Card da Chama de Sequência
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = getStreakColor(uiState.streakCount),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(text = uiState.streakCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(text = "Dias seguidos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Card de Pontos Totais
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(text = uiState.points.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(text = "Pontos totais", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // --- BUSCA: SEÇÃO DE MEDALHAS CLICÁVEIS (HORIZONTAL) ---
            item {
                Column {
                    Text(text = "Minhas Medalhas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(uiState.achievements) { info ->
                            MedalItem(info, onClick = { viewModel.onAchievementClicked(info) })
                        }
                    }
                }
            }

            // --- BUSCA: CARD DE PROGRESSO XP ---
            item {
                Text(text = "Progresso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ActivityCard(userLevel)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/**
 * Representa uma medalha individual no carrossel.
 */
@Composable
fun MedalItem(info: AchievementUiInfo, onClick: () -> Unit) {
    val icon = getIconForAchievement(info.achievement.id)
    val color = if (info.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(2.dp, Brush.sweepGradient(listOf(color, color.copy(alpha = 0.3f), color)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = info.achievement.name.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Diálogo que exibe os detalhes de uma conquista específica.
 */
@Composable
fun AchievementDetailDialog(info: AchievementUiInfo, onDismiss: () -> Unit) {
    val icon = if (info.isUnlocked) getIconForAchievement(info.achievement.id) else Icons.Default.Lock
    val titleColor = if (info.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val alpha = if (info.isUnlocked) 1f else 0.6f
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp).alpha(alpha), tint = titleColor) },
        title = { Text(info.achievement.name, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = titleColor) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(info.achievement.description, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                if (info.isUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("+${info.achievement.points} pts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    info.unlockedDate?.let {
                        Text("Conquistado em: ${dateFormatter.format(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Continue focado para desbloquear!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}

/**
 * Card de progresso visual de experiência (XP).
 */
@Composable
fun ActivityCard(userLevel: UserLevel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(text = "Caminho para o Nível ${userLevel.level + 1}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { userLevel.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${userLevel.currentXp} / ${userLevel.nextLevelXp} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
