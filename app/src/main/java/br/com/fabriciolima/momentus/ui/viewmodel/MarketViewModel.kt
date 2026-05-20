package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.MarketItem
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.data.repository.MarketRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val items: List<MarketItem> = emptyList(),
    val userData: UserData? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val purchaseSuccess: MarketItem? = null
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMarketData()
    }

    private fun loadMarketData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                marketRepository.getAvailableItems(),
                userRepository.userData
            ) { items, user ->
                _uiState.update { 
                    it.copy(
                        items = items,
                        userData = user,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun purchaseItem(item: MarketItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = marketRepository.purchaseItem(item)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        purchaseSuccess = item
                    )
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = AppError.UnknownError(e)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, purchaseSuccess = null) }
    }
}
