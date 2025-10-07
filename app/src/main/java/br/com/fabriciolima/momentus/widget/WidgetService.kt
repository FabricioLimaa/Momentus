package br.com.fabriciolima.momentus.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // A única função deste serviço é criar e retornar nosso provedor de dados.
        // CORREÇÃO: O construtor de WidgetDataProvider espera apenas o Context.
        return WidgetDataProvider(this.applicationContext)
    }
}
