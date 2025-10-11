package br.com.fabriciolima.momentus.data.repository

import android.content.Context
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.model.StatsResult
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

open class RotinaRepository @Inject constructor(
    private val rotinaDao: RotinaDao,
    private val itemCronogramaDao: ItemCronogramaDao,
    private val templateDao: TemplateDao,
    private val metaDao: MetaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao.getRotinasComMetas()
    open val todosOsTemplatesComEventos: Flow<List<TemplateComEventos>> = templateDao.getTemplatesComEventos()
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = rotinaDao.getStats()

    fun getItensParaWidget(data: LocalDate): List<ItemCronograma> {
        val epochDay = data.toEpochDay()
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)
        return itemCronogramaDao.getForWidget(epochDay, dayOfWeekName)
    }

    fun getTodasAsRotinasSync(): List<Rotina> {
        return rotinaDao.getAllSync()
    }

    fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos> {
        return templateDao.getTemplateComEventos(templateId)
    }

    suspend fun insertTemplate(template: Template) {
        templateDao.insert(template)
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template)
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
    }

    open suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
    }

    open suspend fun insertRotina(rotina: Rotina) {
        rotinaDao.insert(rotina)
    }

    open suspend fun deleteRotina(rotina: Rotina) {
        rotinaDao.delete(rotina)
    }

    fun getMetaParaRotina(rotinaId: String): Flow<Meta?> {
        return metaDao.getMetaParaRotina(rotinaId)
    }

    suspend fun salvarMeta(meta: Meta) {
        metaDao.insertOrUpdate(meta)
    }

    suspend fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao.insert(habito)
    }

    suspend fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        habitoConcluidoDao.delete(itemCronogramaId)
    }

    suspend fun salvarEventoNoGoogle(
        context: Context,
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime
    ): Result<String?> = withContext(dispatcher) { // Modificado para usar o dispatcher injetado
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.Error(Exception("Nenhuma conta Google conectada."))

            val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
                .setSelectedAccount(account.account)

            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val service = Calendar.Builder(transport, jsonFactory, credentials)
                .setApplicationName("Momentus")
                .build()

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
            Result.Error(Exception("Falha ao salvar evento no Google Calendar.", e))
        }
    }
    
    suspend fun atualizarEventoCompleto(context: Context, item: ItemCronograma): Result<String?> = withContext(dispatcher) { // Modificado para usar o dispatcher injetado
        try {
            var googleEventId: String? = item.googleCalendarEventId
            val account = GoogleSignIn.getLastSignedInAccount(context)

            if (account != null && item.data != null) {
                val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
                    .setSelectedAccount(account.account)
                val service = Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credentials)
                    .setApplicationName("Momentus").build()

                val event = Event().apply {
                    summary = item.titulo
                    description = item.descricao
                    val zoneId = ZoneId.systemDefault()
                    
                    val eventDate = Instant.ofEpochMilli(item.data).atZone(zoneId).toLocalDate()
                    val startInstant = eventDate.atTime(item.horarioInicio).atZone(zoneId).toInstant()
                    start = EventDateTime().setDateTime(DateTime(startInstant.toEpochMilli())).setTimeZone(zoneId.id)

                    val endInstant = eventDate.atTime(item.horarioTermino).atZone(zoneId).toInstant()
                    end = EventDateTime().setDateTime(DateTime(endInstant.toEpochMilli())).setTimeZone(zoneId.id)
                }

                if (googleEventId != null) {
                    val updatedEvent = service.events().update("primary", googleEventId, event).execute()
                    googleEventId = updatedEvent.id
                } else {
                    val createdEvent = service.events().insert("primary", event).execute()
                    googleEventId = createdEvent.id
                }
            }

            itemCronogramaDao.insert(item.copy(googleCalendarEventId = googleEventId))
            Result.Success(googleEventId)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao atualizar evento.", e))
        }
    }

    suspend fun excluirEventoCompleto(context: Context, item: ItemCronograma): Result<Unit> = withContext(dispatcher) { // Modificado para usar o dispatcher injetado
        try {
            if (item.googleCalendarEventId != null) {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
                        .setSelectedAccount(account.account)
                    val service = Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credentials)
                        .setApplicationName("Momentus").build()

                    service.events().delete("primary", item.googleCalendarEventId).execute()
                }
            }
            
            itemCronogramaDao.delete(item)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir evento.", e))
        }
    }

    suspend fun fetchGoogleCalendarEvents(context: Context, account: GoogleSignInAccount): Result<List<GoogleCalendarEvent>> = withContext(dispatcher) { // Modificado para usar o dispatcher injetado
        try {
            val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR)).apply {
                selectedAccount = account.account
            }

            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val service = Calendar.Builder(transport, jsonFactory, credentials)
                .setApplicationName("Momentus")
                .build()

            val now = DateTime(System.currentTimeMillis())
            val events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            val items = events.items?.map { event ->
                GoogleCalendarEvent(event.summary, event.start.dateTime ?: event.start.date)
            } ?: emptyList()
            
            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao buscar eventos do Google Calendar.", e))
        }
    }
}
