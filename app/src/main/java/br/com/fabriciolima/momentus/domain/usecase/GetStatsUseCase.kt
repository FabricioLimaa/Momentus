package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.database.StatsSummary
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.ui.viewmodel.BarChartData
import br.com.fabriciolima.momentus.ui.viewmodel.CompletionRate
import br.com.fabriciolima.momentus.ui.viewmodel.StatsFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class StatsData(
    val completionRates: List<CompletionRate> = emptyList(),
    val barChartData: List<BarChartData> = emptyList(),
    val streakCount: Int = 0
)

class GetStatsUseCase @Inject constructor(
    private val repository: CategoryRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(filter: StatsFilter): Flow<StatsData> {
        val since = LocalDate.now().minusDays(filter.days.toLong())
        val sinceMillis = since.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayOfWeekCountsInPeriod = getDayOfWeekCounts(since)

        return repository.getStatsSummary(sinceMillis).flatMapLatest { summaries ->
            if (summaries.isEmpty()) {
                repository.getAllCompletionDates().map { dates ->
                    StatsData(streakCount = calculateStreakSync(dates))
                }
            } else {
                val schedulableFlows = summaries.map { summary ->
                    repository.getSchedulableEventsForCategory(summary.categoryId, sinceMillis)
                }

                combine(
                    combine(schedulableFlows) { it.toList() },
                    repository.getAllCompletionDates()
                ) { allEventsLists, completionDates ->
                    val barChartData = summaries.map {
                        BarChartData(label = it.categoryName, value = it.concluidos, color = it.categoryColor)
                    }

                    val completionRates = summaries.mapIndexedNotNull { index, summary ->
                        val events = allEventsLists[index]
                        val total = events.sumOf { event ->
                            if (event.diaDaSemana != null) {
                                dayOfWeekCountsInPeriod[event.diaDaSemana.uppercase()] ?: 0
                            } else { 1 }
                        }

                        if (total > 0) {
                            CompletionRate(
                                categoryName = summary.categoryName,
                                categoryColor = summary.categoryColor,
                                percentage = summary.concluidos.toFloat() / total.toFloat()
                            )
                        } else null
                    }

                    StatsData(
                        completionRates = completionRates,
                        barChartData = barChartData,
                        streakCount = calculateStreakSync(completionDates)
                    )
                }
            }
        }
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
