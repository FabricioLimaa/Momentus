package br.com.fabriciolima.momentus.domain.usecase

import android.app.Application
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import javax.inject.Inject

class DeleteScheduleItemUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val application: Application
) {
    suspend operator fun invoke(item: ItemCronograma): Result<Unit> {
        // 1. Cancela qualquer alarme pendente para este item
        alarmScheduler.cancel(item)

        // 2. Deleta o item do repositório
        val deleteResult = scheduleRepository.deleteScheduleItem(item)

        // 3. Se a deleção foi bem sucedida, atualiza o widget
        if (deleteResult is Result.Success) {
            WidgetUpdater.requestUpdate(application)
        }

        return deleteResult
    }
}
