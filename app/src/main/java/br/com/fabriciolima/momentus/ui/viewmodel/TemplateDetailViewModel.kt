package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.model.SharedTemplate
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalTime
import javax.inject.Inject

data class TemplateDetailUiState(
    val template: TemplateComEventos? = null,
    val rotinasMap: Map<String, Rotina> = emptyMap()
)

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    private val repository: RotinaRepository,
    private val templateRepository: TemplateRepository,
    private val eventoRepository: EventoRepository
) : ViewModel() {

    private val _templateId = MutableStateFlow<String?>("")

    val uiState: StateFlow<TemplateDetailUiState> = combine(
        _templateId,
        templateRepository.todosOsTemplatesComEventos,
        repository.todasAsRotinasComMetas
    ) { id: String?, templates: List<TemplateComEventos>, rotinas: List<RotinaComMeta> ->
        val template = templates.firstOrNull { it.template.id == id }
        val rotinasMap = rotinas.associateBy({ it.rotina.id }, { it.rotina })
        TemplateDetailUiState(template, rotinasMap)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemplateDetailUiState()
    )


    fun loadTemplate(id: String) {
        _templateId.value = id
    }

    fun getShareableJson(): String? {
        val currentTemplate = uiState.value.template?.template ?: return null
        val currentEventos = uiState.value.template?.eventos ?: return null

        val shareableData = SharedTemplate(
            template = currentTemplate,
            eventos = currentEventos
        )
        return Json.encodeToString(shareableData)
    }

    fun addEventToTemplate(titulo: String, descricao: String?, horarioInicio: LocalTime, horarioTermino: LocalTime, rotina: Rotina) {
        val templateId = _templateId.value ?: return
        viewModelScope.launch {
            val novoEvento = ItemCronograma(
                titulo = titulo,
                descricao = descricao,
                data = null,
                diaDaSemana = null,
                horarioInicio = horarioInicio,
                horarioTermino = horarioTermino,
                rotinaId = rotina.id,
                templateId = templateId
            )
            eventoRepository.insertItemCronograma(novoEvento)
        }
    }

    fun reorderEventos(fromId: String, toId: String) {
        viewModelScope.launch {
            // TODO: Persistir a nova ordem requer uma mudança no banco de dados.
            // O modelo de dados atual do 'ItemCronograma' não possui um campo de 'ordem' ou 'posição'.
            // Para salvar corretamente a lista reordenada, você deve:
            // 1. Adicionar um campo 'orderIndex: Int' à classe de dados ItemCronograma.
            // 2. Criar uma migração de banco de dados do Room para a alteração.
            // 3. Nesta função, obter a lista, reordená-la, atualizar o 'orderIndex'
            //    para todos os itens da lista e, em seguida, chamar repository.updateItensCronograma(reorderedList).
            // Por enquanto, esta função é um placeholder para resolver o erro de compilação.
        }
    }
}
