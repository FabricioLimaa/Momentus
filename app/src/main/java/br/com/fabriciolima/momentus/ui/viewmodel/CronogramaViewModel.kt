package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CronogramaUiState(
    val itens: List<ItemCronograma> = emptyList(),
    val rotinas: List<Rotina> = emptyList(),
    val habitosConcluidos: Set<String> = emptySet()
)

@HiltViewModel
class CronogramaViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    val uiState: LiveData<CronogramaUiState> = combine(
        repository.todosOsItensDoCronograma,
        repository.todasAsRotinasComMetas,
        repository.idsHabitosConcluidos
    ) { itens, rotinasComMetas, habitos ->
        CronogramaUiState(
            itens = itens.sortedBy { it.ordem }, // Garante a ordem correta
            rotinas = rotinasComMetas.map { it.rotina },
            habitosConcluidos = habitos.toSet()
        )
    }.asLiveData()

    fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.marcarHabitoComoConcluido(itemCronogramaId)
        }
    }

    fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.desmarcarHabitoComoConcluido(itemCronogramaId)
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

                // Atualiza a propriedade 'ordem' de todos os itens
                val itensAtualizados = listaAtual.mapIndexed { index, it ->
                    it.copy(ordem = index)
                }
                repository.updateItensCronograma(itensAtualizados)
            }
        }
    }
}
