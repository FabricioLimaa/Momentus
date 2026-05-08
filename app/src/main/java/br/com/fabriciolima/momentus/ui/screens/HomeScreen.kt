package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.components.DashboardHeader
import br.com.fabriciolima.momentus.ui.components.TimelineEventItem
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.ui.viewmodel.EventsForDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: CalendarUiState,
    eventsForToday: EventsForDate,
    onNavigateToAchievements: () -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit,
    onAddNewRotinaClicked: () -> Unit,
    onShowDetailClicked: (ItemCronograma) -> Unit
) {
    // Cálculo dinâmico do progresso diário
    val completionsToday = eventsForToday.localRotinas.count { uiState.completedHabitIds.contains(it.id) }
    val totalToday = eventsForToday.localRotinas.size
    val progress = if (totalToday > 0) completionsToday.toFloat() / totalToday.toFloat() else 0f

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewRotinaClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Rotina")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Dashboard Header
            item {
                val userName = uiState.userData?.displayName?.split(" ")?.firstOrNull() ?: "Usuário"
                DashboardHeader(
                    userName = userName,
                    streakCount = uiState.streak,
                    progress = progress,
                    onNavigateToAchievements = onNavigateToAchievements
                )
            }

            // 2. Seção "Seu dia"
            item {
                Text(
                    text = "Seu dia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 3. Lista de Rotinas em Timeline
            if (eventsForToday.localRotinas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma rotina para hoje.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(eventsForToday.localRotinas) { index, rotina ->
                    val category = uiState.categoriesMap[rotina.categoryId]
                    if (category != null) {
                        TimelineEventItem(
                            item = rotina,
                            category = category,
                            isChecked = uiState.completedHabitIds.contains(rotina.id),
                            isFirst = index == 0,
                            isLast = index == eventsForToday.localRotinas.size - 1,
                            onCheckedChange = { isChecked ->
                                if (isChecked) onMarkAsCompleted(rotina.id) else onUnmarkAsCompleted(rotina.id)
                            },
                            onClick = { onShowDetailClicked(rotina) }
                        )
                    }
                }
            }
        }
    }
}
