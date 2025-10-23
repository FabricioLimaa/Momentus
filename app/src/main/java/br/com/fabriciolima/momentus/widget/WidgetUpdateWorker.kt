package br.com.fabriciolima.momentus.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MomentusGlanceWidget::class.java)
        
        return try {
            glanceIds.forEach { glanceId ->
                WidgetUpdater.update(context, glanceId)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "br.com.fabriciolima.momentus.widget.WidgetUpdateWorker"
    }
}
