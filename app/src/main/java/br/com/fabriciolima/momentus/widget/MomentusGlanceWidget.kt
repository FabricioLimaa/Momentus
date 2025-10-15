package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.ui.screens.CalendarActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.glance.color.ColorProvider

// --- DATA E ESTADO DO WIDGET ---

@Serializable
data class WidgetEvent(
    val id: String,
    val title: String,
    val timeRange: String,
    val categoryName: String,
    val categoryColor: String?
)

object EventWidgetStateKeys {
    val loadingKey = booleanPreferencesKey("widget_loading")
    val eventsKey = stringPreferencesKey("events_json")
    val configuredRotinasKey = stringSetPreferencesKey("widget_configured_rotinas")
    val errorKey = stringPreferencesKey("widget_error")
}

const val EVENT_ID_KEY = "br.com.fabriciolima.momentus.EVENT_ID"
const val OPEN_NEW_EVENT_DIALOG_KEY = "br.com.fabriciolima.momentus.OPEN_NEW_EVENT"

// --- HILT ENTRY POINT ---

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetUpdateEntryPoint {
    fun rotinaRepository(): RotinaRepository
}


// --- WIDGET PRINCIPAL ---

class MomentusGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    private val backgroundColor = ColorProvider(day = Color(0xFFF0F0F0), night = Color(0xFF2C2C2E))
    private val itemBackgroundColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF3A3A3C))
    private val primaryTextColor = ColorProvider(day = Color.Black, night = Color.White)
    private val secondaryTextColor = ColorProvider(day = Color(0xFF3C3C43), night = Color(0xFFEBEBF5))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        UpdateAction.run(context, id)

        provideContent {
            val prefs = currentState<Preferences>()
            val isLoading = prefs[EventWidgetStateKeys.loadingKey] ?: true
            val error = prefs[EventWidgetStateKeys.errorKey]
            val eventsJson = prefs[EventWidgetStateKeys.eventsKey]

            val events = if (eventsJson != null) {
                try {
                    Json.decodeFromString<List<WidgetEvent>>(eventsJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            WidgetContent(context, isLoading, error, events)
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        isLoading: Boolean,
        error: String?,
        events: List<WidgetEvent>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(24.dp)
                .padding(16.dp)
        ) {
            WidgetHeader(context)
            Spacer(GlanceModifier.height(16.dp))

            when {
                isLoading -> {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error, style = TextStyle(color = primaryTextColor))
                    }
                }
                events.isEmpty() -> {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum evento para hoje.", style = TextStyle(color = primaryTextColor))
                    }
                }
                else -> {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(count = events.size, itemId = { events[it].id.hashCode().toLong() }) { index ->
                            val event = events[index]
                            EventListItem(context, event)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetHeader(context: Context) {
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM")
        val title = "Hoje, ${today.format(dayFormatter)}"

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryTextColor),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_add),
                contentDescription = "Adicionar novo evento",
                colorFilter = ColorFilter.tint(primaryTextColor),
                modifier = GlanceModifier.size(36.dp).clickable(
                    actionStartActivity(
                        Intent(context, CalendarActivity::class.java).apply {
                            putExtra(OPEN_NEW_EVENT_DIALOG_KEY, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                ).padding(8.dp)
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Atualizar",
                colorFilter = ColorFilter.tint(primaryTextColor),
                modifier = GlanceModifier.size(36.dp).clickable(
                    actionRunCallback<UpdateAction>()
                ).padding(8.dp)
            )
        }
    }

    @Composable
    private fun EventListItem(context: Context, event: WidgetEvent) {
        val categoryColor = try {
            event.categoryColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Gray
        } catch (e: Exception) {
            Color.Gray
        }
        val categoryColorProvider = ColorProvider(categoryColor, categoryColor)
        val categoryColorAlphaProvider = ColorProvider(categoryColor.copy(alpha = 0.25f), categoryColor.copy(alpha = 0.25f))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(itemBackgroundColor)
                .cornerRadius(16.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(
                    actionStartActivity(
                        Intent(context, CalendarActivity::class.java).apply {
                            putExtra(EVENT_ID_KEY, event.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .background(categoryColorProvider)
                    .cornerRadius(5.dp)
            ) {}

            Spacer(GlanceModifier.width(16.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = event.title.ifBlank { "(Sem título)" },
                    style = TextStyle(color = primaryTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_time),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(secondaryTextColor),
                        modifier = GlanceModifier.size(14.dp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = event.timeRange,
                        style = TextStyle(color = secondaryTextColor, fontSize = 14.sp),
                        maxLines = 1
                    )
                }
            }

            Spacer(GlanceModifier.width(8.dp))

            Box(
                modifier = GlanceModifier
                    .background(categoryColorAlphaProvider)
                    .cornerRadius(20.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = event.categoryName,
                    style = TextStyle(color = categoryColorProvider, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
    }
}

// --- LÓGICA DE ATUALIZAÇÃO (ACTION) ---

class UpdateAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        run(context, glanceId)
    }

    companion object {
        suspend fun run(context: Context, glanceId: GlanceId) {
            updateState(context, glanceId) { it[EventWidgetStateKeys.loadingKey] = true; it.remove(EventWidgetStateKeys.errorKey) }

            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetUpdateEntryPoint::class.java)
                val repository = entryPoint.rotinaRepository()

                val allRotinas = withContext(Dispatchers.IO) { repository.getTodasAsRotinasSync() }
                val allRotinaIds = allRotinas.map { it.id }.toSet()

                val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                val allowedRotinaIds = prefs[EventWidgetStateKeys.configuredRotinasKey] ?: allRotinaIds

                val widgetItems = withContext(Dispatchers.IO) {
                    repository.getWidgetEvents(LocalDate.now(), allowedRotinaIds.ifEmpty { allRotinaIds })
                }

                val events = mapToSerializable(widgetItems)
                val eventsJson = Json.encodeToString(events)

                updateState(context, glanceId) {
                    it[EventWidgetStateKeys.eventsKey] = eventsJson
                    it[EventWidgetStateKeys.loadingKey] = false
                    if (it[EventWidgetStateKeys.configuredRotinasKey] == null) {
                        it[EventWidgetStateKeys.configuredRotinasKey] = allRotinaIds
                    }
                }

            } catch (e: Exception) {
                updateState(context, glanceId) {
                    it[EventWidgetStateKeys.errorKey] = "Falha ao carregar eventos."
                    it[EventWidgetStateKeys.loadingKey] = false
                }
                e.printStackTrace()
            } finally {
                MomentusGlanceWidget().update(context, glanceId)
            }
        }

        private suspend fun updateState(
            context: Context,
            glanceId: GlanceId,
            block: (MutablePreferences) -> Unit
        ) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply(block).toPreferences()
            }
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
}
