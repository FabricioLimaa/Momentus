package br.com.fabriciolima.momentus.ui.screens.achievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.ui.screens.getStreakColor
import br.com.fabriciolima.momentus.ui.viewmodel.AchievementUiInfo
import br.com.fabriciolima.momentus.ui.viewmodel.AchievementsUiState
import br.com.fabriciolima.momentus.ui.viewmodel.AchievementsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun getIconForAchievement(achievementId: String): ImageVector {
    return when (achievementId) {
        "first_habit" -> Icons.Default.CheckCircle
        "ten_habits" -> Icons.Default.DoneAll
        "fifty_habits" -> Icons.Default.WorkspacePremium
        "hundred_habits" -> Icons.Default.MilitaryTech
        "streak_3" -> Icons.Default.LocalFireDepartment
        "streak_7" -> Icons.Default.FitnessCenter
        "streak_30" -> Icons.Default.School
        "early_bird" -> Icons.Default.Egg
        "night_owl" -> Icons.Default.Hotel
        "morning_person" -> Icons.Default.Coffee
        else -> Icons.Default.EmojiEvents
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.selectedAchievement?.let {
        AchievementDetailDialog(
            info = it,
            onDismiss = { viewModel.onDialogDismiss() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas e Progresso") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StreakAndPointsCard(uiState = uiState)
            }
            item {
                ProgressSummary(unlocked = uiState.unlockedCount, total = uiState.totalCount)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(uiState.achievements) { achievementInfo ->
                AchievementCard(
                    info = achievementInfo,
                    onClick = { viewModel.onAchievementClicked(achievementInfo) } 
                )
            }
        }
    }
}

@Composable
fun AchievementDetailDialog(info: AchievementUiInfo, onDismiss: () -> Unit) {
    val icon = if (info.isUnlocked) getIconForAchievement(info.achievement.id) else Icons.Default.Lock
    val titleColor = if (info.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val alpha = if (info.isUnlocked) 1f else 0.6f
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp).alpha(alpha), tint = titleColor) },
        title = { Text(info.achievement.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = titleColor) },
        text = {
            Column(modifier = Modifier.alpha(alpha)) {
                Text(info.achievement.description, style = MaterialTheme.typography.bodyLarge)
                if (info.isUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("+${info.achievement.points} pontos", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    info.unlockedDate?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Conquistado em: ${dateFormatter.format(it)}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun StreakAndPointsCard(uiState: AchievementsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Ícone de Sequência",
                    tint = getStreakColor(streakCount = uiState.streakCount),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.streakCount} dias",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Sequência", style = MaterialTheme.typography.bodyMedium)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Ícone de Pontos",
                    tint = Color(0xFFD4AF37), // Dourado
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.points.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Pontos", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Composable
private fun ProgressSummary(unlocked: Int, total: Int) {
    val progress = if (total > 0) unlocked.toFloat() / total else 0f

    Column {
        Text(
            text = "Conquistas Desbloqueadas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Progresso", style = MaterialTheme.typography.bodyMedium)
            Text(text = "$unlocked / $total", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AchievementCard(info: AchievementUiInfo, onClick: () -> Unit) {
    val cardAlpha = if (info.isUnlocked) 1f else 0.6f
    val icon = if (info.isUnlocked) getIconForAchievement(info.achievement.id) else Icons.Default.Lock
    val iconColor = if (info.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (info.isUnlocked) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (info.isUnlocked) "Conquista Desbloqueada" else "Conquista Bloqueada",
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = info.achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = info.achievement.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (info.isUnlocked) {
                Text(
                    text = "+${info.achievement.points} pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
