package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.SyncStatus
import br.com.fabriciolima.momentus.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val points: Int = 0,
    val streak: Int = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val categoriesWithMetas = categoryRepository.allCategoriesWithMetas.asLiveData()

    val syncStatus: StateFlow<SyncStatus> = categoryRepository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus.OFFLINE
    )

    val uiState: StateFlow<MainUiState> = combine(
        userRepository.userData, // Flow<UserData?>
        categoryRepository.currentStreak // Flow<Int>
    ) { userData, streak ->
        MainUiState(
            points = userData?.points ?: 0,
            streak = streak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )


    /**
     * Insere ou atualiza uma categoria no banco de dados.
     */
    fun insertCategory(category: Category) = viewModelScope.launch {
        categoryRepository.insertCategory(category)
    }

    /**
     * Deleta uma categoria do banco de dados.
     */
    fun deleteCategory(category: Category) = viewModelScope.launch {
        categoryRepository.deleteCategory(category)
    }

    /**
     * Insere ou atualiza uma meta no banco de dados.
     */
    fun saveMeta(meta: Meta) = viewModelScope.launch {
        categoryRepository.saveMeta(meta)
    }
}
