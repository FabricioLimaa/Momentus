// ARQUIVO: notifications/AlarmScheduler.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, item: ItemCronograma, rotina: Rotina, leadTimeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            val mensagem = "${item.horarioInicio} - ${rotina.nome}"
            putExtra("MENSAGEM_NOTIFICACAO", mensagem)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val (hora, minuto) = item.horarioInicio.split(":").map { it.toInt() }

        val diaDaSemanaAlvo = getCalendarDayOfWeek(item.diaDaSemana)
        while (calendar.get(Calendar.DAY_OF_WEEK) != diaDaSemanaAlvo) {
            calendar.add(Calendar.DAY_OF_WEEK, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, hora)
        calendar.set(Calendar.MINUTE, minuto)
        calendar.set(Calendar.SECOND, 0)

        calendar.add(Calendar.MINUTE, -leadTimeMinutes)

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context, item: ItemCronograma) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    // --- MODIFICAÇÃO: CORREÇÃO DA SINTAXE AQUI ---
    private fun getCalendarDayOfWeek(day: String): Int {
        // Adicionamos a palavra 'return' antes do 'when'
        return when (day) {
            "DOM" -> Calendar.SUNDAY
            "SEG" -> Calendar.MONDAY
            "TER" -> Calendar.TUESDAY
            "QUA" -> Calendar.WEDNESDAY
            "QUI" -> Calendar.THURSDAY
            "SEX" -> Calendar.FRIDAY
            "SÁB" -> Calendar.SATURDAY
            else -> -1
        }
    }
}