package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.ui.components.EventFormData
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class TemplateUiState(
    val templates: List<TemplateComEventos> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap()
)

class TemplateViewModel(private val repository: RotinaRepository) : ViewModel() {

    val uiState: LiveData<TemplateUiState> = combine(
        repository.todosOsTemplatesComEventos,
        repository.todasAsRotinasComMetas
    ) { templates, rotinasComMetas ->
        val rotinasMap = rotinasComMetas.associateBy({ it.rotina.id }, { it.rotina })
        TemplateUiState(templates, rotinasMap)
    }.asLiveData()

    fun salvarTemplateCompleto(nomeTemplate: String, eventosData: List<EventFormData>) {
        viewModelScope.launch {
            val novoTemplate = Template(id = UUID.randomUUID().toString(), nome = nomeTemplate)
            repository.insertTemplate(novoTemplate)

            val novosItens = eventosData.mapNotNull { formData ->
                formData.selectedRotina?.let {
                    ItemCronograma(
                        titulo = formData.titulo,
                        descricao = formData.descricao,
                        horarioInicio = formData.horarioInicio,
                        horarioTermino = formData.horarioTermino,
                        rotinaId = it.id,
                        templateId = novoTemplate.id,
                        diaDaSemana = null,
                        data = null
                    )
                }
            }
            novosItens.forEach { repository.insertItemCronograma(it) }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            // CORREÇÃO: O repositório espera um objeto Template, não apenas o ID.
            val templateToDelete = Template(id = templateId, nome = "") // O nome não importa para a exclusão
            repository.deleteTemplate(templateToDelete)
        }
    }

    fun applyTemplateToDates(templateId: String, dates: List<LocalDate>) {
        viewModelScope.launch {
            val templateWithEvents = repository.todosOsTemplatesComEventos.first()
                .find { it.template.id == templateId } ?: return@launch

            val newEvents = dates.flatMap { date ->
                templateWithEvents.eventos.map { templateEvent ->
                    templateEvent.copy(
                        id = UUID.randomUUID().toString(),
                        data = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        templateId = null // É um evento concreto, não mais parte de um template
                    )
                }
            }

            newEvents.forEach { repository.insertItemCronograma(it) }
        }
    }
}
