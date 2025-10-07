package br.com.fabriciolima.momentus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.ui.components.EventListItem
import br.com.fabriciolima.momentus.ui.components.NewEventDialog
import br.com.fabriciolima.momentus.viewmodel.CalendarUiState
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.viewmodel.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels {
        ViewModelFactory(AppDatabase.getDatabase(this), this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chama a busca de eventos através do ViewModel
        viewModel.fetchGoogleCalendarEvents(this)

        setContent {
            val uiState by viewModel.uiState.observeAsState()
            val selectedDate by viewModel.selectedDate.observeAsState(LocalDate.now())

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawerContent(
                        onNavigate = {
                            scope.launch { drawerState.close() }
                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState?,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: (LocalDate) -> Unit,
    onNextMonth: (LocalDate) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CalendarViewModel
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NewEventDialog(
            selectedDate = selectedDate,
            rotinas = uiState?.rotinasMap?.values?.toList() ?: emptyList(),
            onDismiss = { showDialog = false },
            onConfirm = {
                titulo, descricao, data, inicio, fim, rotina ->
                viewModel.salvarEventoUnico(titulo, descricao, data, inicio, fim, rotina)
                showDialog = false
            }
        )
    }

    Scaffold(
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("F", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Novo Evento")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            CalendarHeader(selectedDate, onPreviousMonth, onNextMonth)
            CalendarGrid(selectedDate, onDateSelected, uiState)
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            EventsForDay(uiState, selectedDate)
        }
    }
}

@Composable
fun AppDrawerContent(onNavigate: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)

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
            Column(modifier = Modifier.padding(16.dp)) {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "USUÁRIO", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                         Text(
                            text = account?.displayName?.firstOrNull()?.toString() ?: "U", 
                            color = MaterialTheme.colorScheme.onPrimary, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(text = account?.displayName ?: "Usuário", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = account?.email ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onLogout) {
                    Text("Sair")
                }
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
fun CalendarGrid(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, uiState: CalendarUiState?) {
    val yearMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    val localEventsByDate = uiState?.allScheduleItems?.groupBy {
        it.data?.let { instant -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(instant), java.time.ZoneId.systemDefault()) }
    } ?: emptyMap()

    val googleEventsByDate = uiState?.googleCalendarEvents?.groupBy { event ->
        val instant = Instant.ofEpochMilli(event.start.value)
        instant.atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: emptyMap()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                DayCell(day, isSelected, isCurrentMonth, localEventsForDay, googleEventsForDay, uiState?.rotinasMap ?: emptyMap(), onDateSelected)
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
    val textColor = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else Color.Gray

    var modifier = Modifier
        .aspectRatio(1f)
        .padding(2.dp)
        .clip(RoundedCornerShape(4.dp))
        .clickable { onDateSelected(day) }

    if (isSelected) {
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        val totalEvents = localEvents.size + googleEvents.size

        if (totalEvents > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Mostra até 2 eventos locais
                localEvents.take(2).forEach { event ->
                    val rotina = rotinasMap[event.rotinaId]
                    val color = rotina?.cor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Gray
                    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                }
                // Se houver eventos do Google, mostra um indicador
                if (googleEvents.isNotEmpty()) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                }
            }
        }

        if (totalEvents > 2) {
            Text(
                text = "+${totalEvents - 1} mais",
                fontSize = 8.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun EventsForDay(uiState: CalendarUiState?, selectedDate: LocalDate) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pt", "BR"))
    val formattedDate = selectedDate.format(dateFormatter)

    // Filtra os eventos locais para o dia selecionado
    val localEventsForDay = uiState?.allScheduleItems?.filter {
        it.data != null && LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.data), java.time.ZoneId.systemDefault()) == selectedDate
    } ?: emptyList()

    // Filtra os eventos do Google para o dia selecionado
    val googleEventsForDay = uiState?.googleCalendarEvents?.filter { event ->
        val instant = Instant.ofEpochMilli(event.start.value)
        val eventDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        eventDate == selectedDate
    } ?: emptyList()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (localEventsForDay.isEmpty() && googleEventsForDay.isEmpty()) {
            Text("Nenhum evento para este dia.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Renderiza os eventos locais
                items(localEventsForDay) {
                    val rotina = uiState?.rotinasMap?.get(it.rotinaId)
                    if (rotina != null) {
                        EventListItem(item = it, rotina = rotina)
                    }
                }

                // Renderiza os eventos do Google
                items(googleEventsForDay) { event ->
                    GoogleEventListItem(event = event)
                }
            }
        }
    }
}

@Composable
fun GoogleEventListItem(event: br.com.fabriciolima.momentus.viewmodel.GoogleCalendarEvent) {
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
                    .background(Color.Red, CircleShape) // Cor genérica para eventos do Google
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text(text = event.summary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "Início: ${startTime.format(timeFormatter)} - Google Agenda", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
