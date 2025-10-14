package br.com.fabriciolima.momentus.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Data class for the widget's UI state
data class EventInfo(val title: String, val time: String)

// Keys for storing data in the widget's state
object EventWidgetStateKeys {
    val titlesKey = stringPreferencesKey("event_titles")
    val timesKey = stringPreferencesKey("event_times")
}

// Hilt EntryPoint to allow injection in non-Android components
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetUpdateEntryPoint {
    fun rotinaRepository(): RotinaRepository
}

class MomentusGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val titles = (prefs[EventWidgetStateKeys.titlesKey] ?: "").split(';').filter { it.isNotEmpty() }
            val times = (prefs[EventWidgetStateKeys.timesKey] ?: "").split(';').filter { it.isNotEmpty() }
            val events = titles.zip(times).map { EventInfo(it.first, it.second) }

            MomentusGlanceWidgetContent(events)
        }
    }

    @Composable
    private fun MomentusGlanceWidgetContent(events: List<EventInfo>) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Momentus - Hoje",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "🔄",
                        modifier = GlanceModifier.clickable(actionRunCallback<UpdateAction>()).padding(8.dp)
                    )
                }

                if (events.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum evento para hoje.", style = TextStyle(color = GlanceTheme.colors.onSurface))
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(events) { event ->
                            EventListItem(event)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventListItem(event: EventInfo) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(event.time, style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant))
            Spacer(modifier = GlanceModifier.padding(horizontal = 4.dp))
            Text(event.title, style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface))
        }
    }
}

class UpdateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetUpdateEntryPoint::class.java)
        val repository = entryPoint.rotinaRepository()

        val todayEvents = repository.getItensParaWidget(LocalDate.now())
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val titles = todayEvents.joinToString(";") { it.titulo }
        val times = todayEvents.joinToString(";") { it.horarioInicio.format(timeFormatter) }

        // Update the state using the correct API
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[EventWidgetStateKeys.titlesKey] = titles
            prefs[EventWidgetStateKeys.timesKey] = times
        }

        // After updating the state, request a UI update for the widget
        MomentusGlanceWidget().update(context, glanceId)
    }
}
