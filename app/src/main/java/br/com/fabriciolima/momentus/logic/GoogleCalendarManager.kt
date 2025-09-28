package br.com.fabriciolima.momentus.logic

import android.content.Context
import br.com.fabriciolima.momentus.data.RotinaRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar as JavaCalendar

object GoogleCalendarManager {

    suspend fun generateEvents(
        context: Context,
        account: GoogleSignInAccount,
        repository: RotinaRepository,
        startDate: JavaCalendar,
        endDate: JavaCalendar
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Este credential agora funcionará pois a dependência correta está presente
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR)
            ).setSelectedAccount(account.account)

            val calendarService = Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Momentus").build()

            val existingEventsList = calendarService.events().list("primary")
                .setTimeMin(com.google.api.client.util.DateTime(startDate.time))
                .setTimeMax(com.google.api.client.util.DateTime(endDate.time))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .items

            val existingEventsFingerprints = existingEventsList.mapNotNull {
                it.summary?.let { summary ->
                    it.start?.dateTime?.let { dateTime ->
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
                    val (hora, minuto) = item.horarioInicio.split(":").map { it.toInt() }

                    val inicioEvento = dataCorrente.clone() as JavaCalendar
                    inicioEvento.set(JavaCalendar.HOUR_OF_DAY, hora)
                    inicioEvento.set(JavaCalendar.MINUTE, minuto)
                    inicioEvento.set(JavaCalendar.SECOND, 0)
                    inicioEvento.set(JavaCalendar.MILLISECOND, 0)

                    val fimEvento = inicioEvento.clone() as JavaCalendar
                    fimEvento.add(JavaCalendar.MINUTE, rotina.duracaoPadraoMinutos)

                    val inicioDateTime = com.google.api.client.util.DateTime(inicioEvento.time)
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
                            dateTime = com.google.api.client.util.DateTime(fimEvento.time)
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