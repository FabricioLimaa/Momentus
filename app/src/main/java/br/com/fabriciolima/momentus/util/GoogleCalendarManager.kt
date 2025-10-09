package br.com.fabriciolima.momentus.util

import android.content.Context
import android.util.Log
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar as JavaCalendar

object GoogleCalendarManager {

    private const val TAG = "GoogleCalendarManager"

    /**
     * Insere um único evento no calendário principal do usuário.
     */
    suspend fun insertEvent(
        context: Context,
        account: GoogleSignInAccount,
        title: String,
        description: String?,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR)
            ).setSelectedAccount(account.account)

            val calendarService = Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Momentus").build()

            val event = Event().apply {
                summary = title
                this.description = description
                start = EventDateTime().apply {
                    dateTime = DateTime(startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    timeZone = ZoneId.systemDefault().id
                }
                end = EventDateTime().apply {
                    dateTime = DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    timeZone = ZoneId.systemDefault().id
                }
            }

            calendarService.events().insert("primary", event).execute()
            Log.d(TAG, "Evento '$title' inserido com sucesso no Google Calendar.")
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Erro ao inserir evento no Google Calendar", e)
            Result.failure(e)
        }
    }

    /**
     * Gera eventos em lote com base nas rotinas do usuário para um período.
     */
    suspend fun generateEvents(
        context: Context,
        account: GoogleSignInAccount,
        repository: RotinaRepository,
        startDate: JavaCalendar,
        endDate: JavaCalendar
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR)
            ).setSelectedAccount(account.account)

            val calendarService = Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Momentus").build()

            val existingEventsList = calendarService.events().list("primary")
                .setTimeMin(DateTime(startDate.time))
                .setTimeMax(DateTime(endDate.time))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .items

            val existingEventsFingerprints = existingEventsList.mapNotNull { event ->
                event.summary?.let { summary ->
                    event.start?.dateTime?.let { dateTime ->
                        "$summary#${dateTime.value}"
                    }
                }
            }.toSet()

            val rotinas = repository.todasAsRotinasComMetas.first().map { it.rotina }
            val rotinasMap = rotinas.associateBy { it.id }
            val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

            val dataCorrente = startDate.clone() as JavaCalendar
            var eventosCriados = 0

            val finalDate = endDate.clone() as JavaCalendar
            finalDate.set(JavaCalendar.HOUR_OF_DAY, 23)
            finalDate.set(JavaCalendar.MINUTE, 59)

            while (dataCorrente.before(finalDate)) {
                val diaIndex = dataCorrente.get(JavaCalendar.DAY_OF_WEEK) - 1
                val diaStr = diasDaSemana[diaIndex]

                val itensDoDia = repository.getItensDoDia(diaStr).first()

                for (item in itensDoDia) {
                    val rotina = rotinasMap[item.rotinaId] ?: continue
                    
                    // CORREÇÃO: Acessando as propriedades de LocalTime diretamente
                    val hora = item.horarioInicio.hour
                    val minuto = item.horarioInicio.minute

                    val inicioEvento = dataCorrente.clone() as JavaCalendar
                    inicioEvento.set(JavaCalendar.HOUR_OF_DAY, hora)
                    inicioEvento.set(JavaCalendar.MINUTE, minuto)
                    inicioEvento.set(JavaCalendar.SECOND, 0)
                    inicioEvento.set(JavaCalendar.MILLISECOND, 0)

                    val fimEvento = inicioEvento.clone() as JavaCalendar
                    fimEvento.add(JavaCalendar.MINUTE, rotina.duracaoPadraoMinutos)

                    val inicioDateTime = DateTime(inicioEvento.time)
                    val newEventFingerprint = "${rotina.nome}#${inicioDateTime.value}"

                    if (existingEventsFingerprints.contains(newEventFingerprint)) {
                        continue
                    }

                    val event = Event().apply {
                        summary = rotina.nome
                        start = EventDateTime().apply {
                            dateTime = inicioDateTime
                            timeZone = "America/Sao_Paulo"
                        }
                        end = EventDateTime().apply {
                            dateTime = DateTime(fimEvento.time)
                            timeZone = "America/Sao_Paulo"
                        }
                    }
                    calendarService.events().insert("primary", event).execute()
                    eventosCriados++
                }
                dataCorrente.add(JavaCalendar.DAY_OF_YEAR, 1)
            }
            Result.success(eventosCriados)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
