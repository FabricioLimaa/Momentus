package br.com.fabriciolima.momentus.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.ui.components.EventDetailDialog
import br.com.fabriciolima.momentus.ui.components.EventListItem
import br.com.fabriciolima.momentus.ui.components.NewEventDialog
import br.com.fabriciolima.momentus.ui.components.UpdateAvailableDialog
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.*
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import br.com.fabriciolima.momentus.widget.EVENT_ID_KEY
import br.com.fabriciolima.momentus.widget.OPEN_NEW_EVENT_DIALOG_KEY
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.play.core.install.model.InstallStatus
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
import javax.inject.Inject

@AndroidEntryPoint
class CalendarActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            MomentusTheme {
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                    } else {
                    }
                }

                LaunchedEffect(key1 = true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
                val eventsForSelectedDate by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
                val installStatus by viewModel.installStatus.collectAsStateWithLifecycle()
                val account = GoogleSignIn.getLastSignedInAccount(this)

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    viewModel.logoutEvent.collectLatest {
                        when (it) {
                            LogoutEvent.Success -> {
                                val intent = Intent(this@CalendarActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            userData = uiState.userData,
                            account = account,
                            showUpdateBadge = uiState.showUpdateBadge,
                            onNavigate = { scope.launch { drawerState.close() } },
                            onLogout = { viewModel.logout() },
                            onUpdatesClicked = viewModel::onUpdatesClicked
                        )
                    }
                ) {
                    CalendarScreen(
                        uiState = uiState,
                        selectedDate = selectedDate,
                        allCategories = allCategories,
                        eventsForSelectedDate = eventsForSelectedDate,
                        installStatus = installStatus,
                        account = account,
                        onDateSelected = viewModel::selectDate,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAddNewEventClicked = viewModel::onAddNewEventClicked,
                        onDialogDismiss = viewModel::onDialogDismiss,
                        onSaveEvent = viewModel::saveSingleEvent,
                        onUpdateEvent = viewModel::updateEvent,
                        onShowDetailClicked = viewModel::onShowDetailClicked,
                        onEditEventClicked = viewModel::onEditEventClicked,
                        onConfirmDeleteClicked = viewModel::onConfirmDeleteClicked,
                        onDeleteEvent = viewModel::deleteEvent,
                        onMarkAsCompleted = viewModel::markHabitAsCompleted,
                        onUnmarkAsCompleted = viewModel::unmarkHabitAsCompleted,
                        onErrorShown = viewModel::onErrorShown,
                        onSuccessMessageShown = viewModel::onSuccessMessageShown,
                        onAchievementDialogDismissed = viewModel::onAchievementDialogDismissed,
                        onCheckForUpdate = viewModel::checkForAppUpdate,
                        onStartUpdate = { updateInfo ->
                            inAppUpdateManager.startUpdateFlow(updateInfo, this)
                        },
                        onCompleteUpdate = inAppUpdateManager::completeUpdate,
                        onDismissUpdateDialog = viewModel::onUpdateDialogDismissed,
                        // Callbacks for multiple selection
                        onEventLongPressed = viewModel::onEventLongPressed,
                        onEventClicked = viewModel::onEventClicked,
                        onClearSelection = viewModel::onClearSelection,
                        onSelectAll = viewModel::onSelectAll,
                        onDeleteSelectedEvents = viewModel::deleteSelectedEvents,
                        onConfirmDeleteSelectedEvents = viewModel::confirmDeleteSelectedEvents
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra(OPEN_NEW_EVENT_DIALOG_KEY, false)) {
            viewModel.onAddNewEventClicked()
        } else {
            intent.getStringExtra(EVENT_ID_KEY)?.let {
                viewModel.showEventDetails(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchGoogleCalendarEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregisterListener()
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
        title = { Text("$selectedCount selecionados") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Selecionar Todos")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir Selecionados")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}


@Composable
fun UserAvatar(
    userData: UserData?,
    account: GoogleSignInAccount?,
    modifier: Modifier = Modifier
) {
    val photoUrl = account?.photoUrl
    val displayName = userData?.displayName ?: account?.displayName

    if (photoUrl != null) {
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
                ?.split(' ')
                ?.take(2)
                ?.mapNotNull { it.firstOrNull()?.uppercase() }
                ?.joinToString("")
                ?: "U"
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AchievementUnlockedDialog(achievement: Achievement, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(48.dp)) },
        title = { Text("Conquista Desbloqueada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(achievement.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(achievement.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("+${achievement.points} pontos", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Continuar")
            }
        }
    )
}

@Composable
fun getStreakColor(streakCount: Int): Color {
    return when {
        streakCount >= 30 -> Color(0xFF6A1B9A) // Roxo
        streakCount >= 7 -> Color(0xFFD32F2F) // Vermelho
        streakCount > 3 -> Color(0xFFFFA000) // Laranja
        else -> Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    allCategories: List<Category>,
    eventsForSelectedDate: EventsForDate,
    installStatus: Int,
    account: GoogleSignInAccount?,
    onDateSelected: (LocalDate) -> Unit,
    onMenuClick: () -> Unit,
    onAddNewEventClicked: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSaveEvent: (String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onUpdateEvent: (ItemCronograma, String, String?, LocalDate, LocalTime, LocalTime, Category, Boolean) -> Unit,
    onShowDetailClicked: (ItemCronograma) -> Unit,
    onEditEventClicked: (ItemCronograma) -> Unit,
    onConfirmDeleteClicked: (ItemCronograma) -> Unit,
    onDeleteEvent: (ItemCronograma) -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit,
    onErrorShown: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onAchievementDialogDismissed: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onStartUpdate: (com.google.android.play.core.appupdate.AppUpdateInfo) -> Unit,
    onCompleteUpdate: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    // Callbacks for multiple selection
    onEventLongPressed: (String) -> Unit,
    onEventClicked: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onConfirmDeleteSelectedEvents: () -> Unit,
    onDeleteSelectedEvents: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle Status Bar Color
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val selectionColor = MaterialTheme.colorScheme.primaryContainer
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (uiState.isSelectionModeActive) {
                window.statusBarColor = selectionColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            } else {
                window.statusBarColor = primaryColor.toArgb()
                 WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
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
        AchievementUnlockedDialog(
            achievement = achievement,
            onDismiss = onAchievementDialogDismissed
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it)
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
        is DialogState.AddNewEvent -> {
            NewEventDialog(
                eventoParaEditar = null,
                selectedDate = selectedDate,
                categories = allCategories,
                onDismiss = onDialogDismiss,
                onConfirm = { _, titulo, descricao, data, inicio, fim, category, salvarNoGoogle ->
                    onSaveEvent(titulo, descricao, data, inicio, fim, category, salvarNoGoogle)
                }
            )
        }
        is DialogState.EditEvent -> {
            NewEventDialog(
                eventoParaEditar = dialogState.event,
                selectedDate = selectedDate,
                categories = allCategories,
                onDismiss = onDialogDismiss,
                onConfirm = { item, titulo, descricao, data, inicio, fim, category, salvarNoGoogle ->
                    if (item != null) {
                        onUpdateEvent(item, titulo, descricao, data, inicio, fim, category, salvarNoGoogle)
                    }
                }
            )
        }
        is DialogState.ShowDetail -> {
            val category = uiState.categoriesMap[dialogState.event.categoryId]
            if (category != null) {
                EventDetailDialog(
                    event = dialogState.event,
                    category = category,
                    onDismiss = onDialogDismiss,
                    onEditClick = { onEditEventClicked(dialogState.event) },
                    onDeleteClick = { onConfirmDeleteClicked(dialogState.event) }
                )
            }
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = onDialogDismiss,
                icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso de Exclusão") },
                title = { Text("Excluir Evento") },
                text = { Text("Tem certeza que deseja excluir o evento \"${dialogState.event.titulo}\"? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = { onDeleteEvent(dialogState.event) }) {
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
                icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso de Exclusão Múltipla") },
                title = { Text("Excluir Eventos") },
                text = { Text("Tem certeza que deseja excluir os ${dialogState.count} eventos selecionados? Essa ação não pode ser desfeita.") },
                confirmButton = {
                    Button(onClick = onDeleteSelectedEvents) {
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
                        selectedCount = uiState.selectedEventIds.size,
                        onClearSelection = onClearSelection,
                        onSelectAll = onSelectAll,
                        onDelete = onConfirmDeleteSelectedEvents
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = "Minha Agenda") },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { context.startActivity(Intent(context, AchievementsActivity::class.java)) }
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
                    .fillMaxWidth()
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
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Calendário", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Gerencie seus eventos e compromissos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onAddNewEventClicked,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !uiState.isLoading && !uiState.isSelectionModeActive // Disable when in selection mode
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Novo Evento", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    CalendarContent(
                        uiState = uiState,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    EventsForDay(
                        uiState = uiState,
                        selectedDate = selectedDate,
                        eventsForDate = eventsForSelectedDate,
                        onEventClick = {
                             if (uiState.isSelectionModeActive) onEventClicked(it.id) else onShowDetailClicked(it)
                        },
                        onEventLongClick = { onEventLongPressed(it.id) },
                        onAddNewEventClicked = onAddNewEventClicked,
                        onMarkAsCompleted = onMarkAsCompleted,
                        onUnmarkAsCompleted = onUnmarkAsCompleted
                    )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerContent(
    userData: UserData?,
    account: GoogleSignInAccount?,
    showUpdateBadge: Boolean,
    onNavigate: () -> Unit,
    onLogout: () -> Unit,
    onUpdatesClicked: () -> Unit
) {
    val context = LocalContext.current
    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxHeight(),
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
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "NAVEGAÇÃO", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
                NavigationDrawerItem(
                    label = { Text("Calendário") },
                    selected = true,
                    onClick = onNavigate,
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Templates") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, TemplatesActivity::class.java))
                        onNavigate()
                    },
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Categorias") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, CategoryActivity::class.java))
                        onNavigate()
                    },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Estatísticas") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, StatsActivity::class.java))
                        onNavigate()
                    },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Conquistas") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AchievementsActivity::class.java))
                        onNavigate()
                    },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Novidades") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, UpdateNotesActivity::class.java))
                        onUpdatesClicked()
                        onNavigate()
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
                Divider()

                NavigationDrawerItem(
                    label = { Text("Informações Legais") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, LegalActivity::class.java))
                        onNavigate()
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "USUÁRIO", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(userData = userData, account = account, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(text = userData?.displayName ?: account?.displayName ?: "Usuário", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = userData?.email ?: account?.email ?: "",
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
fun CalendarHeader(
    modifier: Modifier = Modifier,
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit
) {
    Row(
        modifier = modifier
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
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
            }
        }
    }
}


@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusYears(100) }
    val endMonth = remember { currentMonth.plusYears(100) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.from(selectedDate),
        firstDayOfWeek = daysOfWeek.first()
    )

    val coroutineScope = rememberCoroutineScope()
    val visibleMonth by remember { derivedStateOf { calendarState.firstVisibleMonth.yearMonth } }

    LaunchedEffect(selectedDate) {
        coroutineScope.launch {
            calendarState.animateScrollToMonth(YearMonth.from(selectedDate))
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = visibleMonth,
            onDismiss = { showMonthYearPicker = false },
            onMonthYearSelected = {
                coroutineScope.launch {
                    calendarState.scrollToMonth(it)
                }
                showMonthYearPicker = false
            }
        )
    }

    Column {
        CalendarHeader(
            month = visibleMonth,
            onPreviousMonth = {
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(visibleMonth.minusMonths(1))
                }
            },
            onNextMonth = {
                coroutineScope.launch {
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

        val localEventsByDate = uiState.allScheduleItems.groupBy {
            it.data?.let { instant -> Instant.ofEpochMilli(instant).atZone(ZoneOffset.UTC).toLocalDate() }
        }

        val googleEventsByDate = uiState.googleCalendarEvents.groupBy { event ->
            val instant = Instant.ofEpochMilli(event.start.value)
            instant.atZone(ZoneOffset.UTC).toLocalDate()
        }

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                DayCell(
                    day = day,
                    isSelected = selectedDate == day.date,
                    localEvents = localEventsByDate[day.date] ?: emptyList(),
                    googleEvents = googleEventsByDate[day.date] ?: emptyList(),
                    categoriesMap = uiState.categoriesMap,
                    onDateSelected = { onDateSelected(it.date) }
                )
            }
        )
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    localEvents: List<ItemCronograma>,
    googleEvents: List<GoogleCalendarEvent>,
    categoriesMap: Map<String, Category>,
    onDateSelected: (CalendarDay) -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val textColor = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    var modifier = Modifier
        .aspectRatio(1f)
        .padding(2.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(
            enabled = isCurrentMonth,
            onClick = { onDateSelected(day) }
        )

    if (isSelected && isCurrentMonth) {
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (isCurrentMonth) {
            val allEventsForDay = localEvents // + googleEvents.map { it.summary }
            if (allEventsForDay.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(8.dp)
                ) {
                    localEvents.take(2).forEach { event ->
                        val category = categoriesMap[event.categoryId]
                        val eventColor = category?.cor?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary } }
                            ?: MaterialTheme.colorScheme.secondary
                        Box(modifier = Modifier.size(6.dp).background(eventColor, CircleShape))
                    }

                    val remainingCount = allEventsForDay.size - 2
                    if (remainingCount > 0) {
                        Text(text = "+${remainingCount}", fontSize = 8.sp, color = textColor)
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
    var selectedYear by remember { mutableStateOf(currentMonth.year) }

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
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Ano anterior")
                    }
                    Text(text = selectedYear.toString(), style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Próximo ano")
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
    onEventClick: (ItemCronograma) -> Unit,
    onEventLongClick: (ItemCronograma) -> Unit,
    onAddNewEventClicked: () -> Unit,
    onMarkAsCompleted: (String) -> Unit,
    onUnmarkAsCompleted: (String) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pt", "BR"))
    val formattedDate = selectedDate.format(dateFormatter)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (eventsForDate.localEvents.isEmpty() && eventsForDate.googleEvents.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Dia livre!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Você não tem nenhum evento para este dia. Que tal adicionar um?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAddNewEventClicked, enabled = !uiState.isSelectionModeActive) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Novo Evento")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Evento")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(eventsForDate.localEvents, key = { event -> event.id }) { event ->
                    val category = uiState.categoriesMap[event.categoryId]
                    if (category != null) {
                        val isChecked = uiState.completedHabitIds.contains(event.id)
                        val isSelected = uiState.selectedEventIds.contains(event.id)

                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onEventClick(event) },
                                onLongClick = { onEventLongClick(event) }
                            ),
                            colors = if(isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                        ) {
                             EventListItem(
                                item = event,
                                category = category,
                                isChecked = isChecked,
                                showCheckbox = uiState.isSelectionModeActive,
                                isSelected = isSelected,
                                onCheckedChange = { isNowChecked ->
                                    if (isNowChecked) onMarkAsCompleted(event.id) else onUnmarkAsCompleted(event.id)
                                },
                                onCardClicked = { onEventClick(event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
