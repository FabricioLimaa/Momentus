// ARQUIVO: ui/CalendarActivity.kt (CÓDIGO COMPLETO E REATORADO)

package br.com.fabriciolima.momentus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.components.AppDrawer
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class CalendarActivity : ComponentActivity() {
    // --- CORREÇÃO AQUI ---
    // A Factory precisa receber o 'application'
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(
            (application as MomentusApplication).repository,
            application
        )
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestScopes(Scope(CalendarScopes.CALENDAR)).build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MomentusTheme {
                val mesVisivel by viewModel.mesVisivel.observeAsState(YearMonth.now())
                val dataSelecionada by viewModel.dataSelecionada.observeAsState(LocalDate.now())
                val eventos by viewModel.eventosDoCronograma.observeAsState(emptyMap())
                val todasAsRotinas by viewModel.todasAsRotinas.observeAsState(emptyList())
                val googleAccount by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(this)) }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // A estrutura de navegação principal agora vive aqui
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            googleAccount = googleAccount,
                            onRoutinesClicked = { startActivity(Intent(this, MainActivity::class.java)) },
                            onTemplatesClicked = { startActivity(Intent(this, TemplatesActivity::class.java)) },
                            onLogoutClicked = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    val intent = Intent(this, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                }
                            },
                            onCloseDrawer = { scope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    CalendarScreen(
                        mesVisivel = mesVisivel,
                        dataSelecionada = dataSelecionada,
                        eventosPorDia = eventos,
                        todasAsRotinas = todasAsRotinas,
                        onDateSelected = { viewModel.selecionarData(it) },
                        onMesAnterior = { viewModel.irParaMesAnterior() },
                        onProximoMes = { viewModel.irParaProximoMes() },
                        onNavigateBack = { finish() },
                        // --- CORREÇÃO: A LINHA ABAIXO ESTAVA FALTANDO ---
                        onMenuClicked = { scope.launch { drawerState.open() } },
                        onNewEventClicked = { /* TODO */ },
                        onHabitoConcluidoChanged = { item, isChecked ->
                            viewModel.onHabitoConcluidoChanged(item, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    mesVisivel: YearMonth,
    dataSelecionada: LocalDate,
    eventosPorDia: Map<LocalDate, List<ItemCronogramaCompletado>>,
    todasAsRotinas: List<Rotina>,
    onDateSelected: (LocalDate) -> Unit,
    onMesAnterior: () -> Unit,
    onProximoMes: () -> Unit,
    onNavigateBack: () -> Unit,
    // --- CORREÇÃO: E garantimos que o nome seja 'onMenuClicked' aqui também ---
    onMenuClicked: () -> Unit,
    onNewEventClicked: () -> Unit,
    onHabitoConcluidoChanged: (ItemCronograma, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope() // <-- CORREÇÃO: O import estava faltando
    val state = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(100),
        endMonth = YearMonth.now().plusMonths(100),
        firstVisibleMonth = mesVisivel,
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    LaunchedEffect(state.firstVisibleMonth) {
        if (state.firstVisibleMonth.yearMonth != mesVisivel) {
            // Lógica para atualizar o ViewModel com o mês visível, se necessário.
            // Para nosso caso, os botões já fazem isso.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendário") },
                navigationIcon = {
                    IconButton(onClick = onMenuClicked) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewEventClicked) {
                Icon(Icons.Default.Add, contentDescription = "Novo Evento")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // CORREÇÃO: A chamada das funções animateScrollToMonth precisa de um 'scope.launch'
            CalendarHeader(
                yearMonth = state.firstVisibleMonth.yearMonth,
                onMesAnterior = { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) } },
                onProximoMes = { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) } }
            )
            DaysOfWeekHeader()
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(
                        day = day,
                        isSelected = dataSelecionada == day.date,
                        hasEvent = eventosPorDia.containsKey(day.date),
                        onClick = { onDateSelected(it.date) }
                    )
                }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            val eventosDoDia = eventosPorDia[dataSelecionada] ?: emptyList()
            val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
            Text(
                text = dataSelecionada.format(formatter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (eventosDoDia.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum evento para este dia.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(eventosDoDia, key = { it.item.id }) { evento ->
                        val rotina = todasAsRotinas.find { it.id == evento.item.rotinaId }
                        EventoListItem(
                            item = evento,
                            rotina = rotina,
                            onCheckedChange = onHabitoConcluidoChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventoListItem(
    item: ItemCronogramaCompletado,
    rotina: Rotina?,
    onCheckedChange: (ItemCronograma, Boolean) -> Unit
) {
    val cor = try {
        Color(android.graphics.Color.parseColor(rotina?.cor ?: "#808080"))
    } catch (e: Exception) { Color.Gray }
    val alpha = if (item.completado) 0.6f else 1f
    val textDecoration = if (item.completado) TextDecoration.LineThrough else TextDecoration.None

    Card(shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(cor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rotina?.nome ?: "Desconhecido",
                    fontWeight = FontWeight.Bold,
                    textDecoration = textDecoration,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = item.item.horarioInicio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            Checkbox(
                checked = item.completado,
                onCheckedChange = { isChecked -> onCheckedChange(item.item, isChecked) }
            )
        }
    }
}


@Composable
fun CalendarHeader(yearMonth: YearMonth, onMesAnterior: () -> Unit, onProximoMes: () -> Unit) {
    // CORREÇÃO: O import para DateTimeFormatter estava faltando
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMesAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
        }
        Text(
            text = yearMonth.format(formatter).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onProximoMes) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val dias = remember { DayOfWeek.values() }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp)) {
        for (dia in dias) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                text = dia.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).uppercase()
            )
        }
    }
}

@Composable
fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvent: Boolean,
    onClick: (CalendarDay) -> Unit
) {
    val isFromCurrentMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                enabled = isFromCurrentMonth,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isFromCurrentMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
            if (hasEvent) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(6.dp).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    MomentusTheme {
        CalendarScreen(
            mesVisivel = YearMonth.now(),
            dataSelecionada = LocalDate.now(),
            eventosPorDia = emptyMap(),
            todasAsRotinas = emptyList(),
            onDateSelected = {},
            onMesAnterior = {},
            onProximoMes = {},
            onNavigateBack = {},
            onMenuClicked = {},
            onNewEventClicked = {},
            onHabitoConcluidoChanged = { _, _ -> }
        )
    }
}