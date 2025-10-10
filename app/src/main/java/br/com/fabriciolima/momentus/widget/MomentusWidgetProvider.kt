package br.com.fabriciolima.momentus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.screens.MainActivity

class MomentusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val UPDATE_WIDGET_ACTION = "br.com.fabriciolima.momentus.action.UPDATE_WIDGET"

        // Função para converter um drawable em um bitmap
        fun drawableToBitmap(context: Context, drawable: Drawable): Bitmap {
            // CORREÇÃO: Usar um tamanho fixo para o bitmap, pois o drawable não tem tamanho intrínseco
            val sizeInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                10f, 
                context.resources.displayMetrics
            ).toInt()

            val bitmap = Bitmap.createBitmap(sizeInPixels, sizeInPixels, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == UPDATE_WIDGET_ACTION) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            // Notifica o data set da lista para ser alterado
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Cria a view remota para o layout do widget
        val views = RemoteViews(context.packageName, R.layout.momentus_widget)

        // Configura o adapter do serviço para a lista
        val intent = Intent(context, WidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, intent)

        // Configura a view para quando a lista estiver vazia
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

        // Configura o clique no título para abrir o app
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            Intent(context, MainActivity::class.java), 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // Atualiza o widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
