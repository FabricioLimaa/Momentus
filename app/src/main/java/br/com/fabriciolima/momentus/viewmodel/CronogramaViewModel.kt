// ARQUIVO: viewmodel/CronogramaViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import android.app.Application
import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map // Adicione este import
import kotlinx.coroutines.launch

class CronogramaViewModel(
    private val repository: RotinaRepository,
    application: Application
) : AndroidViewModel(application) {

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Usamos a propriedade correta 'todasAsRotinasComMetas'.
    // 2. Como ela retorna Flow<List<RotinaComMeta>>, usamos .map { it.rotina }
    //    para transformar o fluxo em um Flow<List<Rotina>>, que é o que a tela espera.
    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { listaComMetas ->
        listaComMetas.map { it.rotina }
    }.asLiveData()
    // --- MODIFICAÇÃO TERMINA AQUI ---

    private var ultimoItemDeletado: ItemCronograma? = null

    fun getItensDoDia(dia: String): LiveData<List<ItemCronograma>> {
        return repository.getItensDoDia(dia).asLiveData()
    }

    fun insertItem(item: ItemCronograma) = viewModelScope.launch {
        repository.insertItemCronograma(item)
        // Usamos .first() para pegar o valor atual do Flow de forma síncrona dentro da coroutine
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