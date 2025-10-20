package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository

/**
 * Factory para criar instâncias de CreateTemplateViewModel.
 * Recebe o repositório e o injeta na ViewModel.
 */
class CreateTemplateViewModelFactory(
    private val repository: RotinaRepository,
    private val templateRepository: TemplateRepository,
    private val eventoRepository: EventoRepository
    ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTemplateViewModel::class.java)) {
            return CreateTemplateViewModel(repository, templateRepository, eventoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
