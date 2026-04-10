package br.com.fabriciolima.momentus.util

import android.content.Context
import android.util.Log
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.domain.error.AppError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface para abstrair a implementação do GoogleCalendarManager.
 */
interface IGoogleCalendarManager {
    fun syncEventsToCalendar()
    suspend fun getCalendarEvents(): Result<Events?>
    suspend fun updateEvent(item: ItemCronograma): Result<Event?>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    suspend fun insertEvent(item: ItemCronograma): Result<Event?>
}

/**
 * Gerencia a interação com a API do Google Calendar.
 */
@Singleton
class GoogleCalendarManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository 
) : IGoogleCalendarManager {

    private val calendar: Calendar? by lazy {
        getCalendarService()
    }

    private fun getCalendarService(): Calendar? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(CalendarScopes.CALENDAR_EVENTS)
        ).setSelectedAccount(account.account)

        return Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Momentus")
            .build()
    }

    override fun syncEventsToCalendar() {
        if (calendar == null) {
            Log.w("CalendarSync", "Não foi possível obter o serviço de calendário.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val diasDaSemana = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")

            try {
                val calendarEventsResult = getCalendarEvents()
                if (calendarEventsResult is Result.Error) {
                    Log.e("CalendarSync", "Erro ao buscar eventos: ${calendarEventsResult.error}")
                    return@launch
                }

                val eventsMap = (calendarEventsResult as Result.Success).data?.items?.associateBy { it.id }.orEmpty()

                diasDaSemana.forEach { dia ->
                    val roomEvents = scheduleRepository.getItemsForDay(dia).first()

                    roomEvents.forEach { roomEvent ->
                        val eventId = roomEvent.googleCalendarEventId

                        if (eventId != null && eventsMap.containsKey(eventId)) {
                            updateEvent(roomEvent)
                        } else {
                            insertEvent(roomEvent)
                        }
                    }
                }
                Log.i("CalendarSync", "Sincronização com Google Calendar concluída.")

            } catch (e: Exception) {
                Log.e("CalendarSync", "Falha na sincronização", e)
            }
        }
    }

    override suspend fun getCalendarEvents(): Result<Events?> = withContext(Dispatchers.IO) {
        try {
            val now = DateTime(System.currentTimeMillis())
            val events = calendar?.events()?.list("primary")
                ?.setMaxResults(2500)
                ?.setTimeMin(now)
                ?.setOrderBy("startTime")
                ?.setSingleEvents(true)
                ?.execute()
            Result.Success(events)
        } catch (e: IOException) {
            Result.Error(AppError.SyncError)
        }
    }

    override suspend fun updateEvent(item: ItemCronograma): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            if (calendar == null || item.googleCalendarEventId.isNullOrEmpty()) {
                return@withContext Result.Error(AppError.UnknownError(IllegalStateException("Serviço ou ID nulos.")))
            }

            val event = calendar!!.events().get("primary", item.googleCalendarEventId).execute()
            val updatedEvent = configureEvent(event, item)

            val result = calendar!!.events().update("primary", event.id, updatedEvent).execute()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            calendar?.events()?.delete("primary", eventId)?.execute()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun insertEvent(item: ItemCronograma): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            if (calendar == null) {
                return@withContext Result.Error(AppError.AuthRequiredError)
            }
            val newEvent = configureEvent(Event(), item)
            val createdEvent = calendar!!.events().insert("primary", newEvent).execute()
            Result.Success(createdEvent)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }

    private fun configureEvent(event: Event, item: ItemCronograma): Event {
        val data = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startDateTime = data.atTime(item.horarioInicio).atZone(zoneId).toInstant()
        val endDateTime = data.atTime(item.horarioTermino).atZone(zoneId).toInstant()

        event.summary = item.titulo
        event.description = item.descricao
        event.start = com.google.api.services.calendar.model.EventDateTime().setDateTime(DateTime(startDateTime.toEpochMilli()))
        event.end = com.google.api.services.calendar.model.EventDateTime().setDateTime(DateTime(endDateTime.toEpochMilli()))

        return event
    }
}
