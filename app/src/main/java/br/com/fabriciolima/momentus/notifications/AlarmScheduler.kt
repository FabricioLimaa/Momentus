package br.com.fabriciolima.momentus.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.com.fabriciolima.momentus.data.ItemCronograma
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(item: ItemCronograma) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_ITEM_TITLE, item.titulo)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()

        item.diaDaSemana?.let {
            // CORREÇÃO: Acessando as propriedades de LocalTime diretamente
            val hora = item.horarioInicio.hour
            val minuto = item.horarioInicio.minute

            // Mapeia nossa string de dia da semana para o inteiro do Calendar
            val dayOfWeekInt = when (it) {
                "DOM" -> Calendar.SUNDAY
                "SEG" -> Calendar.MONDAY
                "TER" -> Calendar.TUESDAY
                "QUA" -> Calendar.WEDNESDAY
                "QUI" -> Calendar.THURSDAY
                "SEX" -> Calendar.FRIDAY
                "SÁB" -> Calendar.SATURDAY
                else -> return // Dia inválido, não agendar
            }

            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeekInt)
            calendar.set(Calendar.HOUR_OF_DAY, hora)
            calendar.set(Calendar.MINUTE, minuto)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Se o alarme para hoje já passou, agendar para a próxima semana
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Repetir semanalmente
                pendingIntent
            )
        }
    }

    fun cancel(item: ItemCronograma) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_ITEM_ID = "EXTRA_ITEM_ID"
        const val EXTRA_ITEM_TITLE = "EXTRA_ITEM_TITLE"
    }
}
