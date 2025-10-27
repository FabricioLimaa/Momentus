package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categoriesWithMetas = repository.allCategoriesWithMetas.asLiveData()

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus.OFFLINE
    )

    /**
     * Insere ou atualiza uma categoria no banco de dados.
     */
    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    /**
     * Deleta uma categoria do banco de dados.
     */
    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    /**
     * Insere ou atualiza uma meta no banco de dados.
     */
    fun saveMeta(meta: Meta) = viewModelScope.launch {
        repository.saveMeta(meta)
    }
}
