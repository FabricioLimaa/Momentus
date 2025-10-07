package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter

class WidgetDataProvider(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var itensDoDia = listOf<ItemCronograma>()
    private var rotinasMap = mapOf<String, Rotina>()

    override fun onCreate() {
        // Nada a fazer aqui
    }

    override fun onDataSetChanged() {
        // Esta é a parte importante. É chamada pelo sistema para atualizar os dados.
        // Como estamos em um processo diferente, precisamos acessar o banco de dados de forma síncrona.
        runBlocking {
            // CORREÇÃO: Instanciando o repositório corretamente, passando os DAOs do banco de dados.
            val db = AppDatabase.getDatabase(context)
            val repository = RotinaRepository(
                rotinaDao = db.rotinaDao(),
                itemCronogramaDao = db.itemCronogramaDao(),
                templateDao = db.templateDao(),
                metaDao = db.metaDao(),
                habitoConcluidoDao = db.habitoConcluidoDao()
            )
            val diaDaSemana = java.time.LocalDate.now().dayOfWeek.name.substring(0, 3)
            
            itensDoDia = repository.getItensDoDia(diaDaSemana).first()
            rotinasMap = repository.todasAsRotinasComMetas.first().associate { it.rotina.id to it.rotina }
        }
    }

    override fun onDestroy() {
        // Nada a fazer aqui
    }

    override fun getCount(): Int {
        return itensDoDia.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = itensDoDia[position]
        val rotina = rotinasMap[item.rotinaId]

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        remoteViews.setTextViewText(R.id.widget_item_title, item.titulo)
        remoteViews.setTextViewText(R.id.widget_item_time, "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}")
        remoteViews.setTextViewText(R.id.widget_item_category, rotina?.nome ?: "Sem categoria")

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? {
        return null // Pode retornar uma view de loading se desejar
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        // O ID precisa ser um Long, então usamos o hashCode da nossa String ID
        return itensDoDia[position].id.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
