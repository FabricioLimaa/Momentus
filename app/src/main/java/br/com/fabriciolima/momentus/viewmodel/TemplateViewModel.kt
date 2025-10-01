// ARQUIVO: viewmodel/TemplateViewModel.kt
package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.data.TemplateComItens
import kotlinx.coroutines.launch

class TemplateViewModel(private val repository: RotinaRepository) : ViewModel() {
    // Agora o LiveData é do tipo TemplateComItens
    val todosOsTemplates: LiveData<List<TemplateComItens>> = repository.todosOsTemplatesComItens.asLiveData()

    fun saveTemplate(nome: String) = viewModelScope.launch {
        repository.saveCurrentScheduleAsTemplate(nome)
    }

    fun loadTemplate(template: Template) = viewModelScope.launch {
        repository.loadTemplateIntoSchedule(template.id)
    }

    fun deleteTemplate(template: Template) = viewModelScope.launch {
        repository.deleteTemplate(template)
    }
}

class TemplateViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TemplateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}