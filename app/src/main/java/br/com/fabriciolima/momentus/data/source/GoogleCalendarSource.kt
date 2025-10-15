package br.com.fabriciolima.momentus.data.source

import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import java.time.LocalDate
import java.time.LocalTime

/**
 * Interface que define o contrato para a fonte de dados do Google Calendar.
 */
interface GoogleCalendarSource {

    suspend fun saveEvent(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        cor: String? // Parâmetro da cor adicionado
    ): Result<String?>

    suspend fun updateEvent(item: ItemCronograma): Result<String?>

    suspend fun deleteEvent(eventId: String): Result<Unit>

    suspend fun fetchEvents(): Result<List<GoogleCalendarEvent>>
}
