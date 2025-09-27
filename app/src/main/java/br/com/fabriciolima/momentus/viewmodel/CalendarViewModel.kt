// ARQUIVO: viewmodel/CalendarViewModel.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.fabriciolima.momentus.data.RotinaRepository

// Por enquanto, este ViewModel será simples. Podemos adicionar mais lógica depois.
class CalendarViewModel(private val repository: RotinaRepository) : ViewModel() {
    // No futuro, podemos adicionar aqui um Flow que busca os dias que têm eventos
    // para podermos destacá-los no calendário.
}

class CalendarViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}