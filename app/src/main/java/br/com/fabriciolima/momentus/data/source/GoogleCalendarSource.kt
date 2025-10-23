package br.com.fabriciolima.momentus.data.source

import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import java.time.LocalDate
import java.time.LocalTime

interface GoogleCalendarSource {

    suspend fun saveEvent(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        cor: String?
    ): Result<String?>

    suspend fun updateEvent(item: ItemCronograma, cor: String?): Result<String?>

    suspend fun deleteEvent(eventId: String): Result<Unit>

    suspend fun fetchEvents(): Result<List<GoogleCalendarEvent>>
}
