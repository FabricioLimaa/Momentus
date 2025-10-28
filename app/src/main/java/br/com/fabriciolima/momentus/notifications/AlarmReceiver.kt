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

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarme recebido!")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Sua rotina está prestes a começar"

        Log.d("AlarmReceiver", "Dados recebidos: eventId='$eventId', message='$message'")

        if (eventId == null) {
            Log.e("AlarmReceiver", "Erro: eventId é nulo. A notificação não pode ser criada.")
            return
        }

        // Intent para abrir a ReminderActivity em tela cheia
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_MESSAGE, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação para "Concluir"
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_COMPLETED
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            "${eventId}_complete".hashCode(), // Request code único
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação para "Adiar"
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            "${eventId}_snooze".hashCode(), // Request code único
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construção da notificação
        val notification = NotificationCompat.Builder(context, "LEMBRETE_ROTINA_CHANNEL")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Lembrete de Rotina")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setFullScreenIntent(fullScreenPendingIntent, true) 
            .addAction(R.drawable.ic_check_circle_outline, "Concluir", completePendingIntent)
            .addAction(R.drawable.ic_time, "Adiar 15 min", snoozePendingIntent) // Adicionado botão Adiar
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)
        Log.d("AlarmReceiver", "Notificação de tela cheia para o evento '$eventId' disparada.")
    }
}
