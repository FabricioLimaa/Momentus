// ARQUIVO: ui/StatsActivity.kt (CÓDIGO COMPLETO E FINAL)

package br.com.fabriciolima.momentus.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.StatsResult
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.StatsViewModel
import br.com.fabriciolima.momentus.viewmodel.StatsViewModelFactory
class StatsActivity : ComponentActivity() {

    private val viewModel: StatsViewModel by viewModels {
        StatsViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                val stats by viewModel.stats.observeAsState(initial = emptyList())
                StatsScreen(statsData = stats)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(statsData: List<StatsResult>) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Estatísticas de Rotinas") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (statsData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum dado de cronograma para exibir.")
                }
            } else {
                val totalMinutos = statsData.sumOf { it.totalMinutos }
                val pieChartData = statsData.map {
                    PieChartEntry(
                        color = androidx.compose.ui.graphics.Color(Color.parseColor(it.corRotina)),
                        percentage = (it.totalMinutos.toFloat() / totalMinutos.toFloat()),
                        label = it.nomeRotina
                    )
                }
                PieChart(
                    entries = pieChartData,
                    modifier = Modifier
                        .size(250.dp)
                        .padding(16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                StatsLegend(entries = pieChartData)
            }
        }
    }
}

@Composable
fun PieChart(
    entries: List<PieChartEntry>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 50f
) {
    var startAngle = -90f
    Canvas(modifier = modifier) {
        entries.forEach { entry ->
            val sweepAngle = entry.percentage * 360f
            drawArc(
                color = entry.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun StatsLegend(entries: List<PieChartEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries) { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(entry.color, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${entry.label} (${"%.1f".format(entry.percentage * 100)}%)",
                    fontSize = 16.sp
                )
            }
        }
    }
}

data class PieChartEntry(
    val color: androidx.compose.ui.graphics.Color,
    val percentage: Float,
    val label: String
)

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    MomentusTheme {
        val previewData = listOf(
            StatsResult("Estudo", "#4CAF50", 120),
            StatsResult("Família", "#2196F3", 180),
            StatsResult("Lazer", "#FFC107", 60)
        )
        StatsScreen(statsData = previewData)
    }
}