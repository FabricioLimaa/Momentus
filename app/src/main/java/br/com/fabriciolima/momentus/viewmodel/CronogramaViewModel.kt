package br.com.fabriciolima.momentus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map // <-- ESTE É O IMPORT CRUCIAL E CORRETO
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class CronogramaViewModel(
    private val repository: RotinaRepository,
    application: Application
) : AndroidViewModel(application) {

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas
        .map { listaComMetas -> // Este .map agora é o do kotlinx.coroutines.flow
            listaComMetas.map { it.rotina }
        }
        .asLiveData()

    private var ultimoItemDeletado: ItemCronograma? = null

    fun getItensDoDiaCompletados(dia: String): LiveData<List<ItemCronogramaCompletado>> {
        return repository.getItensDoDia(dia).combine(repository.idsHabitosConcluidos) { itens, idsConcluidos ->
            itens.map { item ->
                ItemCronogramaCompletado(item = item, completado = idsConcluidos.contains(item.id))
            }
        }.asLiveData()
    }

    fun onHabitoConcluidoChanged(item: ItemCronograma, isChecked: Boolean) = viewModelScope.launch {
        if (isChecked) {
            repository.marcarHabitoComoConcluido(item.id)
        } else {
            repository.desmarcarHabitoComoConcluido(item.id)
        }
    }

    fun insertItem(item: ItemCronograma) = viewModelScope.launch {
        repository.insertItemCronograma(item)
        val rotina = repository.todasAsRotinasComMetas.first().find { it.rotina.id == item.rotinaId }?.rotina
        rotina?.let {
            AlarmScheduler.schedule(getApplication(), item, it)
        }
    }

    fun deleteItem(item: ItemCronograma) = viewModelScope.launch {
        ultimoItemDeletado = item
        repository.deleteItemCronograma(item)
        AlarmScheduler.cancel(getApplication(), item)
    }

    fun reinsereItem() = viewModelScope.launch {
        ultimoItemDeletado?.let { item ->
            repository.insertItemCronograma(item)
            val rotina = repository.todasAsRotinasComMetas.first().find { it.rotina.id == item.rotinaId }?.rotina
            rotina?.let {
                AlarmScheduler.schedule(getApplication(), item, it)
            }
        }
    }
}

class CronogramaViewModelFactory(
    private val repository: RotinaRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CronogramaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CronogramaViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}