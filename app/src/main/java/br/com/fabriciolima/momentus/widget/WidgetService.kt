// ARQUIVO: widget/WidgetService.kt

package br.com.fabriciolima.momentus.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // A única função deste serviço é criar e retornar nosso provedor de dados.
        return WidgetDataProvider(this.applicationContext, intent)
    }
}