// ARQUIVO: logic/GoogleCalendarManager.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.logic

import android.content.Context
import br.com.fabriciolima.momentus.data.RotinaRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

object GoogleCalendarManager {

    suspend fun generateEvents(
        context: Context,
        account: GoogleSignInAccount,
        repository: RotinaRepository,
        startDate: Calendar,
        endDate: Calendar
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR)
            ).setSelectedAccount(account.account)

            val calendarService = com.google.api.services.calendar.Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Momentus").build()

            // --- MODIFICAÇÃO INICIA AQUI (BUSCAR EVENTOS EXISTENTES) ---

            // 1. Buscamos todos os eventos que já existem no calendário do usuário
            // dentro do período de datas selecionado. Isso é muito mais eficiente
            // do que buscar todos os eventos e filtrar depois.
            val existingEventsList = calendarService.events().list("primary")
                .setTimeMin(com.google.api.client.util.DateTime(startDate.time))
                .setTimeMax(com.google.api.client.util.DateTime(endDate.time))
                .setOrderBy("startTime")
                .setSingleEvents(true) // Expande eventos recorrentes em instâncias únicas
                .execute()
                .items

            // 2. Criamos um 'Set' (conjunto) para uma busca super rápida.
            // Para cada evento existente, criamos uma "impressão digital" única
            // combinando o nome e a data/hora de início.
            val existingEventsFingerprints = existingEventsList.mapNotNull {
                // Usamos 'let' para garantir que summary e start não sejam nulos
                it.summary?.let { summary ->
                    it.start?.dateTime?.let { dateTime ->
                        "$summary#${dateTime.value}"
                    }
                }
            }.toSet()

            // --- FIM DA MODIFICAÇÃO ---

            // --- MODIFICAÇÃO INICIA AQUI ---
            // 1. Usamos o nome correto da propriedade: 'todasAsRotinasComMetas'.
            // 2. Como ela retorna uma lista de 'RotinaComMeta', usamos '.map { it.rotina }'
            //    para extrair apenas a lista de 'Rotina' de que precisamos aqui.
            val rotinas = repository.todasAsRotinasComMetas.first().map { it.rotina }
            // --- MODIFICAÇÃO TERMINA AQUI ---
            val rotinasMap = rotinas.associateBy { it.id }
            val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

            val dataCorrente = startDate.clone() as Calendar
            var eventosCriados = 0

            val finalDate = endDate.clone() as Calendar
            finalDate.set(Calendar.HOUR_OF_DAY, 23)
            finalDate.set(Calendar.MINUTE, 59)


            while (dataCorrente.before(finalDate)) {
                val diaIndex = dataCorrente.get(Calendar.DAY_OF_WEEK) - 1
                val diaStr = diasDaSemana[diaIndex]

                val itensDoDia = repository.getItensDoDia(diaStr).first()

                for (item in itensDoDia) {
                    val rotina = rotinasMap[item.rotinaId] ?: continue

                    val (hora, minuto) = item.horarioInicio.split(":").map { it.toInt() }

                    val inicioEvento = dataCorrente.clone() as Calendar
                    inicioEvento.set(Calendar.HOUR_OF_DAY, hora)
                    inicioEvento.set(Calendar.MINUTE, minuto)
                    inicioEvento.set(Calendar.SECOND, 0)
                    inicioEvento.set(Calendar.MILLISECOND, 0)

                    val fimEvento = inicioEvento.clone() as Calendar
                    fimEvento.add(Calendar.MINUTE, rotina.duracaoPadraoMinutos)

                    val inicioDateTime = com.google.api.client.util.DateTime(inicioEvento.time)

                    // --- MODIFICAÇÃO INICIA AQUI (VERIFICAR DUPLICIDADE) ---

                    // 3. Criamos a mesma "impressão digital" para o evento que pretendemos criar.
                    val newEventFingerprint = "${rotina.nome}#${inicioDateTime.value}"

                    // 4. Se a impressão digital já existir no nosso conjunto de eventos,
                    // nós pulamos para o próximo item e NÃO criamos o evento duplicado.
                    if (existingEventsFingerprints.contains(newEventFingerprint)) {
                        continue
                    }

                    // --- FIM DA MODIFICAÇÃO ---

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

                dataCorrente.add(Calendar.DAY_OF_YEAR, 1)
            }
            Result.success(eventosCriados)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}