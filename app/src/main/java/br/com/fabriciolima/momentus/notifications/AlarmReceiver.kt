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
        // Quando o alarme dispara, esta função é executada.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Pegamos o nome da rotina que passamos ao criar o alarme.
        val nomeRotina = intent.getStringExtra("NOME_ROTINA") ?: "Sua rotina está prestes a começar"

        // Construímos a notificação.
        val notification = NotificationCompat.Builder(context, "LEMBRETE_ROTINA_CHANNEL")
            .setSmallIcon(R.drawable.ic_schedule) // Ícone que aparecerá na barra de status.
            .setContentTitle("Lembrete de Rotina")
            .setContentText(nomeRotina)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Mostramos a notificação.
        notificationManager.notify(1, notification)
    }
}