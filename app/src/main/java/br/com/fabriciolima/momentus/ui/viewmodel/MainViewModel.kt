package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

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
