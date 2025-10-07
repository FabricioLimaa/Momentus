package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import br.com.fabriciolima.momentus.data.RotinaRepository

class StatsViewModel(repository: RotinaRepository) : ViewModel() {

    /**
     * Expõe o fluxo de dados de estatísticas do repositório como LiveData,
     * para que a UI possa observá-lo.
     */
    val stats = repository.stats.asLiveData()
}

/**
 * Factory para criar instâncias de StatsViewModel com o repositório necessário.
 */
class StatsViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
