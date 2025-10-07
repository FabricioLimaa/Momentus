package br.com.fabriciolima.momentus.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.RotinaComMeta
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Representa um evento vindo da API do Google Calendar
data class GoogleCalendarEvent(
    val summary: String,
    val start: DateTime
)

data class CalendarUiState(
    val allScheduleItems: List<ItemCronograma> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap(),
    val completedHabitIds: Set<String> = emptySet(),
    val googleCalendarEvents: List<GoogleCalendarEvent> = emptyList() // Novo campo
)

class CalendarViewModel(private val repository: RotinaRepository, application: Application) : ViewModel() {

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _googleCalendarEvents = MutableStateFlow<List<GoogleCalendarEvent>>(emptyList())

    val uiState: LiveData<CalendarUiState> = combine(
        repository.todosOsItensDoCronograma,
        repository.todasAsRotinasComMetas,
        repository.idsHabitosConcluidos,
        _googleCalendarEvents // Usa o StateFlow diretamente
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
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        rotina: Rotina
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
                val credentials = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR)
                ).apply {
                    selectedAccount = account.account
                }

                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                val service = com.google.api.services.calendar.Calendar.Builder(
                    transport,
                    jsonFactory,
                    credentials
                )
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
                 _googleCalendarEvents.value = emptyList() // Limpa em caso de erro
            }
        }
    }
}
