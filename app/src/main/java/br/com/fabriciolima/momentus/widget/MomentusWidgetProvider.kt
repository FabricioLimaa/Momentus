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
import android.os.UserManager
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.screens.MainActivity

class MomentusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val UPDATE_WIDGET_ACTION = "br.com.fabriciolima.momentus.action.UPDATE_WIDGET"

        fun drawableToBitmap(context: Context, drawable: Drawable): Bitmap {
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
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        for (appWidgetId in appWidgetIds) {
            if (userManager.isUserUnlocked) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } else {
                showLockedView(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == UPDATE_WIDGET_ACTION || action == Intent.ACTION_USER_UNLOCKED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            if (userManager.isUserUnlocked) {
                 for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
            } else {
                for (appWidgetId in appWidgetIds) {
                    showLockedView(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.momentus_widget)

        val intent = Intent(context, WidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, intent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
    }

    private fun showLockedView(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.momentus_widget_locked)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
