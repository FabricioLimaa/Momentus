package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.database.AppDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetDataProvider(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var itensDoDia = listOf<ItemCronograma>()
    private var rotinasMap = mapOf<String, Rotina>()

    override fun onCreate() {
    }

    override fun onDataSetChanged() {
        // LÓGICA CORRIGIDA E FINAL
        // Acesso direto e síncrono ao banco de dados, sem dados falsos.
        val db = AppDatabase.getDatabase(context)
        val repository = RotinaRepository(
            rotinaDao = db.rotinaDao(),
            itemCronogramaDao = db.itemCronogramaDao(),
            templateDao = db.templateDao(),
            metaDao = db.metaDao(),
            habitoConcluidoDao = db.habitoConcluidoDao()
        )

        val hoje = LocalDate.now()
        
        // 1. Busca os itens do dia usando a consulta síncrona.
        itensDoDia = repository.getItensParaWidget(hoje).sortedBy { it.horarioInicio }

        // 2. Busca as rotinas usando a consulta síncrona.
        rotinasMap = repository.getTodasAsRotinasSync().associateBy { it.id }
    }

    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return itensDoDia.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= itensDoDia.size) return RemoteViews(context.packageName, R.layout.widget_list_item)

        val item = itensDoDia[position]
        val rotina = rotinasMap[item.rotinaId]

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        remoteViews.setTextViewText(R.id.widget_item_title, item.titulo)
        remoteViews.setTextViewText(R.id.widget_item_time, "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}")
        remoteViews.setTextViewText(R.id.widget_item_category, rotina?.nome ?: "Sem categoria")

        val cor = rotina?.cor?.let { try { Color.parseColor(it) } catch (e: IllegalArgumentException) { Color.GRAY } } ?: Color.GRAY
        remoteViews.setInt(R.id.widget_item_color_dot, "setColorFilter", cor)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return itensDoDia[position].id.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean = true
}
