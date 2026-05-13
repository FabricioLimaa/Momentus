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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlinx.coroutines.launch
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.ui.components.*
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.ui.viewmodel.DialogState
import br.com.fabriciolima.momentus.ui.viewmodel.EventsForDate
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: CalendarUiState,
    eventsForToday: EventsForDate,
    allCategories: List<Category>,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
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
    onDeleteSelectedRotinas: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val isTablet = isExpanded || isMedium

    // Bloqueio de orientação e detecção física para celulares
    DisposableEffect(isTablet) {
        val activity = context as? Activity
        var listener: OrientationEventListener? = null

        if (!isTablet && !view.isInEditMode) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            listener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return
                    val isTiltedHorizontal = (orientation in 70..110) || (orientation in 250..290)
                    if (isTiltedHorizontal) {
                        scope.launch {
                            if (snackbarHostState.currentSnackbarData == null) {
                                snackbarHostState.showSnackbar(
                                    message = "Momentus: Otimizado para o modo retrato no seu celular! ✨",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            }
            listener.enable()
        }

        onDispose {
            listener?.disable()
            if (!isTablet && !view.isInEditMode) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // 1. Lógica de Diálogos (Necessária para que o FAB funcione nesta tela)
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
                title = { Text("Excluir Rotina") },
                text = { Text("Tem certeza que deseja excluir esta rotina?") },
                confirmButton = {
                    Button(onClick = { onDeleteRotina(dialogState.rotina) }) { Text("Excluir") }
                },
                dismissButton = {
                    TextButton(onClick = onDialogDismiss) { Text("Cancelar") }
                }
            )
        }
        is DialogState.ConfirmDeleteMultiple -> {
            AlertDialog(
                onDismissRequest = onDialogDismiss,
                title = { Text("Excluir Rotinas") },
                text = { Text("Tem certeza que deseja excluir as ${dialogState.count} rotinas selecionadas?") },
                confirmButton = {
                    Button(onClick = { onDeleteSelectedRotinas() }) { Text("Excluir") }
                },
                dismissButton = {
                    TextButton(onClick = onDialogDismiss) { Text("Cancelar") }
                }
            )
        }
        else -> {}
    }

    // Cálculo dinâmico do progresso diário
    val completionsToday = eventsForToday.localRotinas.count { uiState.completedHabitIds.contains(it.id) }
    val totalToday = eventsForToday.localRotinas.size
    val progress = if (totalToday > 0) completionsToday.toFloat() / totalToday.toFloat() else 0f

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Nova Rotina")
                    }
                }
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
