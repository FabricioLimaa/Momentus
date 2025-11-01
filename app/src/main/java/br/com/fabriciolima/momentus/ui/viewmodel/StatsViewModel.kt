package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class StatsFilter(val days: Long) {
    WEEK(7L),
    MONTH(30L),
    YEAR(365L)
}

data class CompletionRate(val categoryName: String, val categoryColor: String, val percentage: Float)
data class BarChartData(val label: String, val value: Int, val color: String)

data class StatsUiState(
    val filter: StatsFilter = StatsFilter.MONTH,
    val completionRates: List<CompletionRate> = emptyList(),
    val barChartData: List<BarChartData> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsData: StateFlow<Pair<List<CompletionRate>, List<BarChartData>>> = _uiState.flatMapLatest { state ->
        val since = LocalDate.now().minusDays(state.filter.days)
        val sinceMillis = since.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val dayOfWeekCountsInPeriod = getDayOfWeekCounts(since)

        repository.getStatsSummary(sinceMillis).flatMapLatest { summaries ->
            if (summaries.isEmpty()) {
                flowOf(Pair(emptyList(), emptyList()))
            } else {
                val barChartData = summaries.map {
                    BarChartData(label = it.categoryName, value = it.concluidos, color = it.categoryColor)
                }

                val schedulableEventsFlows = summaries.map { summary ->
                    repository.getSchedulableEventsForCategory(summary.categoryId, sinceMillis)
                }

                combine(schedulableEventsFlows) { allEventsLists ->
                    val completionRates = summaries.mapIndexedNotNull { index, summary ->
                        val eventsForThisCategory = allEventsLists[index]
                        val total = eventsForThisCategory.sumOf { event ->
                            if (event.diaDaSemana != null) {
                                dayOfWeekCountsInPeriod[event.diaDaSemana.uppercase()] ?: 0
                            } else { 1 }
                        }

                        if (total > 0) {
                            CompletionRate(
                                categoryName = summary.categoryName,
                                categoryColor = summary.categoryColor,
                                percentage = summary.concluidos.toFloat() / total
                            )
                        } else { null }
                    }
                    Pair(completionRates, barChartData)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(emptyList(), emptyList())
    )

    init {
        viewModelScope.launch {
            statsData.collect { (rates, chartData) ->
                _uiState.value = _uiState.value.copy(
                    completionRates = rates,
                    barChartData = chartData
                )
            }
        }
    }

    fun setFilter(filter: StatsFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    private fun getDayOfWeekCounts(since: LocalDate): Map<String, Int> {
        val today = LocalDate.now()
        return sequence {
            var currentDate = since
            while (!currentDate.isAfter(today)) {
                yield(currentDate)
                currentDate = currentDate.plusDays(1)
            }
        }
        .map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase() }
        .groupBy { it }
        .mapValues { it.value.size }
    }
}
