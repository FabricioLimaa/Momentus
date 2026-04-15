package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.database.StatsSummary
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.ui.viewmodel.BarChartData
import br.com.fabriciolima.momentus.ui.viewmodel.CompletionRate
import br.com.fabriciolima.momentus.ui.viewmodel.StatsFilter
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class StatsData(
    val completionRates: List<CompletionRate> = emptyList(),
    val barChartData: List<BarChartData> = emptyList(),
    val streakCount: Int = 0,
    val totalCompletions: Int = 0,
    val bestCategory: String? = null,
    val dailyAverage: Float = 0f,
    val completionDates: List<LocalDate> = emptyList()
)

class GetStatsUseCase @Inject constructor(
    private val repository: CategoryRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(filter: StatsFilter): Flow<Result<StatsData>> {
        val today = LocalDate.now()
        val since = today.minusDays(filter.days.toLong() - 1)
        val sinceMillis = since.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayOfWeekCountsInPeriod = getDayOfWeekCounts(since)

        return repository.getStatsSummary(sinceMillis).flatMapLatest { summaries ->
            val allDatesFlow = repository.getAllCompletionDates()
            
            val schedulableFlows = summaries.map { summary ->
                repository.getSchedulableEventsForCategory(summary.categoryId, sinceMillis)
            }

            val combinedEventsFlow: Flow<List<List<ItemCronograma>>> = if (schedulableFlows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(schedulableFlows) { it.toList() }
            }

            combine(
                combinedEventsFlow,
                allDatesFlow
            ) { allEventsLists, datesMillis ->
                val filteredDates = datesMillis.map { 
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() 
                }.filter { !it.isBefore(since) }
                
                val totalCompletions = filteredDates.size

                val barChartData = summaries.map {
                    BarChartData(label = it.categoryName, value = it.concluidos, color = it.categoryColor)
                }

                val completionRates = summaries.mapIndexedNotNull { index, summary ->
                    val events = if (allEventsLists.isNotEmpty() && index < allEventsLists.size) allEventsLists[index] else emptyList()
                    val total = events.sumOf { event: ItemCronograma ->
                        if (event.diaDaSemana != null) {
                            dayOfWeekCountsInPeriod[event.diaDaSemana!!.uppercase()] ?: 0
                        } else { 1 }
                    }

                    if (total > 0) {
                        CompletionRate(
                            categoryName = summary.categoryName,
                            categoryColor = summary.categoryColor,
                            percentage = (summary.concluidos.toFloat() / total.toFloat()).coerceAtMost(1f)
                        )
                    } else null
                }

                val bestCategory = summaries.maxByOrNull { it.concluidos }?.categoryName
                
                // Média baseada em dias ativos para ser mais motivadora
                val activeDays = filteredDates.distinct().size.coerceAtLeast(1)
                val dailyAverage = totalCompletions.toFloat() / activeDays.toFloat()

                val data = StatsData(
                    completionRates = completionRates,
                    barChartData = barChartData,
                    streakCount = calculateStreakSync(datesMillis),
                    totalCompletions = totalCompletions,
                    bestCategory = bestCategory,
                    dailyAverage = dailyAverage,
                    completionDates = filteredDates
                )
                Result.Success(data) as Result<StatsData>
            }
        }.catch { e ->
            emit(Result.Error(AppError.UnknownError(e)))
        }.flowOn(dispatcher)
    }

    private fun calculateStreakSync(completionDatesMillis: List<Long>): Int {
        if (completionDatesMillis.isEmpty()) return 0
        val dates = completionDatesMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct().sortedDescending()
        
        if (dates.isEmpty()) return 0
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        if (dates.first() != today && dates.first() != yesterday) return 0
        
        var count = 1
        var last = dates.first()
        for (i in 1 until dates.size) {
            if (last.minusDays(1) == dates[i]) {
                count++
                last = dates[i]
            } else break
        }
        return count
    }

    private fun getDayOfWeekCounts(since: LocalDate): Map<String, Int> {
        val today = LocalDate.now()
        val counts = mutableMapOf<String, Int>()
        var curr = since
        while (!curr.isAfter(today)) {
            val dow = curr.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()
            counts[dow] = (counts[dow] ?: 0) + 1
            curr = curr.plusDays(1)
        }
        return counts
    }
}
