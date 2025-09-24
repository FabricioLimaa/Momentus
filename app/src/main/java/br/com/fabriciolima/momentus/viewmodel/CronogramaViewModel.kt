// ARQUIVO: viewmodel/CronogramaViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.launch

class CronogramaViewModel(private val repository: RotinaRepository) : ViewModel() {

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinas.asLiveData()

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Variável para guardar o último item do cronograma que foi deletado.
    private var ultimoItemDeletado: ItemCronograma? = null
    // --- MODIFICAÇÃO TERMINA AQUI ---

    fun getItensDoDia(dia: String): LiveData<List<ItemCronograma>> {
        return repository.getItensDoDia(dia).asLiveData()
    }

    fun insertItem(item: ItemCronograma) = viewModelScope.launch {
        repository.insertItemCronograma(item)
    }

    fun deleteItem(item: ItemCronograma) = viewModelScope.launch {
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Antes de deletar, guardamos o item na nossa variável temporária.
        ultimoItemDeletado = item
        // --- MODIFICAÇÃO TERMINA AQUI ---
        repository.deleteItemCronograma(item)
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 3. Nova função para reinserir o último item deletado.
    fun reinsereItem() = viewModelScope.launch {
        ultimoItemDeletado?.let {
            repository.insertItemCronograma(it)
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}

class CronogramaViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CronogramaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CronogramaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}