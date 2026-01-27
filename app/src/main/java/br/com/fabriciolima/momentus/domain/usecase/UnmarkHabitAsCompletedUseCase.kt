package br.com.fabriciolima.momentus.domain.usecase

import android.app.Application
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import javax.inject.Inject

class UnmarkHabitAsCompletedUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val application: Application
) {
    suspend operator fun invoke(habitId: String) {
        categoryRepository.unmarkHabitAsCompleted(habitId)
        WidgetUpdater.requestUpdate(application)
    }
}
