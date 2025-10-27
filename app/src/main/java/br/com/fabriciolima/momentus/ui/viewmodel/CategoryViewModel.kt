package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.domain.usecase.DeleteCategoryUseCase
import br.com.fabriciolima.momentus.domain.usecase.GetCategoriesUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpsertCategoryUseCase
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
    data class Edit(val category: Rotina) : CategoryDialogState
    data class ConfirmDelete(val category: Rotina) : CategoryDialogState
}

data class CategoryUiState(
    val categories: List<Rotina> = emptyList(),
    val dialogState: CategoryDialogState = CategoryDialogState.Hidden,
    val error: String? = null // Adicionado para mensagens de erro
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val allRotinas: StateFlow<List<Rotina>> = getCategoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            allRotinas.collect { rotinas ->
                _uiState.update { it.copy(categories = rotinas) }
            }
        }
    }

    fun onShowCreateDialog() {
        _uiState.update { it.copy(dialogState = CategoryDialogState.CreateNew) }
    }

    fun onShowEditDialog(category: Rotina) {
        _uiState.update { it.copy(dialogState = CategoryDialogState.Edit(category)) }
    }

    fun onShowConfirmDeleteDialog(category: Rotina) {
        _uiState.update { it.copy(dialogState = CategoryDialogState.ConfirmDelete(category)) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(dialogState = CategoryDialogState.Hidden) }
    }

    fun upsertRotina(id: String?, nome: String, cor: String) {
        viewModelScope.launch {
            try {
                upsertCategoryUseCase(id, nome, cor)
                onDialogDismiss()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteRotina(rotina: Rotina) {
        viewModelScope.launch {
            deleteCategoryUseCase(rotina)
            onDialogDismiss()
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
