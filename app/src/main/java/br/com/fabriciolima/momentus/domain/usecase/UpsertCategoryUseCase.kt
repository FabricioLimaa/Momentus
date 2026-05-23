package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class UpsertCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String?, nome: String, cor: String, stickerId: String? = null): Result<Unit> {
        val currentCategories = categoryRepository.allCategoriesWithMetas.first().map { it.category }
        val isDuplicate = currentCategories.any { it.nome.equals(nome, ignoreCase = true) && it.id != id }

        if (isDuplicate) {
            return Result.Error(AppError.DuplicateCategoryNameError(nome))
        }

        val category = Category(
            id = id ?: UUID.randomUUID().toString(),
            nome = nome.trim(),
            cor = cor,
            stickerId = stickerId,
            descricao = null,
            tag = null
        )
        
        return try {
            categoryRepository.insertCategory(category)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }
}
