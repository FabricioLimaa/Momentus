package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: RotinaRepository
) : ViewModel() {

    val rotinasComMetas = repository.todasAsRotinasComMetas.asLiveData()

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus.OFFLINE
    )

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
