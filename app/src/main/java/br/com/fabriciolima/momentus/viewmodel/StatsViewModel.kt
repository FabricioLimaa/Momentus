// ARQUIVO: viewmodel/StatsViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.StatsResult

class StatsViewModel(private val repository: RotinaRepository) : ViewModel() {
    val stats: LiveData<List<StatsResult>> = repository.stats.asLiveData()
}

class StatsViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}