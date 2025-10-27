package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.domain.exception.DuplicateCategoryNameException
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class UpsertCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String?, nome: String, cor: String) {
        val currentCategories = categoryRepository.allCategoriesWithMetas.first().map { it.category }
        val isDuplicate = currentCategories.any { it.nome.equals(nome, ignoreCase = true) && it.id != id }

        if (isDuplicate) {
            throw DuplicateCategoryNameException()
        }

        val category = Category(
            id = id ?: UUID.randomUUID().toString(),
            nome = nome.trim(),
            cor = cor,
            descricao = null,
            tag = null
        )
        categoryRepository.insertCategory(category)
    }
}
