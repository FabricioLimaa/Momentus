package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WidgetConfigurationViewModel @Inject constructor(
    rotinaRepository: RotinaRepository
) : ViewModel() {

    val rotinas: StateFlow<List<Rotina>> = rotinaRepository.getTodasAsRotinas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
