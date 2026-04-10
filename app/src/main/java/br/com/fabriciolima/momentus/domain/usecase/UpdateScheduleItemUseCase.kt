package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

class UpdateScheduleItemUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(
        item: ItemCronograma,
        newTitle: String,
        newDescription: String?,
        newDate: LocalDate,
        newStartTime: LocalTime,
        newEndTime: LocalTime,
        newCategory: Category,
        syncWithGoogle: Boolean
    ): Result<Unit> {
        if (newEndTime.isBefore(newStartTime) || newEndTime == newStartTime) {
            return Result.Error(AppError.InvalidTimeError)
        }

        val updatedItem = item.copy(
            titulo = newTitle,
            descricao = newDescription,
            data = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            horarioInicio = newStartTime,
            horarioTermino = newEndTime,
            categoryId = newCategory.id
        )

        try {
            if (syncWithGoogle) {
                val result = categoryRepository.updateCompleteEvent(updatedItem, newCategory.cor)
                if (result is Result.Error) {
                    return result
                }
            } else {
                scheduleRepository.insertItem(updatedItem)
            }

            // Reagenda o alarme para a tarefa atualizada
            alarmScheduler.schedule(updatedItem)

            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Error(AppError.UnknownError(e))
        }
    }
}
