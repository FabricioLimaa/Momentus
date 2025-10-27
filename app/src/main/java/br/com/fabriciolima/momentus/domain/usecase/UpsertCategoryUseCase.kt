package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.domain.exception.DuplicateCategoryNameException
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class UpsertCategoryUseCase @Inject constructor(
    private val rotinaRepository: RotinaRepository
) {
    suspend operator fun invoke(id: String?, nome: String, cor: String) {
        val currentCategories = rotinaRepository.todasAsRotinasComMetas.first().map { it.rotina }
        val isDuplicate = currentCategories.any { it.nome.equals(nome, ignoreCase = true) && it.id != id }

        if (isDuplicate) {
            throw DuplicateCategoryNameException()
        }

        val rotina = Rotina(
            id = id ?: UUID.randomUUID().toString(),
            nome = nome.trim(),
            cor = cor,
            descricao = null,
            tag = null
        )
        rotinaRepository.insertRotina(rotina)
    }
}
