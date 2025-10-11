package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface CategoryDialogState {
    object Hidden : CategoryDialogState
    object CreateNew : CategoryDialogState
    data class Edit(val category: Rotina) : CategoryDialogState
    data class ConfirmDelete(val category: Rotina) : CategoryDialogState
}

data class CategoryUiState(
    val categories: List<Rotina> = emptyList(),
    val dialogState: CategoryDialogState = CategoryDialogState.Hidden
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    val allRotinas: StateFlow<List<Rotina>> = repository.todasAsRotinasComMetas
        .map { listaRotinaComMeta ->
            listaRotinaComMeta.map { it.rotina } // Extrai apenas o objeto Rotina
        }.stateIn(
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
        _uiState.value = _uiState.value.copy(dialogState = CategoryDialogState.CreateNew)
    }

    fun onShowEditDialog(category: Rotina) {
        _uiState.value = _uiState.value.copy(dialogState = CategoryDialogState.Edit(category))
    }

    fun onShowConfirmDeleteDialog(category: Rotina) {
        _uiState.value = _uiState.value.copy(dialogState = CategoryDialogState.ConfirmDelete(category))
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(dialogState = CategoryDialogState.Hidden)
    }

    fun upsertRotina(id: String?, nome: String, cor: String) {
        viewModelScope.launch {
            val rotina = Rotina(
                id = id ?: UUID.randomUUID().toString(),
                nome = nome,
                cor = cor,
                descricao = null,
                tag = null
            )
            repository.insertRotina(rotina)
            onDialogDismiss() // Esconde o diálogo após a operação
        }
    }

    fun deleteRotina(rotina: Rotina) {
        viewModelScope.launch {
            repository.deleteRotina(rotina)
            onDialogDismiss() // Esconde o diálogo após a operação
        }
    }
}
