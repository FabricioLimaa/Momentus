package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.components.EventDetailDialog
import br.com.fabriciolima.momentus.ui.components.EventListItem
import br.com.fabriciolima.momentus.ui.components.NewEventDialog
import br.com.fabriciolima.momentus.ui.components.SuccessCelebration
import br.com.fabriciolima.momentus.ui.components.UpdateAvailableDialog
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.ui.viewmodel.DialogState
import br.com.fabriciolima.momentus.ui.viewmodel.EventsForDate
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.play.core.install.model.InstallStatus
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    allCategories: List<Category>,
    eventsForSelectedDate: EventsForDate,
    installStatus: Int,
    showCompletionAnimation: Flow<Unit>,
    account: GoogleSignInAccount?,
    onNavigateToAchievements: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMenuClick: () -> Unit,
    onAddNewRotinaClicked: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSaveRotina: (String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onUpdateRotina: (ItemCronograma, String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onShowDetailClicked: (ItemCronograma) -> Unit,
    onEditRotinaClicked: (ItemCronograma) -> Unit,
    onConfirmDeleteClicked: (ItemCronograma) -> Unit,
    onDeleteRotina: (ItemCronograma) -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit,
    onErrorShown: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onAchievementDialogDismissed: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onStartUpdate: (com.google.android.play.core.appupdate.AppUpdateInfo) -> Unit,
    onCompleteUpdate: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onRotinaLongPressed: (String) -> Unit,
    onRotinaClicked: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onConfirmDeleteSelectedRotinas: () -> Unit,
    onDeleteSelectedRotinas: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessCelebration by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val selectionColor = MaterialTheme.colorScheme.primary.toArgb() // Use Primary em vez de Container para o StatusBar

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (uiState.isSelectionModeActive) {
                window.statusBarColor = selectionColor
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // Sempre claro no primary escuro
            } else {
                window.statusBarColor = surfaceColor
                 WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    LaunchedEffect(key1 = showCompletionAnimation) {
        showCompletionAnimation.collect {
            showSuccessCelebration = true
        }
    }

    LaunchedEffect(Unit) {
        onCheckForUpdate()
    }

    uiState.updateInfo?.let {
        UpdateAvailableDialog(
            onUpdateClick = {
                onStartUpdate(it)
                onDismissUpdateDialog()
            },
            onDismiss = onDismissUpdateDialog
        )
    }

    LaunchedEffect(installStatus) {
        if (installStatus == InstallStatus.DOWNLOADED) {
            snackbarHostState.showSnackbar(
                message = "Atualização pronta para instalar.",
                actionLabel = "REINICIAR",
                duration = SnackbarDuration.Indefinite
            )
            onCompleteUpdate()
        }
    }

    uiState.newlyUnlockedAchievement?.let { achievement ->
        LaunchedEffect(achievement.id) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        
        AchievementUnlockedDialog(
            achievement = achievement,
            onDismiss = onAchievementDialogDismissed
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = error.message ?: error.messageResId?.let { view.context.getString(it) } ?: "Erro desconhecido"
            snackbarHostState.showSnackbar(message = message)
            onErrorShown()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            onSuccessMessageShown()
        }
    }

    when (val dialogState = uiState.dialogState) {
        is DialogState.AddNewRotina -> {
            NewEventDialog(
                eventoParaEditar = null,
                selectedDate = selectedDate,
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
                selectedDate = selectedDate,
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
                icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = "Aviso de Exclusão") },
                title = { Text("Excluir Rotina") },
                text = { Text("Tem certeza que deseja excluir a rotina \"${dialogState.rotina.titulo}\"? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = { onDeleteRotina(dialogState.rotina) }) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDialogDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }
         is DialogState.ConfirmDeleteMultiple -> {
            AlertDialog(
                onDismissRequest = onDialogDismiss,
                icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = "Aviso de Exclusão Múltipla") },
                title = { Text("Excluir Rotinas") },
                text = { Text("Tem certeza que deseja excluir as ${dialogState.count} rotinas selecionadas? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = { onDeleteSelectedRotinas() }) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDialogDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }
        is DialogState.Hidden -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                } else {
                    TopAppBar(
                        title = { Text(text = "Minha Agenda") },
                        navigationIcon = {
                            IconButton(onClick = { onMenuClick() }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToAchievements() }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Sequência",
                                    tint = getStreakColor(uiState.streak)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.streak.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pontos",
                                    tint = Color(0xFFD4AF37)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = (uiState.userData?.points ?: 0).toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                uiState.updateProgress?.let { progress ->
                    if (progress.totalBytesToDownload > 0) {
                        val percentage = (progress.bytesDownloaded * 100 / progress.totalBytesToDownload).toInt()
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = "Baixando atualização: $percentage%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress.bytesDownloaded.toFloat() / progress.totalBytesToDownload.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp) 
                        .align(Alignment.CenterHorizontally)
                        .weight(1f)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAddNewRotinaClicked,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !uiState.isLoading && !uiState.isSelectionModeActive
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nova Rotina", fontSize = 16.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    CalendarContent(
                        uiState = uiState,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1.5f) // Maior peso para o calendário
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.weight(1f)) {
                        EventsForDay(
                            uiState = uiState,
                            selectedDate = selectedDate,
                            eventsForDate = eventsForSelectedDate,
                            onRotinaClick = { item ->
                                 if (uiState.isSelectionModeActive) onRotinaClicked(item.id) else onShowDetailClicked(item)
                            },
                            onRotinaLongClick = { item -> onRotinaLongPressed(item.id) },
                            onAddNewRotinaClicked = onAddNewRotinaClicked,
                            onMarkAsCompleted = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMarkAsCompleted(id)
                            },
                            onUnmarkAsCompleted = onUnmarkAsCompleted
                        )
                    }
                }
            }
        }

        if (uiState.isLoading && !uiState.isSelectionModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (showSuccessCelebration) {
            SuccessCelebration(onFinished = { showSuccessCelebration = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selecionadas") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Selecionar Todas")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir Selecionadas")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun AchievementUnlockedDialog(achievement: Achievement, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Box(contentAlignment = Alignment.Center) {
                SuccessCelebration(onFinished = {})
                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text("Conquista Desbloqueada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(achievement.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(achievement.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+${achievement.points} pontos", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar")
            }
        }
    )
}

@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusYears(100) }
    val endMonth = remember { currentMonth.plusYears(100) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = remember { YearMonth.from(selectedDate) },
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid // Fixa o calendário em 6 linhas
    )

    val scope = rememberCoroutineScope()
    val visibleMonth by remember { derivedStateOf { calendarState.firstVisibleMonth.yearMonth } }

    LaunchedEffect(selectedDate) {
        scope.launch {
            calendarState.animateScrollToMonth(YearMonth.from(selectedDate))
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = visibleMonth,
            onDismiss = { showMonthYearPicker = false },
            onMonthYearSelected = {
                scope.launch {
                    calendarState.scrollToMonth(it)
                }
                showMonthYearPicker = false
            }
        )
    }

    Column(modifier = modifier) {
        CalendarHeader(
            month = visibleMonth,
            onPreviousMonth = {
                scope.launch {
                    calendarState.animateScrollToMonth(visibleMonth.minusMonths(1))
                }
            },
            onNextMonth = {
                scope.launch {
                    calendarState.animateScrollToMonth(visibleMonth.plusMonths(1))
                }
            },
            onTitleClick = { showMonthYearPicker = true }
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val localScheduleByDate = uiState.allScheduleItems.groupBy {
            it.data?.let { instant -> Instant.ofEpochMilli(instant).atZone(ZoneOffset.UTC).toLocalDate() }
        }

        HorizontalCalendar(
            modifier = Modifier.fillMaxWidth(),
            state = calendarState,
            dayContent = { day ->
                DayCell(
                    day = day,
                    isSelected = selectedDate == day.date,
                    localRotinas = localScheduleByDate[day.date] ?: emptyList(),
                    categoriesMap = uiState.categoriesMap,
                    onDateSelected = { onDateSelected(it.date) }
                )
            }
        )
    }
}

@Composable
fun CalendarHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
        Text(
            modifier = Modifier.clickable(onClick = onTitleClick),
            text = month.format(monthFormatter).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row {
            IconButton(onClick = onPreviousMonth) {
                Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Mês Anterior")
            }
            IconButton(onClick = onNextMonth) {
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Próximo Mês")
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    localRotinas: List<ItemCronograma>,
    categoriesMap: Map<String, Category>,
    onDateSelected: (CalendarDay) -> Unit
) {
    val today = LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == today

    val textColor = if (isSelected && isCurrentMonth) {
        MaterialTheme.colorScheme.onPrimary
    } else if (isToday && isCurrentMonth) {
        MaterialTheme.colorScheme.primary
    } else if (isCurrentMonth) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    val backgroundColor = if (isSelected && isCurrentMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isToday && !isSelected && isCurrentMonth) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else Modifier
            )
            .clickable(
                enabled = isCurrentMonth,
                onClick = { onDateSelected(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            if (isCurrentMonth && !isSelected) {
                if (localRotinas.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(6.dp)
                    ) {
                        localRotinas.take(2).forEach { rotina ->
                            val category = categoriesMap[rotina.categoryId]
                            val rotinaColor = category?.cor?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary } }
                                ?: MaterialTheme.colorScheme.secondary
                            Box(modifier = Modifier.size(4.dp).background(rotinaColor, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthYearSelected: (YearMonth) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selecione Mês e Ano", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Ano anterior")
                    }
                    Text(text = selectedYear.toString(), style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Próximo ano")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(12) { month ->
                        val monthName = YearMonth.of(selectedYear, month + 1).month.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (currentMonth.year == selectedYear && currentMonth.monthValue == month + 1) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { onMonthYearSelected(YearMonth.of(selectedYear, month + 1)) }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = monthName.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EventsForDay(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    eventsForDate: EventsForDate,
    onRotinaClick: (ItemCronograma) -> Unit,
    onRotinaLongClick: (ItemCronograma) -> Unit,
    onAddNewRotinaClicked: () -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit
) {
    val ptBr = Locale("pt", "BR")
    val dateFormatter = remember(selectedDate) {
        DateTimeFormatter.ofPattern("dd 'de' MMMM", ptBr)
    }
    val formattedDate = selectedDate.format(dateFormatter)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))

        if (eventsForDate.localRotinas.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dia livre!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Você não tem nenhuma rotina para este dia. Que tal adicionar uma?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAddNewRotinaClicked, enabled = !uiState.isSelectionModeActive) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Rotina")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(eventsForDate.localRotinas, key = { rotina -> rotina.id }) { rotina ->
                    val category = uiState.categoriesMap[rotina.categoryId]
                    if (category != null) {
                        val isChecked = uiState.completedHabitIds.contains(rotina.id)
                        val isSelected = uiState.selectedRotinaIds.contains(rotina.id)

                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onRotinaClick(rotina) },
                                onLongClick = { onRotinaLongClick(rotina) }
                            ),
                            colors = if(isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)) else CardDefaults.cardColors()
                        ) {
                             EventListItem(
                                item = rotina,
                                category = category,
                                isChecked = isChecked,
                                showCheckbox = uiState.isSelectionModeActive,
                                isSelected = isSelected,
                                onCheckedChange = { isNowChecked ->
                                    if (isNowChecked) onMarkAsCompleted(rotina.id) else onUnmarkAsCompleted(rotina.id)
                                },
                                onCardClicked = { onRotinaClick(rotina) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getStreakColor(streakCount: Int): Color {
    return when {
        streakCount >= 30 -> Color(0xFF6A1B9A) // Roxo
        streakCount >= 7 -> Color(0xFFD32F2F) // Vermelho
        streakCount >= 3 -> Color(0xFFFFA000) // Laranja
        else -> Color.Gray
    }
}
