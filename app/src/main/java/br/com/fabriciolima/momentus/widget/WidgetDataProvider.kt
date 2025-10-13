package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.UserManager
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.DrawableCompat
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSourceImpl
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class WidgetDataProvider(private val context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    private var itensDoDia: List<ItemCronograma> = emptyList()
    private var rotinasMap: Map<String, Rotina> = emptyMap()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate() {
        // Não é necessário fazer nada aqui
    }

    override fun onDataSetChanged() {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        if (!userManager.isUserUnlocked) {
            itensDoDia = emptyList()
            return
        }

        val future = Executors.newSingleThreadExecutor().submit<Unit> {
            val db = AppDatabase.getDatabase(context)
            val googleCalendarSource = GoogleCalendarSourceImpl(context, Dispatchers.IO)
            val repository = RotinaRepository(
                rotinaDao = db.rotinaDao(),
                itemCronogramaDao = db.itemCronogramaDao(),
                templateDao = db.templateDao(),
                metaDao = db.metaDao(),
                habitoConcluidoDao = db.habitoConcluidoDao(),
                googleCalendarSource = googleCalendarSource,
                dispatcher = Dispatchers.IO
            )
            val hoje = LocalDate.now()
            itensDoDia = repository.getItensParaWidget(hoje).sortedBy { it.horarioInicio }
            rotinasMap = repository.getTodasAsRotinasSync().associateBy { it.id }
        }
        try {
            future.get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        itensDoDia = emptyList()
    }

    override fun getCount(): Int {
        return itensDoDia.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = itensDoDia[position]
        val rotina = rotinasMap[item.rotinaId]
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)

        remoteViews.setTextViewText(R.id.widget_item_title, item.titulo)
        remoteViews.setTextViewText(R.id.widget_item_category, rotina?.nome ?: "Sem categoria")
        remoteViews.setTextViewText(R.id.widget_item_time, "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}")

        val color = try {
            Color.parseColor(rotina?.cor)
        } catch (e: Exception) {
            android.graphics.Color.GRAY
        }
        val drawable = context.getDrawable(R.drawable.widget_item_dot)?.mutate()
        drawable?.let {
            DrawableCompat.setTint(it, color)
            remoteViews.setImageViewBitmap(R.id.widget_item_dot, MomentusWidgetProvider.drawableToBitmap(context, it))
        }

        val fillInIntent = Intent()
        remoteViews.setOnClickFillInIntent(R.id.widget_item_title, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return itensDoDia[position].id.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
