package br.com.fabriciolima.momentus.fakes

import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.model.StatsResult
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import java.time.LocalDate
import java.time.LocalTime

class FakeRepository : RotinaRepository(
    FakeRotinaDao(),
    FakeItemCronogramaDao(),
    FakeTemplateDao(),
    FakeMetaDao(),
    FakeHabitoConcluidoDao(),
    FakeGoogleCalendarSource(),
    StandardTestDispatcher()
) {

    private val rotinasFlow = MutableStateFlow<List<RotinaComMeta>>(emptyList())

    override val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinasFlow

    override suspend fun insertRotina(rotina: Rotina) {
        val listaAtual = rotinasFlow.value.toMutableList()
        val index = listaAtual.indexOfFirst { it.rotina.id == rotina.id }
        val rotinaComMeta = RotinaComMeta(rotina = rotina, meta = null)

        if (index == -1) {
            listaAtual.add(rotinaComMeta)
        } else {
            listaAtual[index] = rotinaComMeta
        }
        rotinasFlow.value = listaAtual
    }

    override suspend fun deleteRotina(rotina: Rotina) {
        val listaAtual = rotinasFlow.value.toMutableList()
        listaAtual.removeAll { it.rotina.id == rotina.id }
        rotinasFlow.value = listaAtual
    }
}

class FakeGoogleCalendarSource : GoogleCalendarSource {
    override suspend fun saveEvent(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        cor: String?
    ): Result<String?> {
        return Result.Success("fake-event-id")
    }

    override suspend fun updateEvent(item: ItemCronograma): Result<String?> {
        return Result.Success(item.googleCalendarEventId)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun fetchEvents(): Result<List<GoogleCalendarEvent>> {
        return Result.Success(emptyList())
    }
}

class FakeRotinaDao : RotinaDao {
    override fun getRotinasComMetas(): Flow<List<RotinaComMeta>> = emptyFlow()
    override fun getAllSync(): List<Rotina> = emptyList()
    override suspend fun insert(rotina: Rotina) {}
    override suspend fun insertAll(rotinas: List<Rotina>) {}
    override suspend fun delete(rotina: Rotina) {}
    override fun getStats(): Flow<List<StatsResult>> = emptyFlow()
    override fun getAll(): Flow<List<Rotina>> = emptyFlow()
}

class FakeItemCronogramaDao : ItemCronogramaDao {
    override fun getAllItems(): Flow<List<ItemCronograma>> = emptyFlow()
    override fun getForWidget(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<ItemCronograma> = emptyList()
    override fun getItemsByDayOfWeek(dia: String): Flow<List<ItemCronograma>> = emptyFlow()
    override suspend fun insert(item: ItemCronograma) {}
    override suspend fun insertAll(items: List<ItemCronograma>) {}
    override suspend fun updateAll(items: List<ItemCronograma>) {}
    override suspend fun delete(item: ItemCronograma) {}
    override suspend fun getItemById(itemId: String): ItemCronograma? = null
    override fun getWidgetEventItems(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<WidgetEventItem> = emptyList()
}

class FakeTemplateDao : TemplateDao {
    override fun getTemplatesComEventos(): Flow<List<TemplateComEventos>> = emptyFlow()
    override fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos> = emptyFlow()
    override suspend fun insert(template: Template) {}
    override suspend fun insertAll(templates: List<Template>) {}
    override suspend fun delete(template: Template) {}
    override fun getAllSync(): List<Template> = emptyList()
}

class FakeMetaDao : MetaDao {
    override fun getMetaParaRotina(rotinaId: String): Flow<Meta?> = emptyFlow()
    override suspend fun insertOrUpdate(meta: Meta) {}
}

class FakeHabitoConcluidoDao : HabitoConcluidoDao {
    override fun getIdsConcluidos(): Flow<List<String>> = emptyFlow()
    override suspend fun insert(habito: HabitoConcluido) {}
    override suspend fun delete(itemCronogramaId: String) {}
}
