package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.MarketItem
import br.com.fabriciolima.momentus.data.repository.MarketRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.domain.usecase.DeleteCategoryUseCase
import br.com.fabriciolima.momentus.domain.usecase.GetCategoriesUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpsertCategoryUseCase
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CategoryDialogState {
    object Hidden : CategoryDialogState
    object CreateNew : CategoryDialogState
    data class Edit(val category: Category) : CategoryDialogState
    data class ConfirmDelete(val category: Category) : CategoryDialogState
}

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val ownedStickers: List<MarketItem> = emptyList(),
    val dialogState: CategoryDialogState = CategoryDialogState.Hidden,
    val successMessage: String? = null,
    val error: AppError? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getCategoriesUseCase(),
                marketRepository.getAvailableItems()
            ) { categories, marketItems ->
                val ownedStickers = marketItems.filter { it.isOwned && it.type == br.com.fabriciolima.momentus.data.model.ItemType.STICKER }
                _uiState.update { it.copy(categories = categories, ownedStickers = ownedStickers) }
            }.collect {}
        }
    }

    fun onShowCreateDialog() {
        _uiState.update { it.copy(dialogState = CategoryDialogState.CreateNew) }
    }

    fun onShowEditDialog(category: Category) {
        _uiState.update { it.copy(dialogState = CategoryDialogState.Edit(category)) }
    }

    fun onShowConfirmDeleteDialog(category: Category) {
        _uiState.update { it.copy(dialogState = CategoryDialogState.ConfirmDelete(category)) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(dialogState = CategoryDialogState.Hidden) }
    }

    fun upsertCategory(
        id: String?,
        nome: String,
        cor: String,
        stickerId: String? = null
    ) {
        viewModelScope.launch {
            val result = upsertCategoryUseCase(id, nome, cor, stickerId)
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = if (id == null) "Categoria criada!" else "Alterações salvas!") }
                    onDialogDismiss()
                }
                is Result.Error -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase(category)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Categoria excluída com sucesso.") }
                    onDialogDismiss()
                }
                is Result.Error -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
