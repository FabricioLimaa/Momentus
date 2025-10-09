package br.com.fabriciolima.momentus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import br.com.fabriciolima.momentus.R

class MomentusWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Lógica para lidar com diferentes ações
        when (intent.action) {
            ACTION_DATA_UPDATED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MomentusWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                // Notifica a mudança de dados para TODOS os widgets ativos
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widgetListView)
            }
            ACTION_REFRESH_BUTTON_CLICKED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val ACTION_REFRESH_BUTTON_CLICKED = "br.com.fabriciolima.momentus.widget.ACTION_REFRESH_BUTTON_CLICKED"
        const val ACTION_DATA_UPDATED = "br.com.fabriciolima.momentus.widget.ACTION_DATA_UPDATED"

        /**
         * Função pública para notificar o widget de que os dados do app mudaram.
         * Deve ser chamada sempre que um evento for criado, atualizado ou deletado.
         */
        fun sendDataUpdatedBroadcast(context: Context) {
            val intent = Intent(context, MomentusWidgetProvider::class.java).apply {
                action = ACTION_DATA_UPDATED
            }
            context.sendBroadcast(intent)
        }

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.momentus_widget)

            val serviceIntent = Intent(context, WidgetService::class.java)
            views.setRemoteAdapter(R.id.widgetListView, serviceIntent)
            views.setEmptyView(R.id.widgetListView, R.id.widgetEmptyView)

            // Configura o botão de refresh
            val refreshIntent = Intent(context, MomentusWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_BUTTON_CLICKED
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetButtonRefresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}