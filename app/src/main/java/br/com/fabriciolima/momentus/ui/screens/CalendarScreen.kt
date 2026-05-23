package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.components.*
import br.com.fabriciolima.momentus.ui.theme.*
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    allCategories: List<Category>,
    eventsForSelectedDate: EventsForDate,
    installStatus: Int,
    showCompletionAnimation: Flow<Unit>,
    account: GoogleSignInAccount?,
    windowSizeClass: WindowSizeClass,
    bottomBarPadding: Dp = 0.dp,
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
    onDeleteSelectedRotinas: () -> Unit,
    onUnmarkAsCompleted: (String) -> Unit,
    onErrorShown: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onAchievementDialogDismissed: () -> Unit,
    onCheckForAppUpdate: () -> Unit,
    onStartUpdate: (com.google.android.play.core.appupdate.AppUpdateInfo) -> Unit,
    onCompleteUpdate: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onRotinaLongPressed: (String) -> Unit,
    onRotinaClicked: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onConfirmDeleteSelectedRotinas: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        val scope = rememberCoroutineScope()
        var showSuccessCelebration by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current

        val darkTheme = isSystemInDarkTheme()
        val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
        val selectionColor = MaterialTheme.colorScheme.primary.toArgb()

        val activity = androidx.compose.ui.platform.LocalContext.current as Activity

        val configuration = LocalConfiguration.current
        val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val useSidePanel = isExpanded || (isMedium && isLandscape)

        if (!androidx.compose.ui.platform.LocalView.current.isInEditMode) {
            SideEffect {
                val window = activity.window
                if (uiState.isSelectionModeActive) {
                    window.statusBarColor = selectionColor
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                } else {
                    window.statusBarColor = surfaceColor
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
                }
            }
        }

        LaunchedEffect(key1 = showCompletionAnimation) {
            showCompletionAnimation.collect { showSuccessCelebration = true }
        }

        LaunchedEffect(Unit) { onCheckForAppUpdate() }

        uiState.updateInfo?.let {
            UpdateAvailableDialog(
                onUpdateClick = { onStartUpdate(it); onDismissUpdateDialog() },
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
            LaunchedEffect(achievement.id) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            AchievementUnlockedDialog(achievement = achievement, onDismiss = onAchievementDialogDismissed)
        }

        LaunchedEffect(uiState.error) {
            uiState.error?.let { error ->
                val activityContext = activity
                val message = error.message ?: error.messageResId?.let { activityContext.getString(it) } ?: "Erro desconhecido"
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

        if (!useSidePanel) {
            when (val dialogState = uiState.dialogState) {
                is DialogState.AddNewRotina -> {
                    NewEventDialog(
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
                else -> {}
            }
        }

        when (val dialogState = uiState.dialogState) {
            is DialogState.ConfirmDelete -> {
                AlertDialog(
                    onDismissRequest = onDialogDismiss,
                    icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir Rotina", fontWeight = FontWeight.Black) },
                    text = { Text("Deseja realmente remover a rotina \"${dialogState.rotina.titulo}\"? Esta ação não poderá ser desfeita.") },
                    confirmButton = { 
                        Button(
                            onClick = { onDeleteRotina(dialogState.rotina) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Excluir", fontWeight = FontWeight.Bold) } 
                    },
                    dismissButton = { 
                        TextButton(onClick = onDialogDismiss) { 
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        } 
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            is DialogState.ConfirmDeleteMultiple -> {
                AlertDialog(
                    onDismissRequest = onDialogDismiss,
                    icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir Selecionadas", fontWeight = FontWeight.Black) },
                    text = { Text("Tem certeza que deseja excluir as ${dialogState.count} rotinas selecionadas?") },
                    confirmButton = { 
                        Button(
                            onClick = { onDeleteSelectedRotinas() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Excluir Todas", fontWeight = FontWeight.Bold) } 
                    },
                    dismissButton = { 
                        TextButton(onClick = onDialogDismiss) { 
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        } 
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            else -> {}
        }

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
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = bottomBarPadding)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), 
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Add,
                                contentDescription = "Nova Rotina")
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    if (useSidePanel) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                                CalendarContent(
                                    uiState = uiState,
                                    selectedDate = selectedDate,
                                    onDateSelected = onDateSelected
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                EventsForDay(
                                    uiState = uiState,
                                    selectedDate = selectedDate,
                                    eventsForDate = eventsForSelectedDate,
                                    bottomBarPadding = bottomBarPadding,
                                    onRotinaClick = { item ->
                                        if (uiState.isSelectionModeActive) onRotinaClicked(item.id) else onShowDetailClicked(item)
                                    },
                                    onRotinaLongClick = { item -> onRotinaLongPressed(item.id) },
                                    onMarkAsCompleted = { id ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onMarkAsCompleted(id)
                                    },
                                    onUnmarkAsCompleted = onUnmarkAsCompleted
                                )
                            }
                        }
                    } else {
                        CalendarContent(
                            uiState = uiState,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            EventsForDay(
                                uiState = uiState,
                                selectedDate = selectedDate,
                                eventsForDate = eventsForSelectedDate,
                                bottomBarPadding = bottomBarPadding,
                                onRotinaClick = { item ->
                                    if (uiState.isSelectionModeActive) onRotinaClicked(item.id) else onShowDetailClicked(item)
                                },
                                onRotinaLongClick = { item -> onRotinaLongPressed(item.id) },
                                onMarkAsCompleted = { id ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMarkAsCompleted(id)
                                },
                                onUnmarkAsCompleted = onUnmarkAsCompleted
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = useSidePanel && uiState.dialogState != DialogState.Hidden,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Surface(
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))) {
                            when (val dialogState = uiState.dialogState) {
                                is DialogState.ShowDetail -> {
                                    val category = uiState.categoriesMap[dialogState.rotina.categoryId]
                                    if (category != null) {
                                        EventDetailContent(
                                            event = dialogState.rotina,
                                            category = category,
                                            onEditClick = { onEditRotinaClicked(dialogState.rotina) },
                                            onDeleteClick = { onConfirmDeleteClicked(dialogState.rotina) },
                                            onCloseClick = onDialogDismiss,
                                            showCloseButton = true
                                        )
                                    }
                                }
                                is DialogState.AddNewRotina -> {
                                    NewEventContent(
                                        selectedDate = selectedDate,
                                        categories = allCategories,
                                        onDismiss = onDialogDismiss,
                                        onConfirm = { _, titulo, descricao, data, inicio, fim, category, salvarNoGoogle ->
                                            onSaveRotina(titulo, descricao, data, inicio, fim, category, salvarNoGoogle)
                                        }
                                    )
                                }
                                is DialogState.EditRotina -> {
                                    NewEventContent(
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
                                else -> {}
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading && !uiState.isSelectionModeActive) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            }

            if (showSuccessCelebration) {
                SuccessCelebration(onFinished = { showSuccessCelebration = false })
            }
        }
    }
}

@Composable
fun AchievementUnlockedDialog(achievement: Achievement, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(contentAlignment = Alignment.Center) {
                SuccessCelebration(onFinished = {})
                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text("Conquista Desbloqueada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(achievement.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(achievement.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text("+${achievement.points} pontos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text("Continuar", fontWeight = FontWeight.Bold)
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
    val startMonth = remember { currentMonth.minusYears(10) }
    val endMonth = remember { currentMonth.plusYears(10) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = remember { YearMonth.from(selectedDate) },
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid
    )

    val scope = rememberCoroutineScope()
    val visibleMonth by remember { derivedStateOf { calendarState.firstVisibleMonth.yearMonth } }

    LaunchedEffect(selectedDate) {
        scope.launch {
            calendarState.animateScrollToMonth(YearMonth.from(selectedDate))
        }
    }

    Column(modifier = modifier) {
        CalendarHeader(
            month = visibleMonth,
            onPreviousMonth = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.minusMonths(1)) } },
            onNextMonth = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.plusMonths(1)) } }
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).uppercase(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        val localScheduleByDate = uiState.allScheduleItems.groupBy {
            it.data?.let { instant -> Instant.ofEpochMilli(instant).atZone(ZoneOffset.UTC).toLocalDate() }
        }

        HorizontalCalendar(
            modifier = Modifier.fillMaxWidth(),
            state = calendarState,
            dayContent = { day ->
                val dayEvents = uiState.allScheduleItems.filter {
                    it.data?.let { instant -> Instant.ofEpochMilli(instant).atZone(ZoneOffset.UTC).toLocalDate() == day.date } ?: false
                }.sortedBy { it.horarioInicio }
                
                val colors = dayEvents.take(3).map { item ->
                    val colorHex = uiState.categoriesMap[item.categoryId]?.cor ?: "#CCCCCC"
                    try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                }

                DayCell(
                    day = day,
                    isSelected = selectedDate == day.date,
                    isToday = day.date == LocalDate.now(),
                    eventColors = colors,
                    hasMoreEvents = dayEvents.size > 3,
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
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))

        // Título do Mês com estilo correto
        Text(
            text = month.format(monthFormatter).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Botões de navegação simplificados e alinhados
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.ChevronLeft, "Anterior", tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.ChevronRight, "Próximo", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    eventColors: List<Color>,
    hasMoreEvents: Boolean,
    onDateSelected: (CalendarDay) -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate

    val textColor = when {
        isSelected && isCurrentMonth -> MaterialTheme.colorScheme.onPrimary
        isToday && isCurrentMonth -> MaterialTheme.colorScheme.primary
        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }

    val backgroundColor = if (isSelected && isCurrentMonth) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isToday && !isSelected && isCurrentMonth) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(enabled = isCurrentMonth, onClick = { onDateSelected(day) }),
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
                fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium
            )
            
            if (eventColors.isNotEmpty() && isCurrentMonth) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    eventColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else color)
                        )
                    }
                    if (hasMoreEvents) {
                        Text(
                            text = "+",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventsForDay(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    eventsForDate: EventsForDate,
    bottomBarPadding: Dp = 0.dp,
    onRotinaClick: (ItemCronograma) -> Unit,
    onRotinaLongClick: (ItemCronograma) -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit
) {
    val ptBr = Locale("pt", "BR")
    val dayOfMonth = selectedDate.dayOfMonth
    val monthName = selectedDate.month.getDisplayName(TextStyle.FULL, ptBr)
    val dayOfWeek = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, ptBr).replaceFirstChar { it.titlecase(ptBr) }

    val formattedDate = "$dayOfMonth de $monthName"

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (eventsForDate.localRotinas.isNotEmpty()) {
                Text(
                    text = "${eventsForDate.localRotinas.size} itens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (eventsForDate.localRotinas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = bottomBarPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Dia livre! Aproveite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                contentPadding = PaddingValues(bottom = bottomBarPadding + 40.dp)
            ) {
                val sortedRotinas = eventsForDate.localRotinas.sortedBy { it.horarioInicio }
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
                            onClick = { onRotinaClick(rotina) },
                            onLongClick = { onRotinaLongClick(rotina) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getStreakColor(streakCount: Int): Color {
    return when {
        streakCount >= 30 -> Color(0xFF6A1B9A)
        streakCount >= 7 -> Color(0xFFD32F2F)
        streakCount >= 3 -> Color(0xFFFFA000)
        else -> Color.Gray
    }
}
