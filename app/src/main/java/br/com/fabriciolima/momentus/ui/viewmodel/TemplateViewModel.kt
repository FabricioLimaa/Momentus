package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

sealed interface TemplateDialogState {
    object Hidden : TemplateDialogState
    object CreateNew : TemplateDialogState
    data class ConfirmDelete(val template: TemplateComEventos) : TemplateDialogState
    data class ApplyTemplate(val template: TemplateComEventos) : TemplateDialogState
}

data class TemplateUiState(
    val templates: List<TemplateComEventos> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap(),
    val error: String? = null,
    val dialogState: TemplateDialogState = TemplateDialogState.Hidden
)

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.todosOsTemplatesComEventos,
                repository.todasAsRotinasComMetas
            ) { templates, rotinasComMetas ->
                val rotinasMap = rotinasComMetas.associateBy({ it.rotina.id }, { it.rotina })
                Pair(templates, rotinasMap)
            }.collect { (templates, rotinasMap) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        templates = templates,
                        rotinasMap = rotinasMap
                    )
                }
            }
        }
    }

    fun onShowCreateDialog() {
        _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.CreateNew)
    }

    fun onShowDeleteDialog(template: TemplateComEventos) {
        _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.ConfirmDelete(template))
    }

    fun onShowApplyDialog(template: TemplateComEventos) {
        _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.ApplyTemplate(template))
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.Hidden)
    }

    fun salvarTemplateCompleto(nomeTemplate: String, eventosData: List<EventFormData>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            if (nomeTemplate.isBlank()) {
                onResult(Result.Error(Exception("O nome do template não pode estar vazio.")))
                return@launch
            }
            try {
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
                _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.Hidden)
                onResult(Result.Success(Unit))
            } catch (e: Exception) {
                onResult(Result.Error(e))
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val templateToDelete = Template(id = templateId, nome = "")
                repository.deleteTemplate(templateToDelete)
                _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.Hidden)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Falha ao deletar o template.")
            }
        }
    }

    fun applyTemplateToDates(templateId: String, dates: List<LocalDate>) {
        viewModelScope.launch {
            try {
                val templateWithEvents = repository.todosOsTemplatesComEventos.first()
                    .find { it.template.id == templateId }
                
                if (templateWithEvents == null) {
                    _uiState.value = _uiState.value.copy(error = "Template não encontrado.")
                    return@launch
                }

                val newEvents = dates.flatMap { date ->
                    templateWithEvents.eventos.map { templateEvent ->
                        templateEvent.copy(
                            id = UUID.randomUUID().toString(),
                            data = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            templateId = null
                        )
                    }
                }
                newEvents.forEach { repository.insertItemCronograma(it) }
                _uiState.value = _uiState.value.copy(dialogState = TemplateDialogState.Hidden)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Falha ao aplicar o template.")
            }
        }
    }

    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
