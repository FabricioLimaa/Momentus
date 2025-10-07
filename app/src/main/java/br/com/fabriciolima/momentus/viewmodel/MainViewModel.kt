package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.Meta
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: RotinaRepository) : ViewModel() {

    // CORREÇÃO: Adicionando a propriedade que estava faltando na EditorRotinaActivity.
    val rotinasComMetas = repository.todasAsRotinasComMetas.asLiveData()

    /**
     * Insere ou atualiza uma rotina (categoria) no banco de dados.
     */
    fun insertRotina(rotina: Rotina) = viewModelScope.launch {
        repository.insertRotina(rotina)
    }

    /**
     * Deleta uma rotina do banco de dados.
     */
    fun deleteRotina(rotina: Rotina) = viewModelScope.launch {
        repository.deleteRotina(rotina)
    }

    /**
     * Insere ou atualiza uma meta no banco de dados.
     */
    fun salvarMeta(meta: Meta) = viewModelScope.launch {
        repository.salvarMeta(meta)
    }
}
