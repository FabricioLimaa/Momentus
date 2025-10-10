package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    val allRotinas: StateFlow<List<Rotina>> = repository.todasAsRotinasComMetas
        .map { listaRotinaComMeta ->
            listaRotinaComMeta.map { it.rotina } // Extrai apenas o objeto Rotina
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun upsertRotina(id: String?, nome: String, cor: String) {
        viewModelScope.launch {
            val rotina = Rotina(
                id = id ?: UUID.randomUUID().toString(),
                nome = nome,
                cor = cor,
                descricao = null,
                tag = null
            )
            repository.insertRotina(rotina)
        }
    }

    fun deleteRotina(rotina: Rotina) {
        viewModelScope.launch {
            repository.deleteRotina(rotina)
        }
    }
}
