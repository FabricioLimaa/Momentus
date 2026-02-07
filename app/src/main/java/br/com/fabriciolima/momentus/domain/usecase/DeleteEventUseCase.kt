package br.com.fabriciolima.momentus.domain.usecase

import android.app.Application
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import javax.inject.Inject

class DeleteEventUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val application: Application
) {
    suspend operator fun invoke(item: ItemCronograma): Result<Unit> {
        // First, cancel any pending alarms for this event
        alarmScheduler.cancel(item)

        // Then, delete the event from the repository (which handles local and remote deletion)
        val deleteResult = categoryRepository.deleteCompleteEvent(item)

        // If deletion was successful, trigger a widget update
        if (deleteResult is Result.Success) {
            WidgetUpdater.requestUpdate(application)
        }

        return deleteResult
    }
}
