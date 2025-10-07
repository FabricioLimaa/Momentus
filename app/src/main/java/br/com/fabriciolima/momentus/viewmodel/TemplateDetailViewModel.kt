package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.TemplateComEventos
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TemplateDetailUiState(
    val template: TemplateComEventos? = null,
    val rotinasMap: Map<String, Rotina> = emptyMap()
)

class TemplateDetailViewModel(private val repository: RotinaRepository) : ViewModel() {

    private val _templateId = MutableLiveData<String>()

    val uiState: LiveData<TemplateDetailUiState> = combine(
        _templateId.asFlow(),
        repository.todosOsTemplatesComEventos,
        repository.todasAsRotinasComMetas
    ) { id, templates, rotinas ->
        val template = templates.firstOrNull { it.template.id == id }
        val rotinasMap = rotinas.associateBy({ it.rotina.id }, { it.rotina })
        TemplateDetailUiState(template, rotinasMap)
    }.asLiveData()


    fun loadTemplate(id: String) {
        _templateId.value = id
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
            repository.insertItemCronograma(novoEvento)
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
