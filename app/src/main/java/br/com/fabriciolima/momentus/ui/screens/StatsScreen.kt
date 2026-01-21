package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.ui.viewmodel.BarChartData
import br.com.fabriciolima.momentus.ui.viewmodel.CompletionRate
import br.com.fabriciolima.momentus.ui.viewmodel.StatsFilter
import br.com.fabriciolima.momentus.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progresso e Estatísticas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Filtro de Período",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilterButtons(
                    selectedFilter = uiState.filter, 
                    onFilterSelected = viewModel::setFilter 
                )
            }
            
            item {
                Text(
                    text = "Total de Hábitos Concluídos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                 Spacer(modifier = Modifier.height(8.dp))
                 if (uiState.barChartData.isNotEmpty()) {
                    BarChart(data = uiState.barChartData)
                 } else {
                     Text("Nenhum dado de conclusão para este período.")
                 }
            }

            item {
                Divider()
                Text(
                    text = "Taxa de Conclusão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Percentual de hábitos concluídos em relação aos agendados.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Adicionado KEY para evitar recomposições desnecessárias da lista
            items(
                items = uiState.completionRates,
                key = { it.categoryName } 
            ) { rate ->
                CompletionRateItem(rate = rate)
            }

            if (uiState.completionRates.isEmpty()) {
                item {
                    Text("Nenhuma taxa de conclusão para este período.")
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterButtons(selectedFilter: StatsFilter, onFilterSelected: (StatsFilter) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StatsFilter.entries.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(when(filter) {
                    StatsFilter.WEEK -> "Semana"
                    StatsFilter.MONTH -> "Mês"
                    StatsFilter.YEAR -> "Ano"
                })
            }
        }
    }
}

@Composable
fun CompletionRateItem(rate: CompletionRate) {
    // Cache da cor para evitar parse repetitivo
    val categoryColor = remember(rate.categoryColor) {
        try {
            Color(android.graphics.Color.parseColor(rate.categoryColor))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = rate.categoryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${(rate.percentage * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { rate.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = categoryColor,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BarChart(data: List<BarChartData>) {
    // Otimização: Cálculo do valor máximo apenas quando os dados mudam
    val maxValue = remember(data) { data.maxOfOrNull { it.value } ?: 0 }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(200.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                // Cache da cor do item
                val itemColor = remember(item.color) {
                    try {
                        Color(android.graphics.Color.parseColor(item.color))
                    } catch (e: Exception) {
                        Color.LightGray
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = item.value.toString(), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(if (maxValue > 0) item.value.toFloat() / maxValue else 0f)
                            .clip(MaterialTheme.shapes.small)
                            .background(itemColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.sp, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
