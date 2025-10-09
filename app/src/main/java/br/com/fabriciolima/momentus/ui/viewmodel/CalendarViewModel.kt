package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.widget.MomentusWidgetProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class GoogleCalendarEvent(
    val summary: String,
    val start: DateTime
)

data class CalendarUiState(
    val allScheduleItems: List<ItemCronograma> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap(),
    val completedHabitIds: Set<String> = emptySet(),
    val googleCalendarEvents: List<GoogleCalendarEvent> = emptyList()
)

class CalendarViewModel(private val repository: RotinaRepository, application: Application) : ViewModel() {

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _googleCalendarEvents = MutableStateFlow<List<GoogleCalendarEvent>>(emptyList())

    val uiState: LiveData<CalendarUiState> = combine(
        repository.todosOsItensDoCronograma,
        repository.todasAsRotinasComMetas,
        repository.idsHabitosConcluidos,
        _googleCalendarEvents
    ) { allItems, rotinasComMetas, completedIds, googleEvents ->
        val rotinasMap = rotinasComMetas.associateBy({ it.rotina.id }, { it.rotina })
        val completedIdsSet = completedIds.toSet()
        CalendarUiState(allItems, rotinasMap, completedIdsSet, googleEvents)
    }.asLiveData()

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { rotinasComMetas ->
        rotinasComMetas.map(RotinaComMeta::rotina)
    }.asLiveData()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun salvarEventoUnico(
        context: Context,
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        rotina: Rotina,
        salvarNoGoogle: Boolean
    ) {
        viewModelScope.launch {
            val novoItem = ItemCronograma(
                titulo = titulo,
                descricao = descricao,
                data = data.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                diaDaSemana = null,
                horarioInicio = horarioInicio,
                horarioTermino = horarioTermino,
                rotinaId = rotina.id,
                templateId = null
            )
            repository.insertItemCronograma(novoItem)

            // Notifica o widget que os dados mudaram!
            MomentusWidgetProvider.sendDataUpdatedBroadcast(context)

            if (salvarNoGoogle) {
                launch(Dispatchers.IO) { 
                    try {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account == null) {
                            Log.w("CalendarViewModel", "Nenhuma conta para criar evento no Google.")
                            return@launch
                        }

                        val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
                            .setSelectedAccount(account.account)

                        val transport = NetHttpTransport()
                        val jsonFactory = GsonFactory.getDefaultInstance()
                        val service = com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credentials)
                            .setApplicationName("Momentus")
                            .build()

                        val event = Event().apply {
                            summary = titulo
                            description = descricao

                            val zoneId = ZoneId.systemDefault()

                            val startInstant = data.atTime(horarioInicio).atZone(zoneId).toInstant()
                            val startDateTime = DateTime(startInstant.toEpochMilli())
                            start = EventDateTime().setDateTime(startDateTime).setTimeZone(zoneId.id)

                            val endInstant = data.atTime(horarioTermino).atZone(zoneId).toInstant()
                            val endDateTime = DateTime(endInstant.toEpochMilli())
                            end = EventDateTime().setDateTime(endDateTime).setTimeZone(zoneId.id)
                        }

                        service.events().insert("primary", event).execute()
                        Log.d("CalendarViewModel", "Evento criado no Google Calendar com sucesso.")

                        fetchGoogleCalendarEvents(context)

                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "Erro ao criar evento no Google Calendar", e)
                    }
                }
            }
        }
    }

    fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.marcarHabitoComoConcluido(itemCronogramaId)
        }
    }

    fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.desmarcarHabitoComoConcluido(itemCronogramaId)
        }
    }

    fun fetchGoogleCalendarEvents(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.w("CalendarViewModel", "Nenhuma conta do Google conectada.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credentials = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR)).apply {
                    selectedAccount = account.account
                }

                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                val service = com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credentials)
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

                _googleCalendarEvents.value = items

            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao buscar eventos do Google Calendar", e)
                 _googleCalendarEvents.value = emptyList()
            }
        }
    }
}
