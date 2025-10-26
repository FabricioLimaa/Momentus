package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class CompletionRate(val rotinaNome: String, val rotinaCor: String, val percentage: Float)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    private val thirtyDaysAgo = LocalDate.now().minusDays(30)
    private val today = LocalDate.now()
    private val thirtyDaysAgoMillis = thirtyDaysAgo.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    // Create a map to count occurrences of each day of the week in the period, using pure Kotlin
    private val dayOfWeekCountsInPeriod: Map<String, Int> = sequence {
        var currentDate = thirtyDaysAgo
        while (!currentDate.isAfter(today)) {
            yield(currentDate)
            currentDate = currentDate.plusDays(1)
        }
    }
        .map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase() }
        .groupBy { it }
        .mapValues { it.value.size }

    @OptIn(ExperimentalCoroutinesApi::class)
    val completionRates: StateFlow<List<CompletionRate>> = repository.getStatsSummary(thirtyDaysAgoMillis)
        .flatMapLatest { summaries ->
            if (summaries.isEmpty()) {
                flowOf(emptyList())
            } else {
                // For each summary, get the flow of its schedulable events
                val schedulableEventsFlows = summaries.map { summary ->
                    repository.getSchedulableEventsForRotina(summary.rotinaId, thirtyDaysAgoMillis)
                }

                // Combine all these flows to get a list of lists of events
                combine(schedulableEventsFlows) { allEventsLists ->
                    // Now, for each summary, calculate the total and the completion rate
                    summaries.mapIndexedNotNull { index, summary ->
                        val eventsForThisRotina = allEventsLists[index]

                        // Calculate the total occurrences in the period
                        val total = eventsForThisRotina.sumOf { event ->
                            if (event.diaDaSemana != null) {
                                // For recurring events, get the count from our map
                                dayOfWeekCountsInPeriod[event.diaDaSemana.uppercase()] ?: 0
                            } else {
                                // For single-date events, just count 1
                                1
                            }
                        }

                        if (total > 0) {
                            CompletionRate(
                                rotinaNome = summary.rotinaNome,
                                rotinaCor = summary.rotinaCor,
                                percentage = summary.concluidos.toFloat() / total
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
