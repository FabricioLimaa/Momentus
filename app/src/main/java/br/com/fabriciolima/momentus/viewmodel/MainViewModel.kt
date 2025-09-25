// ARQUIVO: viewmodel/MainViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.Meta
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaComMeta
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: RotinaRepository) : ViewModel() {

    val rotinas: LiveData<List<RotinaComMeta>> = repository.todasAsRotinasComMetas.asLiveData()
    private var ultimaRotinaDeletada: Rotina? = null

    fun addRotina(novaRotina: Rotina) = viewModelScope.launch {
        repository.insert(novaRotina)
    }

    fun deleteRotina(rotina: Rotina) = viewModelScope.launch {
        ultimaRotinaDeletada = rotina
        repository.delete(rotina)
    }

    fun reinsereRotina() = viewModelScope.launch {
        ultimaRotinaDeletada?.let {
            repository.insert(it)
        }
    }

    fun salvarMeta(rotinaId: String, metaHoras: Int) = viewModelScope.launch {
        val metaMinutos = metaHoras * 60
        val novaMeta = Meta(rotinaId = rotinaId, metaMinutosSemanal = metaMinutos)
        repository.salvarMeta(novaMeta)
    }
}

// A CLASSE MainViewModelFactory FOI REMOVIDA DESTE ARQUIVO