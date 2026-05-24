package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlinx.coroutines.launch
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.components.*
import br.com.fabriciolima.momentus.ui.theme.*
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.ui.viewmodel.DialogState
import br.com.fabriciolima.momentus.ui.viewmodel.EventsForDate
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.outlined.Warning
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: CalendarUiState,
    eventsForToday: EventsForDate,
    allCategories: List<Category>,
    windowSizeClass: WindowSizeClass,
    bottomBarPadding: Dp = 0.dp,
    onNavigateToAchievements: () -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit,
    onAddNewRotinaClicked: () -> Unit,
    onSaveRotina: (String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onUpdateRotina: (ItemCronograma, String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onDeleteRotina: (ItemCronograma) -> Unit,
    onShowDetailClicked: (ItemCronograma) -> Unit,
    onEditRotinaClicked: (ItemCronograma) -> Unit,
    onConfirmDeleteClicked: (ItemCronograma) -> Unit,
    onDialogDismiss: () -> Unit,
    onRotinaLongPressed: (String) -> Unit,
    onRotinaClicked: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onConfirmDeleteSelectedRotinas: () -> Unit,
    onDeleteSelectedRotinas: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        // Observar mensagens do ViewModel
        LaunchedEffect(uiState.successMessage, uiState.error) {
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                onSuccessMessageShown()
            }
            uiState.error?.let {
                snackbarHostState.showSnackbar(it.message ?: "Erro desconhecido")
                onErrorShown()
            }
        }

        // Cálculo de progresso do dia
        val completionsToday = eventsForToday.localRotinas.count { uiState.completedHabitIds.contains(it.id) }
        val totalToday = eventsForToday.localRotinas.size
        val progress = if (totalToday > 0) completionsToday.toFloat() / totalToday.toFloat() else 0f

        // 1. Lógica de Diálogos
        when (val dialogState = uiState.dialogState) {
            is DialogState.AddNewRotina -> {
                NewEventDialog(
                    selectedDate = LocalDate.now(),
                    categories = allCategories,
                    onDismiss = onDialogDismiss,
                    onConfirm = { _, titulo, descricao, data, inicio, fim, category, salvarNoGoogle ->
                        onSaveRotina(titulo, descricao, data, inicio, fim, category, salvarNoGoogle)
                    }
                )
            }
            is DialogState.EditRotina -> {
                NewEventDialog(
                    eventoParaEditar = dialogState.rotina,
                    selectedDate = LocalDate.now(),
                    categories = allCategories,
                    onDismiss = onDialogDismiss,
                    onConfirm = { item, titulo, descricao, data, inicio, fim, category, salvarNoGoogle ->
                        if (item != null) {
                            onUpdateRotina(item, titulo, descricao, data, inicio, fim, category, salvarNoGoogle)
                        }
                    }
                )
            }
            is DialogState.ShowDetail -> {
                val category = uiState.categoriesMap[dialogState.rotina.categoryId]
                if (category != null) {
                    EventDetailDialog(
                        event = dialogState.rotina,
                        category = category,
                        onDismiss = onDialogDismiss,
                        onEditClick = { onEditRotinaClicked(dialogState.rotina) },
                        onDeleteClick = { onConfirmDeleteClicked(dialogState.rotina) }
                    )
                }
            }
            is DialogState.ConfirmDelete -> {
                AlertDialog(
                    onDismissRequest = onDialogDismiss,
                    icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir Rotina", fontWeight = FontWeight.Black) },
                    text = { Text("Deseja realmente remover esta rotina? Esta ação não poderá ser desfeita.") },
                    confirmButton = {
                        Button(
                            onClick = { onDeleteRotina(dialogState.rotina) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Excluir", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = onDialogDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            is DialogState.ConfirmDeleteMultiple -> {
                AlertDialog(
                    onDismissRequest = onDialogDismiss,
                    icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir Selecionadas", fontWeight = FontWeight.Black) },
                    text = { Text("Tem certeza que deseja excluir as ${dialogState.count} rotinas selecionadas?") },
                    confirmButton = {
                        Button(
                            onClick = { onDeleteSelectedRotinas() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Excluir Todas", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = onDialogDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            else -> {}
        }

        Scaffold(
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    PremiumSnackbar(data)
                }
            },
            topBar = {
                if (uiState.isSelectionModeActive) {
                    SelectionTopAppBar(
                        selectedCount = uiState.selectedRotinaIds.size,
                        onClearSelection = onClearSelection,
                        onSelectAll = onSelectAll,
                        onDelete = onConfirmDeleteSelectedRotinas
                    )
                }
            },
            floatingActionButton = {
                if (!uiState.isSelectionModeActive) {
                    FloatingActionButton(
                        onClick = onAddNewRotinaClicked,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = bottomBarPadding)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nova Rotina"
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = bottomBarPadding + 32.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))

                    // 1. Header de Saudação (Mockup 3)
                    val userName = uiState.userData?.displayName?.split(" ")?.firstOrNull() ?: "Explorador"
                    HomeHeader(
                        userName = userName,
                        onNotificationsClick = { /* TODO */ }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. Card de Streak & Progresso Circular (Mockup 3)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAchievements() }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Sequência Atual",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = getStreakColor(uiState.streak),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${uiState.streak} dias",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = when {
                                        totalToday == 0 -> "Dia livre! Que tal planejar algo novo? ✨"
                                        completionsToday == 0 -> "Vamos começar? O primeiro passo! 🚀"
                                        progress < 0.5f -> "Ótimo começo! Continue focado. 💪"
                                        progress < 1.0f -> "Mais da metade concluído! Você está quase lá. ⚡"
                                        else -> "Dia perfeito! Você conquistou todas as suas metas! 🏆"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(80.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 3. Seção "Seu dia" com Timeline Vertical (Mockup 3)
                    Text(
                        text = "Seu dia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (eventsForToday.localRotinas.isEmpty()) {
                    item {
                        HomeEmptyState()
                    }
                } else {
                    val sortedRotinas = eventsForToday.localRotinas.sortedBy { it.horarioInicio }
                    itemsIndexed(sortedRotinas) { index, rotina ->
                        val category = uiState.categoriesMap[rotina.categoryId]
                        if (category != null) {
                            TimelineEventItem(
                                item = rotina,
                                category = category,
                                isChecked = uiState.completedHabitIds.contains(rotina.id),
                                isFirst = index == 0,
                                isLast = index == sortedRotinas.size - 1,
                                isSelected = uiState.selectedRotinaIds.contains(rotina.id),
                                isSelectionMode = uiState.isSelectionModeActive,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) onMarkAsCompleted(rotina.id) else onUnmarkAsCompleted(rotina.id)
                                },
                                onClick = { 
                                    if (uiState.isSelectionModeActive) onRotinaClicked(rotina.id) 
                                    else onShowDetailClicked(rotina) 
                                },
                                onLongClick = { onRotinaLongPressed(rotina.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bom dia,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$userName 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificações",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HomeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nada agendado para hoje",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Toque no + para começar sua jornada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
