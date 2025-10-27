package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val rotinaRepository: RotinaRepository
) {
    operator fun invoke(): Flow<List<Rotina>> {
        return rotinaRepository.todasAsRotinasComMetas.map { list ->
            list.map { it.rotina }
        }
    }
}
