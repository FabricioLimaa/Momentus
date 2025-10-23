package br.com.fabriciolima.momentus.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.ui.components.EventFormData
import br.com.fabriciolima.momentus.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

private const val TAG = "TemplateViewModel"

sealed interface TemplateDialogState {
    object Hidden : TemplateDialogState
    object CreateNew : TemplateDialogState
    data class Edit(val template: TemplateComEventos) : TemplateDialogState
    data class ConfirmDelete(val template: TemplateComEventos) : TemplateDialogState
    data class ApplyTemplate(val template: TemplateComEventos) : TemplateDialogState
}

data class TemplateUiState(
    val templates: List<TemplateComEventos> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap(),
    val dialogState: TemplateDialogState = TemplateDialogState.Hidden,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = true, // Novo estado para o indicador
    val error: String? = null
)

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val rotinaRepository: RotinaRepository,
    private val eventoRepository: EventoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            combine(
                templateRepository.todosOsTemplatesComEventos,
                rotinaRepository.getTodasAsRotinas()
            ) { templates, rotinas ->
                val rotinasMap = rotinas.associateBy { it.id }
                templates to rotinasMap
            }.collect { (templates, rotinasMap) ->
                _uiState.update {
                    it.copy(
                        templates = templates,
                        rotinasMap = rotinasMap,
                        isSyncing = false // Desativa o indicador quando os dados chegam
                    )
                }
            }
        }
    }

    fun onShowCreateDialog() {
        _uiState.update { it.copy(dialogState = TemplateDialogState.CreateNew) }
    }

    fun onShowEditDialog(template: TemplateComEventos) {
        _uiState.update { it.copy(dialogState = TemplateDialogState.Edit(template)) }
    }

    fun onShowDeleteDialog(template: TemplateComEventos) {
        _uiState.update { it.copy(dialogState = TemplateDialogState.ConfirmDelete(template)) }
    }

    fun onShowApplyDialog(template: TemplateComEventos) {
        _uiState.update { it.copy(dialogState = TemplateDialogState.ApplyTemplate(template)) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(dialogState = TemplateDialogState.Hidden) }
    }

    fun salvarTemplateCompleto(
        templateId: String?,
        nomeTemplate: String,
        eventosForm: List<EventFormData>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando salvarTemplateCompleto. Modo Edição: ${templateId != null}, ID: $templateId")
            _uiState.update { it.copy(isLoading = true) }
            try {
                val id = templateId ?: UUID.randomUUID().toString()
                val template = Template(id, nomeTemplate)
                val eventos = eventosForm.map {
                    ItemCronograma(
                        titulo = it.titulo,
                        descricao = it.descricao,
                        horarioInicio = it.horarioInicio,
                        horarioTermino = it.horarioTermino,
                        rotinaId = it.selectedRotina!!.id,
                        templateId = id
                    )
                }

                if (templateId != null) {
                    Log.d(TAG, "MODO EDIÇÃO: Deletando eventos antigos para o template ID: $templateId")
                    eventoRepository.deleteEventsByTemplateId(templateId)
                }
                
                Log.d(TAG, "Salvando template (Pai): ${template.id}")
                templateRepository.insertTemplate(template)

                Log.d(TAG, "Salvando ${eventos.size} eventos (Filhos) para o template ID: ${template.id}")
                eventoRepository.insertAll(eventos)

                onResult(Result.Success(Unit))
                _uiState.update { it.copy(dialogState = TemplateDialogState.Hidden) }
            } catch (e: Exception) {
                Log.e(TAG, "Erro em salvarTemplateCompleto", e)
                onResult(Result.Error(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                eventoRepository.deleteEventsByTemplateId(template.id)
                templateRepository.deleteTemplate(template)
                _uiState.update { it.copy(dialogState = TemplateDialogState.Hidden) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun applyTemplateToDates(templateId: String, dates: List<LocalDate>, saveToGoogle: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val templateComEventos = templateRepository.todosOsTemplatesComEventos.first().find { it.template.id == templateId }
                if (templateComEventos != null) {
                    val novosEventos = mutableListOf<ItemCronograma>()
                    dates.forEach { date ->
                        templateComEventos.eventos.forEach { eventoTemplate ->
                            val novoEvento = eventoTemplate.copy(
                                id = UUID.randomUUID().toString(),
                                data = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
                                templateId = null
                            )
                            novosEventos.add(novoEvento)
                        }
                    }
                    
                    if(saveToGoogle) {
                        novosEventos.forEach { evento ->
                            val rotina = rotinaRepository.getTodasAsRotinas().first().find { it.id == evento.rotinaId }
                            if (rotina != null) {
                                rotinaRepository.salvarEventoNoGoogle(
                                    titulo = evento.titulo,
                                    descricao = evento.descricao,
                                    data = LocalDate.ofEpochDay(evento.data!! / (24 * 60 * 60 * 1000)),
                                    horarioInicio = evento.horarioInicio,
                                    horarioTermino = evento.horarioTermino,
                                    cor = rotina.cor
                                )
                            }
                        }
                    }

                    eventoRepository.insertAll(novosEventos)
                    _uiState.update { it.copy(dialogState = TemplateDialogState.Hidden) }
                    onResult(Result.Success(Unit))
                } else {
                    onResult(Result.Error(Exception("Template não encontrado.")))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                onResult(Result.Error(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
