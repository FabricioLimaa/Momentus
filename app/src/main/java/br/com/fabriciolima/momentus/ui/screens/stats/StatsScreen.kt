package br.com.fabriciolima.momentus.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import br.com.fabriciolima.momentus.ui.theme.*
import java.time.LocalDate
import java.util.Locale
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.material3.windowsizeclass.WindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        LaunchedEffect(uiState.error) {
            uiState.error?.let { error ->
                val activityContext = context as? android.app.Activity
                val messageText = error.message ?: error.messageResId?.let { activityContext?.getString(it) } ?: "Erro desconhecido"
                snackbarHostState.showSnackbar(message = messageText)
                viewModel.onErrorShown()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Análise de Performance", 
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Sutil gradiente de fundo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterButtonsPremium(
                            selectedFilter = uiState.filter, 
                            onFilterSelected = viewModel::setFilter 
                        )
                    }

                    item {
                        Text(
                            text = "Insights de ${if(uiState.filter == StatsFilter.WEEK) "Semana" else if(uiState.filter == StatsFilter.MONTH) "Mês" else "Ano"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InsightCardPremium(
                                title = if (uiState.improvementPercentage >= 0) "Evolução" else "Retração",
                                value = "${kotlin.math.abs(uiState.improvementPercentage)}%",
                                subtitle = "Vs. período anterior",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                modifier = Modifier.weight(1f),
                                color = if (uiState.improvementPercentage >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            InsightCardPremium(
                                title = "Horário Ápice",
                                value = uiState.bestHour?.let { "${it}h" } ?: "--",
                                subtitle = "Maior produtividade",
                                icon = Icons.Default.Schedule,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Conclusões por Período",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (uiState.barChartData.isNotEmpty()) {
                            BarChartPremium(data = uiState.barChartData)
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Dados insuficientes para análise.", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Taxa de Conclusão por Categoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.completionRates.isEmpty()) {
                        item {
                            Text(
                                "Nenhuma atividade registrada no período.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    } else {
                        items(uiState.completionRates) { rate ->
                            CompletionRateItemPremium(rate = rate)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun FilterButtonsPremium(selectedFilter: StatsFilter, onFilterSelected: (StatsFilter) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Button(
                    onClick = { onFilterSelected(filter) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = null
                ) {
                    Text(
                        text = when(filter) {
                            StatsFilter.WEEK -> "Semana"
                            StatsFilter.MONTH -> "Mês"
                            StatsFilter.YEAR -> "Ano"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
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
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (icon == Icons.AutoMirrored.Filled.TrendingUp && value != "0%") {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp).padding(bottom = 6.dp, start = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun BarChartPremium(data: List<BarChartData>) {
    val maxValue = remember(data) { data.maxOfOrNull { it.value } ?: 1 }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.height(200.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    val itemColor = remember(item.color) {
                        try { Color(android.graphics.Color.parseColor(item.color)) } catch (e: Exception) { Color.Black }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (item.value > 0) {
                            Text(
                                text = item.value.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = itemColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight((item.value.toFloat() / maxValue.toFloat()).coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(itemColor, itemColor.copy(alpha = 0.3f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = item.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
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

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(categoryColor))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rate.categoryName, 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(rate.percentage * 100).toInt()}%", 
                    fontWeight = FontWeight.Black, 
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { rate.percentage },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
