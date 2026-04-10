package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.util.Result
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        return try {
            scheduleRepository.deleteItemsByCategoryId(category.id)
            categoryRepository.deleteCategory(category)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }
}
