// ARQUIVO: ui/CalendarActivity.kt (VERSÃO FINAL PARA VERIFICAÇÃO)

package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModelFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class CalendarActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory((application as MomentusApplication).repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                val mesVisivel by viewModel.mesVisivel.observeAsState(YearMonth.now())
                val dataSelecionada by viewModel.dataSelecionada.observeAsState(LocalDate.now())
                val eventos by viewModel.eventosDoCronograma.observeAsState(emptyMap())
                val todasAsRotinas by viewModel.todasAsRotinas.observeAsState(emptyList())

                CalendarScreen(
                    mesVisivel = mesVisivel,
                    dataSelecionada = dataSelecionada,
                    eventosPorDia = eventos,
                    todasAsRotinas = todasAsRotinas,
                    onDateSelected = { viewModel.selecionarData(it) },
                    onMesAnterior = { viewModel.irParaMesAnterior() },
                    onProximoMes = { viewModel.irParaProximoMes() },
                    onNavigateBack = { finish() }
                )
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
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendário") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            CalendarHeader(
                yearMonth = mesVisivel,
                onMesAnterior = onMesAnterior,
                onProximoMes = onProximoMes
            )
            CalendarGrid(
                yearMonth = mesVisivel,
                dataSelecionada = dataSelecionada,
                eventos = eventosPorDia.keys,
                onDateSelected = onDateSelected
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
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(eventosDoDia) { evento ->
                        val rotina = todasAsRotinas.find { it.id == evento.item.rotinaId }
                        EventoListItem(item = evento, rotina = rotina)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    yearMonth: YearMonth,
    onMesAnterior: () -> Unit,
    onProximoMes: () -> Unit
) {
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
fun CalendarGrid(
    yearMonth: YearMonth,
    dataSelecionada: LocalDate,
    eventos: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val diasNoMes = yearMonth.lengthOfMonth()
    val primeiroDiaDoMes = yearMonth.atDay(1).dayOfWeek.value % 7
    val diasDaSemana = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row {
            diasDaSemana.forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(primeiroDiaDoMes) { Box(modifier = Modifier.aspectRatio(1f)) }
            items(diasNoMes) { dia ->
                val data = yearMonth.atDay(dia + 1)
                val isSelected = data == dataSelecionada
                val hasEvent = eventos.contains(data)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onDateSelected(data) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${dia + 1}",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasEvent) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EventoListItem(item: ItemCronogramaCompletado, rotina: Rotina?) {
    val cor = try {
        Color(android.graphics.Color.parseColor(rotina?.cor ?: "#808080"))
    } catch (e: Exception) { Color.Gray }

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
                Text(rotina?.nome ?: "Desconhecido", fontWeight = FontWeight.Bold)
                Text(item.item.horarioInicio, style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(
                checked = item.completado,
                onCheckedChange = null,
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    MomentusTheme {
        CalendarScreen(YearMonth.now(), LocalDate.now(), emptyMap(), emptyList(), {}, {}, {}, {})
    }
}