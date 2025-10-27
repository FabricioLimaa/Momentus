package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateEvent
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class CreateTemplateViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val templateRepository: TemplateRepository,
    private val eventoRepository: EventoRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<TemplateEvent>>(emptyList())
    val events: StateFlow<List<TemplateEvent>> = _events.asStateFlow()

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    val allCategories: StateFlow<List<Category>> = categoryRepository.allCategoriesWithMetas.map { list ->
        list.map { it.category }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onTemplateNameChange(newName: String) {
        _templateName.value = newName
    }

    fun addEvent(event: TemplateEvent) {
        _events.value = _events.value + event
    }

    fun removeEvent(event: TemplateEvent) {
        _events.value = _events.value - event
    }

    fun saveTemplate(onResult: (Result<Unit>) -> Unit) {
        val name = _templateName.value
        if (name.isBlank()) {
            onResult(Result.Error(Exception("O nome do template não pode estar vazio.")))
            return
        }

        viewModelScope.launch {
            try {
                val newTemplate = Template(nome = name)
                templateRepository.insertTemplate(newTemplate)

                _events.value.forEach { eventUI ->
                    val eventDB = ItemCronograma(
                        titulo = eventUI.titulo,
                        descricao = eventUI.descricao,
                        data = null,
                        diaDaSemana = null,
                        horarioInicio = LocalTime.parse(eventUI.horarioInicio),
                        horarioTermino = LocalTime.parse(eventUI.horarioTermino),
                        categoryId = eventUI.categoria.id,
                        templateId = newTemplate.id
                    )
                    eventoRepository.insertItemCronograma(eventDB)
                }
                _templateName.value = ""
                _events.value = emptyList()
                onResult(Result.Success(Unit))
            } catch (e: Exception) {
                onResult(Result.Error(e))
            }
        }
    }
}
