package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.ui.viewmodel.BarChartData
import br.com.fabriciolima.momentus.ui.viewmodel.CompletionRate
import br.com.fabriciolima.momentus.ui.viewmodel.StatsFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
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
        val since = LocalDate.now().minusDays(filter.days)
        val sinceMillis = since.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayOfWeekCountsInPeriod = getDayOfWeekCounts(since)

        val summaryFlow = repository.getStatsSummary(sinceMillis)
        val completionDatesFlow = repository.getAllCompletionDates()

        // Combina os fluxos de dados base. O lambda aqui retorna um Flow<StatsData>.
        val combinedFlow: Flow<Flow<StatsData>> = combine(summaryFlow, completionDatesFlow) { summaries, completionDates ->
            val streakCount = calculateStreak(completionDates)

            if (summaries.isEmpty()) {
                // Se não há dados, retorna um Flow contendo apenas o streak.
                flowOf(StatsData(streakCount = streakCount))
            } else {
                val barChartData = summaries.map {
                    BarChartData(label = it.categoryName, value = it.concluidos, color = it.categoryColor)
                }

                val schedulableEventsFlows = summaries.map { summary ->
                    repository.getSchedulableEventsForCategory(summary.categoryId, sinceMillis)
                }

                // Combina os eventos agendáveis para calcular a taxa de conclusão.
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
                    StatsData(completionRates, barChartData, streakCount)
                }
            }
        }

        // flatMapLatest "achata" o Flow<Flow<StatsData>> para o Flow<StatsData> que a UI espera.
        return combinedFlow.flatMapLatest { it }
    }

    private suspend fun calculateStreak(completionDatesMillis: List<Long>): Int = withContext(dispatcher) {
        if (completionDatesMillis.isEmpty()) return@withContext 0

        val completionDates = completionDatesMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()

        if (completionDates.isEmpty()) return@withContext 0

        var currentStreak = 0
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        if (completionDates.first() == today || completionDates.first() == yesterday) {
            currentStreak = 1
            var lastDate = completionDates.first()

            for (i in 1 until completionDates.size) {
                val currentDate = completionDates[i]
                if (lastDate.minusDays(1) == currentDate) {
                    currentStreak++
                    lastDate = currentDate
                } else {
                    break
                }
            }
        }

        currentStreak
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
