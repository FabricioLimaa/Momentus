// ARQUIVO: viewmodel/CreateTemplateViewModel.kt
package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.TemplateEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CreateTemplateViewModel(private val repository: RotinaRepository) : ViewModel() {

    // LiveData para a lista de todas as categorias/rotinas disponíveis
    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { listaComMetas ->
        listaComMetas.map { it.rotina }
    }.asLiveData()

    // Estado interno da tela
    private val _templateName = MutableLiveData("")
    val templateName: LiveData<String> = _templateName

    private val _events = MutableLiveData<List<TemplateEvent>>(emptyList())
    val events: LiveData<List<TemplateEvent>> = _events

    fun onTemplateNameChange(newName: String) {
        _templateName.value = newName
    }

    fun addEvent(event: TemplateEvent) {
        val currentList = _events.value ?: emptyList()
        _events.value = currentList + event
    }

    fun removeEvent(event: TemplateEvent) {
        val currentList = _events.value ?: emptyList()
        _events.value = currentList - event
    }

    fun saveTemplate(onSuccess: () -> Unit) = viewModelScope.launch {
        val name = _templateName.value ?: ""
        val eventList = _events.value ?: emptyList()

        if (name.isNotBlank() && eventList.isNotEmpty()) {
            repository.saveCompleteTemplate(name, eventList)
            onSuccess() // Chama o callback de sucesso
        }
    }
}

class CreateTemplateViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateTemplateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}