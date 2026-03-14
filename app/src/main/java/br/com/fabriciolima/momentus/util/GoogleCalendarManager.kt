package br.com.fabriciolima.momentus.util

import android.content.Context
import android.util.Log
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.EventoRepository
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
 * Facilita a injeção de dependência e os testes.
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
    private val eventoRepository: EventoRepository // MUDANÇA: Injetar o repositório correto
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

    // Sincroniza todos os eventos da semana (baseado no Room) para o Google Calendar
    override fun syncEventsToCalendar() {
        if (calendar == null) {
            Log.w("CalendarSync", "Não foi possível obter o serviço de calendário. O usuário está logado?")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val diasDaSemana = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")

            try {
                // 1. Pega todos os eventos do Google Calendar
                val calendarEvents = getCalendarEvents()
                if (calendarEvents is Result.Error) {
                    Log.e("CalendarSync", "Erro ao buscar eventos do Google Calendar", calendarEvents.exception)
                    return@launch
                }

                val eventsMap = (calendarEvents as Result.Success).data?.items?.associateBy { it.id }.orEmpty()

                // 2. Itera sobre os eventos do Room e os sincroniza
                diasDaSemana.forEach { dia ->
                    val roomEvents = eventoRepository.getItensDoDia(dia).first()

                    roomEvents.forEach { roomEvent ->
                        val eventId = roomEvent.googleCalendarEventId

                        if (eventId != null && eventsMap.containsKey(eventId)) {
                            // Evento existe, atualiza
                            updateEvent(roomEvent)
                        } else {
                            // Evento não existe, insere
                            insertEvent(roomEvent)
                        }
                    }
                }
                Log.i("CalendarSync", "Sincronização com Google Calendar concluída.")

            } catch (e: Exception) {
                Log.e("CalendarSync", "Falha na sincronização com o Google Calendar", e)
            }
        }
    }

    // Busca eventos do Google Calendar
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
            Result.Error(e)
        }
    }

    // Atualiza um evento existente
    override suspend fun updateEvent(item: ItemCronograma): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            if (calendar == null || item.googleCalendarEventId.isNullOrEmpty()) {
                return@withContext Result.Error(IllegalStateException("Serviço de calendário ou ID do evento nulos."))
            }

            val event = calendar!!.events().get("primary", item.googleCalendarEventId).execute()
            val updatedEvent = configureEvent(event, item)

            val result = calendar!!.events().update("primary", event.id, updatedEvent).execute()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Deleta um evento
    override suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            calendar?.events()?.delete("primary", eventId)?.execute()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Insere um nova rotina
    override suspend fun insertEvent(item: ItemCronograma): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            if (calendar == null) {
                return@withContext Result.Error(IllegalStateException("Serviço de calendário nulo."))
            }
            val newEvent = configureEvent(Event(), item)
            val createdEvent = calendar!!.events().insert("primary", newEvent).execute()
            Result.Success(createdEvent)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun configureEvent(event: Event, item: ItemCronograma): Event {
        val data = LocalDate.now()
        val startDateTime = data.atTime(item.horarioInicio).atZone(ZoneId.systemDefault()).toInstant()
        val endDateTime = data.atTime(item.horarioTermino).atZone(ZoneId.systemDefault()).toInstant()

        event.summary = item.titulo
        event.description = item.descricao
        // TODO: Implementar recorrência para eventos de template
        // event.recurrence = listOf("RRULE:FREQ=WEEKLY;BYDAY=${item.diaDaSemana}")
        event.start = com.google.api.services.calendar.model.EventDateTime().setDateTime(DateTime(startDateTime.toEpochMilli()))
        event.end = com.google.api.services.calendar.model.EventDateTime().setDateTime(DateTime(endDateTime.toEpochMilli()))

        return event
    }
}
