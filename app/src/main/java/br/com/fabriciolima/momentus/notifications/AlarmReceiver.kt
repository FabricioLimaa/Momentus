// ARQUIVO: notifications/AlarmReceiver.kt (CÓDIGO COMPLETO)
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

        // --- MODIFICAÇÃO INICIA AQUI ---
        // Pegamos a nova mensagem completa que enviamos.
        val mensagem = intent.getStringExtra("MENSAGEM_NOTIFICACAO") ?: "Sua rotina está prestes a começar"
        // --- MODIFICAÇÃO TERMINA AQUI ---

        val notification = NotificationCompat.Builder(context, "LEMBRETE_ROTINA_CHANNEL")
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle("Lembrete de Rotina")
            // --- MODIFICAÇÃO INICIA AQUI ---
            // Usamos a nova mensagem como texto da notificação.
            .setContentText(mensagem)
            // --- MODIFICAÇÃO TERMINA AQUI ---
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}