package br.com.fabriciolima.momentus.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.ui.screens.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
    val categoryColor: String?,
    val isPast: Boolean = false,
    val isCompleted: Boolean = false
)

object EventWidgetStateKeys {
    val loadingKey = booleanPreferencesKey("widget_loading")
    val eventsKey = stringPreferencesKey("events_json")
    val configuredRotinasKey = stringSetPreferencesKey("widget_configured_rotinas")
    val errorKey = stringPreferencesKey("widget_error")
}

const val EVENT_ID_KEY = "br.com.fabriciolima.momentus.EVENT_ID"
const val OPEN_NEW_EVENT_DIALOG_KEY = "br.com.fabriciolima.momentus.OPEN_NEW_EVENT"
private val eventIdParam = ActionParameters.Key<String>("eventId")
private val isCompletedParam = ActionParameters.Key<Boolean>("isCompleted")

// --- HILT ENTRY POINT ---

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetUpdateEntryPoint {
    fun categoryRepository(): CategoryRepository
    fun scheduleRepository(): ScheduleRepository
}


// --- WIDGET PRINCIPAL ---

class MomentusGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    private val backgroundColor = ColorProvider(day = Color(0xFFF0F0F0), night = Color(0xFF2C2C2E))
    private val itemBackgroundColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF3A3A3C))
    private val primaryTextColor = ColorProvider(day = Color.Black, night = Color.White)
    private val secondaryTextColor = ColorProvider(day = Color(0xFF3C3C43), night = Color(0xFFEBEBF5))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
                        androidx.glance.appwidget.CircularProgressIndicator()
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
                        items(items = events, itemId = { it.id.hashCode().toLong() }) { event ->
                            EventListItem(context, event)
                            Spacer(GlanceModifier.height(8.dp))
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
                contentDescription = "Adicionar nova rotina",
                colorFilter = ColorFilter.tint(primaryTextColor),
                modifier = GlanceModifier.size(36.dp).clickable(
                    actionStartActivity(
                        ComponentName(context, MainActivity::class.java),
                        parameters = actionParametersOf(ActionParameters.Key<Boolean>(OPEN_NEW_EVENT_DIALOG_KEY) to true)
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
        val textAlpha = if (event.isCompleted) 0.6f else 1f
        val textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None

        val itemTextColor = ColorProvider(day = Color.Black.copy(alpha = textAlpha), night = Color.White.copy(alpha = textAlpha))
        val itemSecondaryTextColor = ColorProvider(day = Color(0xFF3C3C43).copy(alpha = textAlpha), night = Color(0xFFEBEBF5).copy(alpha = textAlpha))

        val categoryColor = try {
            event.categoryColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Gray
        } catch (e: Exception) {
            Color.Gray
        }
        val categoryColorProvider = ColorProvider(
            day = categoryColor.copy(alpha = textAlpha),
            night = categoryColor.copy(alpha = textAlpha)
        )
        val categoryColorAlphaProvider = ColorProvider(
            day = categoryColor.copy(alpha = 0.2f * textAlpha),
            night = categoryColor.copy(alpha = 0.2f * textAlpha)
        )

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(itemBackgroundColor)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(
                    actionStartActivity(
                        ComponentName(context, MainActivity::class.java),
                        parameters = actionParametersOf(ActionParameters.Key<String>(EVENT_ID_KEY) to event.id)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(
                checked = event.isCompleted,
                onCheckedChange = actionRunCallback<ToggleHabitAction>(
                    parameters = actionParametersOf(
                        eventIdParam to event.id,
                        isCompletedParam to event.isCompleted
                    )
                )
            )
            Spacer(GlanceModifier.width(8.dp))
            @Suppress("DEPRECATION")
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = event.title.ifBlank { "(Sem título)" },
                    style = TextStyle(color = itemTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, textDecoration = textDecoration),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_time),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(itemSecondaryTextColor),
                        modifier = GlanceModifier.size(14.dp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = event.timeRange,
                        style = TextStyle(color = itemSecondaryTextColor, fontSize = 14.sp, textDecoration = textDecoration),
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
    }
}

class UpdateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetUpdater.update(context, glanceId)
    }
}

class ToggleHabitAction : ActionCallback {
    private val scope = MainScope()

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = parameters[eventIdParam] ?: return
        val isCompleted = parameters[isCompletedParam] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetUpdateEntryPoint::class.java)
        val categoryRepository = entryPoint.categoryRepository()

        scope.launch {
            if (isCompleted) {
                categoryRepository.unmarkHabitAsCompleted(eventId)
            } else {
                categoryRepository.markHabitAsCompleted(eventId)
            }
            WidgetUpdater.update(context, glanceId)
        }.join()
    }
}
