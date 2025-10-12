package br.com.fabriciolima.momentus.data.source

import android.content.Context
import android.util.Log
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private fun getService(account: GoogleSignInAccount): Calendar {
        val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
            .setSelectedAccount(account.account)

        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        return Calendar.Builder(transport, jsonFactory, credentials)
            .setApplicationName("Momentus")
            .build()
    }

    suspend fun saveEvent(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime
    ): Result<String?> = withContext(dispatcher) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.Error(Exception("Nenhuma conta Google conectada."))

            val service = getService(account)
            val event = Event().apply {
                summary = titulo
                description = descricao
                val zoneId = ZoneId.systemDefault()
                val startInstant = data.atTime(horarioInicio).atZone(zoneId).toInstant()
                start = EventDateTime().setDateTime(DateTime(startInstant.toEpochMilli())).setTimeZone(zoneId.id)
                val endInstant = data.atTime(horarioTermino).atZone(zoneId).toInstant()
                end = EventDateTime().setDateTime(DateTime(endInstant.toEpochMilli())).setTimeZone(zoneId.id)
            }

            val createdEvent = service.events().insert("primary", event).execute()
            Result.Success(createdEvent.id)
        } catch (e: Exception) {
            Log.e("GoogleCalendarSource", "Falha ao salvar evento", e)
            Result.Error(Exception("Falha ao salvar evento no Google Calendar.", e))
        }
    }

    suspend fun updateEvent(item: ItemCronograma): Result<String?> = withContext(dispatcher) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.Error(Exception("Nenhuma conta Google conectada."))

            val service = getService(account)
            val event = Event().apply {
                summary = item.titulo
                description = item.descricao
                val zoneId = ZoneId.systemDefault()
                val eventDate = item.data?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() } ?: LocalDate.now()
                val startInstant = eventDate.atTime(item.horarioInicio).atZone(zoneId).toInstant()
                start = EventDateTime().setDateTime(DateTime(startInstant.toEpochMilli())).setTimeZone(zoneId.id)
                val endInstant = eventDate.atTime(item.horarioTermino).atZone(zoneId).toInstant()
                end = EventDateTime().setDateTime(DateTime(endInstant.toEpochMilli())).setTimeZone(zoneId.id)
            }
            
            val googleEventId = item.googleCalendarEventId
            val updatedEventId = if (googleEventId != null) {
                service.events().update("primary", googleEventId, event).execute().id
            } else {
                service.events().insert("primary", event).execute().id
            }
            Result.Success(updatedEventId)
        } catch (e: Exception) {
            Log.e("GoogleCalendarSource", "Falha ao atualizar evento", e)
            Result.Error(Exception("Falha ao atualizar evento no Google Calendar.", e))
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(dispatcher) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.Error(Exception("Nenhuma conta Google conectada."))

            val service = getService(account)
            service.events().delete("primary", eventId).execute()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("GoogleCalendarSource", "Falha ao excluir evento", e)
            Result.Error(Exception("Falha ao excluir evento no Google Calendar.", e))
        }
    }

    suspend fun fetchEvents(): Result<List<GoogleCalendarEvent>> = withContext(dispatcher) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.Success(emptyList()) // Não é um erro, apenas não há conta

            val service = getService(account)
            val now = DateTime(System.currentTimeMillis())
            val events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            val items = events.items?.mapNotNull { event ->
                (event.start?.dateTime ?: event.start?.date)?.let {
                    GoogleCalendarEvent(event.summary, it)
                }
            } ?: emptyList()
            
            Result.Success(items)
        } catch (e: Exception) {
            Log.e("GoogleCalendarSource", "Falha ao buscar eventos", e)
            Result.Error(Exception("Falha ao buscar eventos do Google Calendar.", e))
        }
    }
}