// ARQUIVO: widget/WidgetDataProvider.kt (VERSÃO DE TESTE)

package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.utils.getOrAwaitValue
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class WidgetDataProvider(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var itensDoDia = listOf<ItemCronograma>()
    private var rotinasMap = mapOf<String, String>()
    private var coresMap = mapOf<String, String>()

    override fun onCreate() {
        Log.d("WidgetDataProvider", "onCreate foi chamado.")
    }

    override fun onDataSetChanged() {
        Log.d("WidgetDataProvider", "onDataSetChanged: Iniciando busca de dados...")
        try {
            // --- MODIFICAÇÃO INICIA AQUI (PARA TESTE) ---

            // 1. Comentamos temporariamente toda a busca no banco de dados.
            /*
            runBlocking {
                val repository = (context.applicationContext as MomentusApplication).repository
                val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
                val hojeIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                val hojeStr = diasDaSemana[hojeIndex]
                Log.d("WidgetDataProvider", "Hoje é: $hojeStr")

                itensDoDia = repository.getItensDoDia(hojeStr).getOrAwaitValue()
                Log.d("WidgetDataProvider", "Itens encontrados para hoje: ${itensDoDia.size}")
                val todasAsRotinas = repository.todasAsRotinas.getOrAwaitValue()
                rotinasMap = todasAsRotinas.associate { it.id to it.nome }
                coresMap = todasAsRotinas.associate { it.id to it.cor }
                Log.d("WidgetDataProvider", "Busca de dados concluída com sucesso.")
            }
            */

            // --- CORREÇÃO AQUI ---
            // Adicionamos 'data = null' ao criar o ItemCronograma de teste.
            Log.d("WidgetDataProvider", "USANDO DADOS DE TESTE!")
            itensDoDia = listOf(
                ItemCronograma(id = "teste1", data = null, diaDaSemana = "QUA", horarioInicio = "10:00", rotinaId = "rotina_teste")
            )
            rotinasMap = mapOf("rotina_teste" to "Item de Teste")
            coresMap = mapOf("rotina_teste" to "#FF5722")
            Log.d("WidgetDataProvider", "Dados de teste carregados. Itens: ${itensDoDia.size}")

        } catch (e: Exception) {
            Log.e("WidgetDataProvider", "ERRO AO BUSCAR DADOS: ", e)
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        Log.d("WidgetDataProvider", "getCount: retornando ${itensDoDia.size} itens.")
        return itensDoDia.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = itensDoDia[position]
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_item)

        val nomeRotina = rotinasMap[item.rotinaId] ?: "Desconhecido"
        val corRotina = coresMap[item.rotinaId] ?: "#808080"

        remoteViews.setTextViewText(R.id.widgetItemHorario, item.horarioInicio)
        remoteViews.setTextViewText(R.id.widgetItemNome, nomeRotina)
        try {
            remoteViews.setInt(R.id.widgetItemCor, "setBackgroundColor", Color.parseColor(corRotina))
        } catch (e: Exception) {
            remoteViews.setInt(R.id.widgetItemCor, "setBackgroundColor", Color.GRAY)
        }

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}