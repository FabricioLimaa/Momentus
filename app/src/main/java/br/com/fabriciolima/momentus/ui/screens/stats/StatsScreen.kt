package br.com.fabriciolima.momentus.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.ui.viewmodel.BarChartData
import br.com.fabriciolima.momentus.ui.viewmodel.CompletionRate
import br.com.fabriciolima.momentus.ui.viewmodel.StatsFilter
import br.com.fabriciolima.momentus.ui.viewmodel.StatsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val messageText = error.message ?: error.messageResId?.let { context.getString(it) } ?: "Erro desconhecido"
            snackbarHostState.showSnackbar(message = messageText)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Progresso", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FilterButtons(
                    selectedFilter = uiState.filter, 
                    onFilterSelected = viewModel::setFilter 
                )
            }

            // --- INSIGHTS PREMIUM (Mockup Estilo) ---
            item {
                Text(
                    text = "Insights da ${if(uiState.filter == StatsFilter.WEEK) "semana" else if(uiState.filter == StatsFilter.MONTH) "mês" else "ano"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightCardPremium(
                        title = if (uiState.improvementPercentage >= 0) "Você melhorou" else "Você caiu",
                        value = "${kotlin.math.abs(uiState.improvementPercentage)}%",
                        subtitle = "em relação ao período anterior",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f),
                        color = if (uiState.improvementPercentage >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    InsightCardPremium(
                        title = "Melhor horário",
                        value = uiState.bestHour?.let { "${it}h" } ?: "--",
                        subtitle = "Período de maior produtividade",
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF6366F1)
                    )
                }
            }

            item {
                Text(
                    text = "Total de Hábitos Concluídos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.barChartData.isNotEmpty()) {
                    BarChartPremium(data = uiState.barChartData)
                } else {
                    Text("Dados insuficientes para o gráfico.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Text(
                    text = "Taxa de Conclusão",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.completionRates.isEmpty()) {
                item {
                    Text(
                        "Nenhuma atividade registrada no período.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.completionRates) { rate ->
                    CompletionRateItemPremium(rate = rate)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun InsightCardPremium(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                if (icon == Icons.AutoMirrored.Filled.TrendingUp) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp).padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun BarChartPremium(data: List<BarChartData>) {
    val maxValue = remember(data) { data.maxOfOrNull { it.value } ?: 1 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).height(180.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val itemColor = remember(item.color) {
                    try { Color(android.graphics.Color.parseColor(item.color)) } catch (e: Exception) { Color.Gray }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(item.value.toFloat() / maxValue.toFloat().coerceAtLeast(1f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemColor)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionRateItemPremium(rate: CompletionRate) {
    val categoryColor = remember(rate.categoryColor) {
        try { Color(android.graphics.Color.parseColor(rate.categoryColor)) } catch (e: Exception) { Color.Gray }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = rate.categoryName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "${(rate.percentage * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = categoryColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { rate.percentage },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            color = categoryColor,
            trackColor = categoryColor.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun InsightCardMinimal(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * ANÉIS DE PROGRESSO SEMANAL: Visualização fixa de Domingo a Sábado com nomes abreviados.
 */
@Composable
fun WeeklyProgressRings(dates: List<LocalDate>) {
    val today = LocalDate.now()
    val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val completionSet = dates.toSet()
    val ptBr = Locale.forLanguageTag("pt-BR")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until 7) {
            val date = sunday.plusDays(i.toLong())
            val isActive = completionSet.contains(date)
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { if (isActive) 1f else 0f },
                        modifier = Modifier.size(38.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Exibe o dia abreviado (DOM, SEG, TER...)
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, ptBr).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
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
                label = {
                    Text(when(filter) {
                        StatsFilter.WEEK -> "Semana"
                        StatsFilter.MONTH -> "Mês"
                        StatsFilter.YEAR -> "Ano"
                    })
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsFilter.entries.size)
            )
        }
    }
}

@Composable
fun CompletionRateItem(rate: CompletionRate) {
    val categoryColor = remember(rate.categoryColor) {
        try { Color(android.graphics.Color.parseColor(rate.categoryColor)) } catch (e: Exception) { Color.Gray }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = rate.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = "${(rate.percentage * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = categoryColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { rate.percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = categoryColor,
            trackColor = categoryColor.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun BarChart(data: List<BarChartData>) {
    if (data.isEmpty()) return

    val maxValue = remember(data) { data.maxOfOrNull { it.value } ?: 1 }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(160.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val itemColor = remember(item.color) {
                    try { Color(android.graphics.Color.parseColor(item.color)) } catch (e: Exception) { Color.Gray }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = item.value.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight(item.value.toFloat() / maxValue.toFloat())
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(itemColor)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
