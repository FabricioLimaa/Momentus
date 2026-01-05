package br.com.fabriciolima.momentus.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.screens.ReminderActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ALARM_TYPE = "EXTRA_ALARM_TYPE"
        
        // Novos IDs de canais para sons diferentes
        private const val CHANNEL_INICIO_ID = "CHANNEL_INICIO"
        private const val CHANNEL_TERMINO_ID = "CHANNEL_TERMINO"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val eventTitle = intent.getStringExtra(EXTRA_MESSAGE) ?: "Rotina"
        val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: AlarmScheduler.TYPE_START

        if (eventId == null) return

        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationActionEntryPoint::class.java
        )
        val categoryRepository = hiltEntryPoint.categoryRepository()

        MainScope().launch {
            val completedHabits = categoryRepository.idsHabitosConcluidos.first()
            
            if (completedHabits.contains(eventId)) return@launch

            val title = if (alarmType == AlarmScheduler.TYPE_START) "Início: $eventTitle" else "Término: $eventTitle"
            val message = if (alarmType == AlarmScheduler.TYPE_START) 
                "Sua rotina começou! 🚀" else "Hora de finalizar sua rotina. Como foi seu progresso? ✅"

            showNotification(context, eventId, title, message, alarmType)
        }
    }

    private fun showNotification(context: Context, eventId: String, title: String, message: String, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = eventId.hashCode()

        // Seleciona o canal baseado no tipo para tocar o som correto
        val channelId = if (type == AlarmScheduler.TYPE_START) CHANNEL_INICIO_ID else CHANNEL_TERMINO_ID

        val contentIntent = Intent(context, ReminderActivity::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_MESSAGE, title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_COMPLETED
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            "${eventId}_complete".hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check_circle_outline, "Concluir", completePendingIntent)

        if (type == AlarmScheduler.TYPE_START) {
            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE
                putExtra(EXTRA_EVENT_ID, eventId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                "${eventId}_snooze".hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_time, "Adiar 15 min", snoozePendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
