// ARQUIVO: viewmodel/CronogramaViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import android.app.Application
import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. Mudamos o ViewModel para herdar de AndroidViewModel,
//    o que nos dá acesso ao "contexto" do aplicativo, necessário para o AlarmManager.
class CronogramaViewModel(
    private val repository: RotinaRepository,
    application: Application
) : AndroidViewModel(application) {
// --- MODIFICAÇÃO TERMINA AQUI ---

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinas.asLiveData()
    private var ultimoItemDeletado: ItemCronograma? = null

    fun getItensDoDia(dia: String): LiveData<List<ItemCronograma>> {
        return repository.getItensDoDia(dia).asLiveData()
    }

    fun insertItem(item: ItemCronograma) = viewModelScope.launch {
        repository.insertItemCronograma(item)
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Após inserir (ou atualizar) um item, agendamos (ou reagendamos) o alarme.
        val rotina = repository.todasAsRotinas.first().find { it.id == item.rotinaId }
        rotina?.let {
            AlarmScheduler.schedule(getApplication(), item, it)
        }
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }

    fun deleteItem(item: ItemCronograma) = viewModelScope.launch {
        ultimoItemDeletado = item
        repository.deleteItemCronograma(item)
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 3. Após deletar um item, cancelamos o alarme correspondente.
        AlarmScheduler.cancel(getApplication(), item)
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }

    fun reinsereItem() = viewModelScope.launch {
        ultimoItemDeletado?.let { item ->
            repository.insertItemCronograma(item)
            // --- MODIFICAÇÃO INICIA AQUI ---
            // 4. Se o usuário desfaz a deleção, agendamos o alarme novamente.
            val rotina = repository.todasAsRotinas.first().find { it.id == item.rotinaId }
            rotina?.let {
                AlarmScheduler.schedule(getApplication(), item, it)
            }
            // --- MODIFICAÇÃO TERMINA AQUI ---
        }
    }
}

// --- MODIFICAÇÃO INICIA AQUI ---
// 5. Atualizamos a Factory para passar a instância de 'Application' para o ViewModel.
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
// --- MODIFICAÇÃO TERMINA AQUI ---