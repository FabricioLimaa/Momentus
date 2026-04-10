package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.domain.usecase.DeleteCategoryUseCase
import br.com.fabriciolima.momentus.domain.usecase.GetCategoriesUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpsertCategoryUseCase
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val dialogState: CategoryDialogState = CategoryDialogState.Hidden,
    val error: AppError? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    val allCategories: StateFlow<List<Category>> = getCategoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            allCategories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
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
        descricao: String? = null,
        tag: String? = null,
        duracaoPadraoMinutos: Int = 0
    ) {
        viewModelScope.launch {
            val result = upsertCategoryUseCase(id, nome, cor)
            
            when (result) {
                is Result.Success -> onDialogDismiss()
                is Result.Error -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }
    
    fun insertCategory(category: Category) {
        viewModelScope.launch {
            val result = upsertCategoryUseCase(
                id = category.id,
                nome = category.nome,
                cor = category.cor
            )
            
            if (result is Result.Error) {
                _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase(category)
            when (result) {
                is Result.Success -> onDialogDismiss()
                is Result.Error -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
