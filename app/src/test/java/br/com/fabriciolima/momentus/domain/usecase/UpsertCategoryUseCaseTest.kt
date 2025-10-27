package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.domain.exception.DuplicateCategoryNameException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UpsertCategoryUseCaseTest {

    private lateinit var useCase: UpsertCategoryUseCase
    private val categoryRepository: CategoryRepository = mock()

    @Before
    fun setUp() {
        useCase = UpsertCategoryUseCase(categoryRepository)
    }

    @Test
    fun `invoke should insert category when name is not a duplicate`() = runBlocking {
        // Arrange
        val categories = listOf(Category(id = "1", nome = "Trabalho"))
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(categories))

        // Act
        useCase(id = null, nome = "Estudo", cor = "#FFFFFF")

        // Assert
        verify(categoryRepository).insertCategory(any())
    }

    @Test(expected = DuplicateCategoryNameException::class)
    fun `invoke should throw exception when name is a duplicate`() = runBlocking {
        // Arrange
        val categories = listOf(Category(id = "1", nome = "Trabalho"))
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(categories))

        // Act
        useCase(id = null, nome = "Trabalho", cor = "#FFFFFF")

        // Assert
        verify(categoryRepository, never()).insertCategory(any())
    }
}
