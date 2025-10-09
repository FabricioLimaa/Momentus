package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateEvent
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalTime

class CreateTemplateViewModel(private val repository: RotinaRepository) : ViewModel() {

    // CORREÇÃO: Usando o nome e o tipo que a Activity espera (TemplateEvent)
    private val _events = MutableLiveData<List<TemplateEvent>>(emptyList())
    val events: LiveData<List<TemplateEvent>> = _events

    private val _templateName = MutableLiveData("")
    val templateName: LiveData<String> = _templateName

    // CORREÇÃO: Mapeando corretamente de RotinaComMeta para Rotina
    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { list ->
        list.map { it.rotina }
    }.asLiveData()

    fun onTemplateNameChange(newName: String) {
        _templateName.value = newName
    }

    // CORREÇÃO: Nome e parâmetro alinhados com a Activity
    fun addEvent(event: TemplateEvent) {
        val currentList = _events.value ?: emptyList()
        _events.value = currentList + event
    }

    // CORREÇÃO: Adicionando a função que faltava
    fun removeEvent(event: TemplateEvent) {
        val currentList = _events.value ?: emptyList()
        _events.value = currentList - event
    }

    // CORREÇÃO: Nome e parâmetros alinhados com a Activity
    fun saveTemplate(onSaveFinished: () -> Unit) {
        val name = _templateName.value
        if (name.isNullOrBlank()) {
            // Adicionar tratamento de erro se o nome estiver vazio
            return
        }

        viewModelScope.launch {
            // 1. Criar e inserir o Template pai
            // CORREÇÃO: O construtor de Template não aceita 'descricao'.
            val newTemplate = Template(nome = name)
            repository.insertTemplate(newTemplate)

            // 2. Converter os eventos da UI (TemplateEvent) para o formato do DB (ItemCronograma)
            _events.value?.forEach { eventUI ->
                val eventDB = ItemCronograma(
                    titulo = eventUI.titulo,
                    descricao = eventUI.descricao,
                    data = null, // Template events don't have a date
                    diaDaSemana = null, // TODO: Consider adding day of week to TemplateEvent if needed
                    horarioInicio = LocalTime.parse(eventUI.horarioInicio),
                    horarioTermino = LocalTime.parse(eventUI.horarioTermino),
                    rotinaId = eventUI.categoria.id,
                    templateId = newTemplate.id // Associar com o novo template
                )
                repository.insertItemCronograma(eventDB)
            }

            // 3. Limpar o estado e notificar a UI
            _templateName.postValue("")
            _events.postValue(emptyList())
            onSaveFinished()
        }
    }
}
