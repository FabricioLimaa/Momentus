package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: RotinaRepository
) : ViewModel() {

    /**
     * Expõe o fluxo de dados de estatísticas do repositório como LiveData,
     * para que a UI possa observá-lo.
     */
    val stats = repository.stats.asLiveData()
}
