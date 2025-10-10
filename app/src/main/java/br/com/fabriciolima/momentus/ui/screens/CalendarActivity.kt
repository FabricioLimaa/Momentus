package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.ui.components.EventDetailDialog
import br.com.fabriciolima.momentus.ui.components.EventListItem
import br.com.fabriciolima.momentus.ui.components.NewEventDialog
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.GoogleAuthUtils
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MomentusTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                val todasAsRotinas by viewModel.todasAsRotinas.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val account = GoogleSignIn.getLastSignedInAccount(context)

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            account = account,
                            onNavigate = { scope.launch { drawerState.close() } },
                            onLogout = {
                                val gso = GoogleAuthUtils.getGoogleSignInOptions(this)
                                GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        )
                    }
                ) {
                    CalendarScreen(
                        uiState = uiState,
                        selectedDate = selectedDate,
                        todasAsRotinas = todasAsRotinas,
                        account = account,
                        onDateSelected = { viewModel.selectDate(it) },
                        onPreviousMonth = { viewModel.selectDate(it.minusMonths(1)) },
                        onNextMonth = { viewModel.selectDate(it.plusMonths(1)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchGoogleCalendarEvents()
    }
}

@Composable
fun UserAvatar(account: GoogleSignInAccount?, modifier: Modifier = Modifier) {
    if (account?.photoUrl != null) {
        AsyncImage(
            model = account.photoUrl,
            contentDescription = "Foto do Perfil",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = account?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    selectedDate: LocalDate,
    todasAsRotinas: List<Rotina>,
    account: GoogleSignInAccount?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: (LocalDate) -> Unit,
    onNextMonth: (LocalDate) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CalendarViewModel
) {
    var showNewEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<ItemCronograma?>(null) }
    var selectedEventForDetail by remember { mutableStateOf<ItemCronograma?>(null) }
    var eventToDelete by remember { mutableStateOf<ItemCronograma?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isDialogVisible = showNewEventDialog || eventToEdit != null

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.onErrorShown()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.onSuccessMessageShown()
        }
    }

    AnimatedVisibility(visible = isDialogVisible, enter = fadeIn(), exit = fadeOut()) {
        NewEventDialog(
            eventoParaEditar = eventToEdit,
            selectedDate = selectedDate,
            rotinas = todasAsRotinas,
            onDismiss = { 
                showNewEventDialog = false 
                eventToEdit = null
            },
            onConfirm = { item, titulo, descricao, data, inicio, fim, rotina, salvarNoGoogle ->
                if (item == null) {
                    viewModel.salvarEventoUnico(titulo, descricao, data, inicio, fim, rotina, salvarNoGoogle)
                } else {
                    viewModel.atualizarEvento(item, titulo, descricao, data, inicio, fim, rotina, salvarNoGoogle)
                }
                showNewEventDialog = false
                eventToEdit = null
            }
        )
    }

    selectedEventForDetail?.let { event ->
        val rotina = uiState.rotinasMap[event.rotinaId]
        if (rotina != null) {
            EventDetailDialog(
                event = event,
                rotina = rotina,
                onDismiss = { selectedEventForDetail = null },
                onEditClick = { 
                    selectedEventForDetail = null
                    eventToEdit = event
                },
                onDeleteClick = { 
                    selectedEventForDetail = null
                    eventToDelete = event
                }
            )
        }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = "Aviso de Exclusão") },
            title = { Text("Excluir Evento") },
            text = { Text("Tem certeza que deseja excluir o evento \"${event.titulo}\"? Essa ação não pode ser desfeita.") },
            confirmButton = {
                Button(onClick = { 
                    viewModel.excluirEvento(event)
                    eventToDelete = null 
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Minha Agenda") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Abrir perfil do usuário */ }) {
                        UserAvatar(account = account, modifier = Modifier.size(32.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Calendário", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Gerencie seus eventos e compromissos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showNewEventDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Novo Evento", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            CalendarHeader(selectedDate, onPreviousMonth, onNextMonth)
            CalendarGrid(selectedDate, onDateSelected, uiState)
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            EventsForDay(
                uiState = uiState, 
                selectedDate = selectedDate, 
                onEventClick = { event -> selectedEventForDetail = event }
            )
        }
    }
}

@Composable
fun AppDrawerContent(account: GoogleSignInAccount?, onNavigate: () -> Unit, onLogout: () -> Unit) {
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
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "USUÁRIO", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(account = account, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(text = account?.displayName ?: "Usuário", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = account?.email ?: "", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sair",
                    modifier = Modifier.clickable(onClick = onLogout).padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CalendarHeader(selectedDate: LocalDate, onPreviousMonth: (LocalDate) -> Unit, onNextMonth: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
        Text(
            text = selectedDate.format(monthFormatter).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row {
            IconButton(onClick = { onPreviousMonth(selectedDate) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
            }
            IconButton(onClick = { onNextMonth(selectedDate) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
            }
        }
    }
}

@Composable
fun CalendarGrid(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, uiState: CalendarUiState) {
    val yearMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    val localEventsByDate = uiState.allScheduleItems.groupBy {
        it.data?.let { instant -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(instant), java.time.ZoneId.systemDefault()) }
    }

    val googleEventsByDate = uiState.googleCalendarEvents.groupBy { event ->
        val instant = Instant.ofEpochMilli(event.start.value)
        instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
            daysOfWeek.forEach {
                Text(text = it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false
        ) {
            val startOffset = firstDayOfMonth.dayOfWeek.value % 7
            items(startOffset) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            items(daysInMonth) {
                val day = firstDayOfMonth.plusDays(it.toLong())
                val isSelected = day == selectedDate
                val isCurrentMonth = YearMonth.from(day) == yearMonth
                val localEventsForDay = localEventsByDate[day] ?: emptyList()
                val googleEventsForDay = googleEventsByDate[day] ?: emptyList()

                DayCell(day, isSelected, isCurrentMonth, localEventsForDay, googleEventsForDay, uiState.rotinasMap, onDateSelected)
            }
        }
    }
}

@Composable
fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    localEvents: List<ItemCronograma>,
    googleEvents: List<GoogleCalendarEvent>,
    rotinasMap: Map<String, Rotina>,
    onDateSelected: (LocalDate) -> Unit
) {
    val textColor = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    var modifier = Modifier
        .aspectRatio(1f)
        .padding(2.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable { onDateSelected(day) }

    if (isSelected) {
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        // MELHORIA 3: Indicador visual para múltiplos eventos
        val allEventsForDay = localEvents + googleEvents.map { it.summary } // Simplificando para contagem
        if (allEventsForDay.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(8.dp)
            ) {
                // Mostrar até 2 pontos coloridos
                localEvents.take(2).forEach { event ->
                    val rotina = rotinasMap[event.rotinaId]
                    val eventColor = rotina?.cor?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary } } ?: MaterialTheme.colorScheme.secondary
                    Box(modifier = Modifier.size(6.dp).background(eventColor, CircleShape))
                }

                // Mostrar ponto para eventos do Google se não houver 2 eventos locais
                if (localEvents.size < 2 && googleEvents.isNotEmpty()) {
                    val remainingSlots = 2 - localEvents.size
                    googleEvents.take(remainingSlots).forEach { _ ->
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                    }
                }

                // Mostrar contador se houver mais de 2 eventos
                val remainingCount = allEventsForDay.size - 2
                if (remainingCount > 0) {
                    Text(text = "+${remainingCount}", fontSize = 8.sp, color = textColor)
                }
            }
        }
    }
}

@Composable
private fun rememberContentColorFor(backgroundColor: Color): Color {
    return remember(backgroundColor) {
        if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    }
}


@Composable
fun EventsForDay(uiState: CalendarUiState, selectedDate: LocalDate, onEventClick: (ItemCronograma) -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pt", "BR"))
    val formattedDate = selectedDate.format(dateFormatter)

    val localEventsForDay = uiState.allScheduleItems.filter {
        it.data != null && LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.data), java.time.ZoneId.systemDefault()) == selectedDate
    }

    val googleEventsForDay = uiState.googleCalendarEvents.filter { event ->
        val instant = Instant.ofEpochMilli(event.start.value)
        val eventDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        eventDate == selectedDate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (localEventsForDay.isEmpty() && googleEventsForDay.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nenhum evento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "Você não tem nenhum evento para este dia. Que tal adicionar um?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(localEventsForDay) {
                    val rotina = uiState.rotinasMap[it.rotinaId]
                    if (rotina != null) {
                        EventListItem(item = it, rotina = rotina, modifier = Modifier.clickable { onEventClick(it) })
                    }
                }

                items(googleEventsForDay) { event ->
                    GoogleEventListItem(event = event)
                }
            }
        }
    }
}

@Composable
fun GoogleEventListItem(event: GoogleCalendarEvent) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val instant = Instant.ofEpochMilli(event.start.value)
    val startTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text(text = event.summary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "Início: ${startTime.format(timeFormatter)} - Google Agenda", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
