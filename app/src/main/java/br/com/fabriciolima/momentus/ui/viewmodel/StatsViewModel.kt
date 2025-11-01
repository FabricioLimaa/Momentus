package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.domain.usecase.GetStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val barChartData: List<BarChartData> = emptyList(),
    val streakCount: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getStatsUseCase: GetStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsData = _uiState.flatMapLatest {
        getStatsUseCase(it.filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            statsData.collect { data ->
                if (data == null) return@collect
                _uiState.value = _uiState.value.copy(
                    completionRates = data.completionRates,
                    barChartData = data.barChartData,
                    streakCount = data.streakCount
                )
            }
        }
    }

    fun setFilter(filter: StatsFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }
}
