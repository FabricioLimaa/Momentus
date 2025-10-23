package br.com.fabriciolima.momentus.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "WidgetUpdater"

object WidgetUpdater {

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
     * Envia uma transmissão para o sistema, solicitando a atualização de todos os widgets.
     * Usado pelos ViewModels.
     */
    fun sendBroadcast(context: Context) {
        Log.d(TAG, "Enviando broadcast para atualizar todos os widgets.")
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MomentusGlanceWidgetReceiver::class.java)
        val appWidgetIds = manager.getAppWidgetIds(componentName)

        val updateIntent = Intent(context, MomentusGlanceWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.sendBroadcast(updateIntent)
    }

    private fun mapToSerializable(items: List<WidgetEventItem>): List<WidgetEvent> {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return items.map { item ->
            WidgetEvent(
                id = item.id,
                title = item.titulo,
                timeRange = "${item.horarioInicio.format(timeFormatter)} - ${item.horarioTermino.format(timeFormatter)}",
                categoryName = item.nomeRotina,
                categoryColor = item.corRotina
            )
        }
    }
}
