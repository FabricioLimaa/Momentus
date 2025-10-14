package br.com.fabriciolima.momentus.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MomentusGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    // The GlanceAppWidget that will be managed by this receiver.
    override val glanceAppWidget: GlanceAppWidget = MomentusGlanceWidget()
}
