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
    val completionDates: List<LocalDate> = emptyList(),
    val improvementPercentage: Int = 0,
    val bestHour: Int? = null
)

class GetStatsUseCase @Inject constructor(
    private val repository: CategoryRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(filter: StatsFilter): Flow<Result<StatsData>> {
        val today = LocalDate.now()
        val since = today.minusDays(filter.days.toLong() - 1)
        val sinceMillis = since.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val previousSince = since.minusDays(filter.days.toLong())
        val previousSinceMillis = previousSince.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val dayOfWeekCountsInPeriod = getDayOfWeekCounts(since)

        return combine(
            repository.getStatsSummary(sinceMillis),
            repository.getAllCategories()
        ) { summaries, allCategories ->
            val summariesMap = summaries.associateBy { it.categoryId }

            // Garantir que TODAS as categorias apareçam na lista de taxas de conclusão
            val allCategorySummaries = allCategories.map { category ->
                summariesMap[category.id] ?: StatsSummary(
                    categoryId = category.id,
                    categoryName = category.nome,
                    categoryColor = category.cor,
                    concluidos = 0
                )
            }

            val allDatesFlow = repository.getAllCompletionDates()
            
            // Busca eventos agendáveis para calcular a taxa de conclusão (esperado vs realizado)
            val schedulableFlows = allCategorySummaries.map { summary ->
                repository.getSchedulableEventsForCategory(summary.categoryId, sinceMillis)
            }

            val combinedEventsFlow: Flow<List<List<ItemCronograma>>> = if (schedulableFlows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(schedulableFlows) { it.toList() }
            }

            combine(
                allDatesFlow,
                combinedEventsFlow
            ) { datesMillis, allEventsLists ->
                val zoneId = ZoneId.systemDefault()
                
                val currentPeriodDates = datesMillis.map { 
                    Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() 
                }.filter { !it.isBefore(since) && !it.isAfter(today) }

                val previousPeriodDates = datesMillis.map { 
                    Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() 
                }.filter { !it.isBefore(previousSince) && it.isBefore(since) }
                
                val currentCount = currentPeriodDates.size
                val previousCount = previousPeriodDates.size

                val improvement = if (previousCount > 0) {
                    (((currentCount - previousCount).toFloat() / previousCount.toFloat()) * 100).toInt()
                } else if (currentCount > 0) 100 else 0

                val bestHour = datesMillis.filter { it >= sinceMillis }.map { 
                    Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime().hour 
                }.groupBy { it }.maxByOrNull { it.value.size }?.key

                // Cálculo da Taxa de Conclusão (Realizado / Agendado)
                val completionRates = allCategorySummaries.mapIndexedNotNull { index, summary ->
                    val events = if (allEventsLists.isNotEmpty() && index < allEventsLists.size) allEventsLists[index] else emptyList()
                    val totalExpected = events.sumOf { event ->
                        if (event.diaDaSemana != null) {
                            dayOfWeekCountsInPeriod[event.diaDaSemana!!.uppercase()] ?: 0
                        } else { 1 }
                    }

                    // No Momentus, se há eventos agendados, mostramos a taxa.
                    // Se não há NADA agendado para a categoria, não faz sentido mostrar taxa de 0%.
                    if (totalExpected > 0) {
                        CompletionRate(
                            categoryName = summary.categoryName,
                            categoryColor = summary.categoryColor,
                            percentage = (summary.concluidos.toFloat() / totalExpected.toFloat()).coerceAtMost(1f)
                        )
                    } else null
                }

                val barChartData = allCategorySummaries.filter { it.concluidos > 0 }.map {
                    BarChartData(label = it.categoryName, value = it.concluidos, color = it.categoryColor)
                }

                val bestCategory = allCategorySummaries.maxByOrNull { it.concluidos }?.categoryName
                val activeDays = currentPeriodDates.distinct().size.coerceAtLeast(1)

                StatsData(
                    completionRates = completionRates.sortedByDescending { it.percentage },
                    barChartData = barChartData,
                    streakCount = calculateStreakSync(datesMillis),
                    totalCompletions = currentCount,
                    bestCategory = if(currentCount > 0) bestCategory else null,
                    dailyAverage = currentCount.toFloat() / activeDays.toFloat(),
                    completionDates = currentPeriodDates,
                    improvementPercentage = improvement,
                    bestHour = bestHour
                )
            }
        }.flatMapLatest { it }.map { Result.Success(it) as Result<StatsData> }
        .catch { e ->
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
