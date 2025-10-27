package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val rotinaRepository: RotinaRepository,
    private val eventoRepository: EventoRepository
) {
    suspend operator fun invoke(rotina: Rotina) {
        eventoRepository.deleteEventsByRotinaId(rotina.id)
        rotinaRepository.deleteRotina(rotina)
    }
}
