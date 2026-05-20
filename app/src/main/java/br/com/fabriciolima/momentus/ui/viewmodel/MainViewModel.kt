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
    val points: Long = 0,
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
        userRepository.userData,
        categoryRepository.currentStreak
    ) { userData, streak ->
        MainUiState(
            points = userData?.points ?: 0L,
            streak = streak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )


    fun insertCategory(category: Category) = viewModelScope.launch {
        categoryRepository.insertCategory(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        categoryRepository.deleteCategory(category)
    }

    fun saveMeta(meta: Meta) = viewModelScope.launch {
        categoryRepository.saveMeta(meta)
    }
}
