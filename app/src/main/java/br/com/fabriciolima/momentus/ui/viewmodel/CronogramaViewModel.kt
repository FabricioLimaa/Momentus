package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CronogramaUiState(
    val itens: List<ItemCronograma> = emptyList(),
    val categories: List<Category> = emptyList(),
    val habitosConcluidos: Set<String> = emptySet()
)

@HiltViewModel
class CronogramaViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val uiState: LiveData<CronogramaUiState> = combine(
        scheduleRepository.allScheduleItems,
        categoryRepository.allCategoriesWithMetas,
        categoryRepository.idsHabitosConcluidos
    ) { itens: List<ItemCronograma>, categoriesWithMetas: List<CategoryWithMeta>, habitos: List<String> ->
        CronogramaUiState(
            itens = itens.sortedBy { it.ordem }, 
            categories = categoriesWithMetas.map { it.category },
            habitosConcluidos = habitos.toSet()
        )
    }.asLiveData()

    fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            categoryRepository.markHabitAsCompleted(itemCronogramaId)
        }
    }

    fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            categoryRepository.unmarkHabitAsCompleted(itemCronogramaId)
        }
    }

    fun onMove(fromId: String, toId: String) {
        viewModelScope.launch {
            val listaAtual = uiState.value?.itens?.toMutableList() ?: return@launch
            
            val fromIndex = listaAtual.indexOfFirst { it.id == fromId }
            val toIndex = listaAtual.indexOfFirst { it.id == toId }

            if (fromIndex != -1 && toIndex != -1) {
                val item = listaAtual.removeAt(fromIndex)
                listaAtual.add(toIndex, item)

                val itensAtualizados = listaAtual.mapIndexed { index, it ->
                    it.copy(ordem = index)
                }
                scheduleRepository.updateItems(itensAtualizados)
            }
        }
    }
}
