package br.com.fabriciolima.momentus.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.fabriciolima.momentus.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mensagem = intent.getStringExtra("MENSAGEM_NOTIFICACAO") ?: "Sua rotina está prestes a começar"

        val notification = NotificationCompat.Builder(context, "LEMBRETE_ROTINA_CHANNEL")
            .setSmallIcon(R.drawable.ic_stat_name) // Ícone de notificação padrão
            .setContentTitle("Lembrete de Rotina")
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}