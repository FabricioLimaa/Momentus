package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class CategoryViewModel(private val repository: RotinaRepository) : ViewModel() {

    // CORREÇÃO: Mapeando o fluxo para extrair apenas a Rotina do objeto RotinaComMeta
    val allRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas
        .map { listaRotinaComMeta ->
            listaRotinaComMeta.map { it.rotina } // Extrai apenas o objeto Rotina
        }.asLiveData()

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
