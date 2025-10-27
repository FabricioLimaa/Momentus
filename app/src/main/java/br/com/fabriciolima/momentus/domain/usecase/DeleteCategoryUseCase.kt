package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val eventoRepository: EventoRepository
) {
    suspend operator fun invoke(category: Category) {
        eventoRepository.deleteEventsByCategoryId(category.id)
        categoryRepository.deleteCategory(category)
    }
}
