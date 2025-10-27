package br.com.fabriciolima.momentus.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "br.com.fabriciolima.momentus.widget.WidgetUpdateWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MomentusGlanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                WidgetUpdater.update(context, glanceId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
