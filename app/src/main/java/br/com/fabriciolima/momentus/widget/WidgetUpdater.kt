package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val TAG = "WidgetUpdater"

object WidgetUpdater {

    private val coroutineScope = MainScope()

    /**
     *  Executa a lógica de atualização completa para uma instância específica do widget.
     *  Usado pelo Worker e pelo botão de refresh.
     */
    suspend fun update(context: Context, glanceId: GlanceId) {
        Log.d(TAG, "Iniciando atualização do widget para glanceId: $glanceId")
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
            it.toMutablePreferences().apply {
                this[EventWidgetStateKeys.loadingKey] = true
                remove(EventWidgetStateKeys.errorKey)
            }.toPreferences()
        }
        MomentusGlanceWidget().update(context, glanceId)

        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetUpdateEntryPoint::class.java)
            val rotinaRepository = entryPoint.rotinaRepository()
            val eventoRepository = entryPoint.eventoRepository()

            val allRotinas = withContext(Dispatchers.IO) { rotinaRepository.getTodasAsRotinasSync() }
            val allRotinaIds = allRotinas.map { it.id }.toSet()
            Log.d(TAG, "Total de rotinas encontradas: ${allRotinaIds.size}")

            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            val allowedRotinaIds = prefs[EventWidgetStateKeys.configuredRotinasKey] ?: allRotinaIds
            Log.d(TAG, "Rotinas permitidas para este widget: ${allowedRotinaIds.size}")

            val widgetItems = withContext(Dispatchers.IO) {
                eventoRepository.getWidgetEvents(LocalDate.now(), allowedRotinaIds.ifEmpty { allRotinaIds })
            }
            Log.d(TAG, "Itens encontrados no repositório para o widget: ${widgetItems.size}")

            val events = mapToSerializable(widgetItems)
            val eventsJson = Json.encodeToString(events)
            Log.d(TAG, "JSON de eventos gerado: $eventsJson")

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[EventWidgetStateKeys.eventsKey] = eventsJson
                    this[EventWidgetStateKeys.loadingKey] = false
                    if (this[EventWidgetStateKeys.configuredRotinasKey] == null) {
                        this[EventWidgetStateKeys.configuredRotinasKey] = allRotinaIds
                    }
                }.toPreferences()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar widget", e)
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[EventWidgetStateKeys.errorKey] = "Falha ao carregar eventos."
                    this[EventWidgetStateKeys.loadingKey] = false
                }.toPreferences()
            }
            e.printStackTrace()
        } finally {
            Log.d(TAG, "Atualização final da UI do widget.")
            MomentusGlanceWidget().update(context, glanceId)
        }
    }

    /**
     * Dispara a atualização para todas as instâncias ativas do widget.
     * Usado pelos ViewModels para notificar o widget sobre mudanças nos dados.
     */
    fun requestUpdate(context: Context) {
        coroutineScope.launch {
            Log.d(TAG, "Solicitando atualização para todos os widgets.")
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MomentusGlanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                update(context, glanceId)
            }
        }
    }

    private fun mapToSerializable(items: List<WidgetEventItem>): List<WidgetEvent> {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return items.map { item ->
            val horarioInicio = LocalTime.parse(item.horarioInicio)
            val horarioTermino = LocalTime.parse(item.horarioTermino)
            WidgetEvent(
                id = item.id,
                title = item.titulo,
                timeRange = "${timeFormatter.format(horarioInicio)} - ${timeFormatter.format(horarioTermino)}",
                categoryName = item.nomeRotina,
                categoryColor = item.corRotina
            )
        }
    }
}
