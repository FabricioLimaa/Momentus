package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

class UpdateRotinaUseCase @Inject constructor(
    private val rotinaRepository: RotinaRepository,
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
            return Result.Error(IllegalArgumentException("O horário de término deve ser depois do início."))
        }

        val updatedItem = item.copy(
            titulo = newTitle,
            descricao = newDescription,
            data = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            horarioInicio = newStartTime,
            horarioTermino = newEndTime,
            categoryId = newCategory.id
        )

        if (syncWithGoogle) {
            val result = categoryRepository.updateCompleteEvent(updatedItem, newCategory.cor)
            if (result is Result.Error) {
                return result
            }
        } else {
            rotinaRepository.insertItemCronograma(updatedItem)
        }

        // Reagenda o alarme para a rotina atualizada
        alarmScheduler.schedule(updatedItem)

        return Result.Success(Unit)
    }
}
