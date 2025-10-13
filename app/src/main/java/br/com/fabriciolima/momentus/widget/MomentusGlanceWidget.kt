package br.com.fabriciolima.momentus.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class MomentusGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val events = withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val today = LocalDate.now()
            val epochDay = today.toEpochDay()
            val dayOfWeekName = today.dayOfWeek.name.uppercase()
            database.itemCronogramaDao().getForWidget(epochDay, dayOfWeekName)
        }

        provideContent {
            MomentusTheme {
                WidgetContent(events = events)
            }
        }
    }
}

@Composable
private fun WidgetContent(events: List<ItemCronograma>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(16.dp),
    ) {
        Text("Momentus", style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold))
        Text("Suas tarefas para hoje", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        Spacer(GlanceModifier.height(16.dp))

        if (events.isEmpty()) {
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                Text("Nenhuma tarefa para hoje.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                items(events, itemId = { it.id.hashCode().toLong() }) { event ->
                    EventWidgetItem(event)
                }
            }
        }
    }
}

@Composable
private fun EventWidgetItem(event: ItemCronograma) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.size(10.dp).background(GlanceTheme.colors.primary).cornerRadius(5.dp)) {}
        Spacer(GlanceModifier.padding(12.dp))
        Text(
            text = event.titulo,
            style = TextStyle(color = GlanceTheme.colors.onSurface),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )
    }
}

class MomentusGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentusGlanceWidget()
}
