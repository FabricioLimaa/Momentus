package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.domain.usecase.GetStatsUseCase
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class StatsFilter(val days: Int) {
    WEEK(7),
    MONTH(30),
    YEAR(365)
}

data class CompletionRate(val categoryName: String, val categoryColor: String, val percentage: Float)
data class BarChartData(val label: String, val value: Int, val color: String)

data class StatsUiState(
    val filter: StatsFilter = StatsFilter.MONTH,
    val completionRates: List<CompletionRate> = emptyList(),
    val barChartData: List<BarChartData> = emptyList(),
    val streakCount: Int = 0,
    val totalCompletions: Int = 0,
    val bestCategory: String? = null,
    val dailyAverage: Float = 0f,
    val completionDates: List<LocalDate> = emptyList(),
    val improvementPercentage: Int = 0,
    val bestHour: Int? = null,
    val error: AppError? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getStatsUseCase: GetStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsData = _uiState
        .map { it.filter }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            getStatsUseCase(filter)
        }

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            statsData.collect { result ->
                when (result) {
                    is Result.Success -> {
                        val data = result.data
                        _uiState.update { it.copy(
                            completionRates = data.completionRates,
                            barChartData = data.barChartData,
                            streakCount = data.streakCount,
                            totalCompletions = data.totalCompletions,
                            bestCategory = data.bestCategory,
                            dailyAverage = data.dailyAverage,
                            completionDates = data.completionDates,
                            improvementPercentage = data.improvementPercentage,
                            bestHour = data.bestHour,
                            error = null,
                            isLoading = false
                        )}
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(error = result.error, isLoading = false) }
                    }
                }
            }
        }
    }

    fun setFilter(filter: StatsFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
