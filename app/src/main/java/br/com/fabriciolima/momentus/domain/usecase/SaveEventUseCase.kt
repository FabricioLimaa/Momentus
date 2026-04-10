package br.com.fabriciolima.momentus.domain.usecase

import android.app.Application
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

class SaveEventUseCase @Inject constructor(
    private val eventoRepository: EventoRepository,
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val application: Application
) {

    suspend operator fun invoke(
        title: String,
        description: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        category: Category,
        saveToGoogle: Boolean
    ): Result<Unit> {
        if (endTime.isBefore(startTime) || endTime == startTime) {
            return Result.Error(AppError.InvalidTimeError)
        }

        var googleEventId: String? = null
        if (saveToGoogle) {
            when (val result = categoryRepository.saveEventToGoogle(title, description, date, startTime, endTime, category.cor)) {
                is Result.Success -> googleEventId = result.data
                is Result.Error -> return result // Propagate the error
            }
        }

        val newItem = ItemCronograma(
            titulo = title,
            descricao = description,
            data = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            diaDaSemana = null,
            horarioInicio = startTime,
            horarioTermino = endTime,
            categoryId = category.id,
            templateId = null,
            googleCalendarEventId = googleEventId
        )

        eventoRepository.insertItemCronograma(newItem)
        alarmScheduler.schedule(newItem)
        WidgetUpdater.requestUpdate(application)

        return Result.Success(Unit)
    }
}
