package br.com.fabriciolima.momentus.widget

import android.content.Context
import android.content.Intent

object WidgetUpdater {

    // Ação que o Receiver irá escutar
    const val UPDATE_ACTION = "br.com.fabriciolima.momentus.action.UPDATE_WIDGET_DATA"

    /**
     * Envia um broadcast para todos os widgets do app, solicitando uma atualização de dados.
     */
    fun sendBroadcast(context: Context) {
        val intent = Intent(context, MomentusGlanceWidgetReceiver::class.java).apply {
            action = UPDATE_ACTION
        }
        context.sendBroadcast(intent)
    }
}
