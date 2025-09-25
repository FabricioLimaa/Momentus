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

    // Função para agendar um alarme para um item do cronograma.
    fun schedule(context: Context, item: ItemCronograma, rotina: Rotina) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Criamos uma Intent que aponta para o nosso AlarmReceiver.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Passamos o nome da rotina como um "extra" para que a notificação possa exibi-lo.
            putExtra("NOME_ROTINA", rotina.nome)
        }

        // Criamos um PendingIntent. É como uma "autorização" para que o sistema
        // execute nossa Intent no futuro, mesmo que nosso app esteja fechado.
        // O 'id' do item é usado como requestCode para garantir que cada alarme seja único.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(), // Usamos o hashCode do ID do item como um ID único para o alarme.
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculamos a hora exata em que o alarme deve disparar.
        val calendar = Calendar.getInstance()
        val (hora, minuto) = item.horarioInicio.split(":").map { it.toInt() }

        // Encontramos o próximo dia da semana que corresponde ao dia agendado.
        val diaDaSemanaAlvo = getCalendarDayOfWeek(item.diaDaSemana)
        while (calendar.get(Calendar.DAY_OF_WEEK) != diaDaSemanaAlvo) {
            calendar.add(Calendar.DAY_OF_WEEK, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, hora)
        calendar.set(Calendar.MINUTE, minuto)
        calendar.set(Calendar.SECOND, 0)

        // Se o horário já passou hoje, agendamos para a próxima semana.
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // --- AQUI O ALARME É AGENDADO ---
        // Usamos setExactAndAllowWhileIdle para garantir que o alarme dispare na hora certa,
        // mesmo que o celular esteja em modo de economia de energia.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    // Função para cancelar um alarme.
    fun cancel(context: Context, item: ItemCronograma) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        // Criamos um PendingIntent idêntico ao que foi usado para criar o alarme.
        // O sistema o usa para encontrar e cancelar o alarme correspondente.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancela o alarme.
        alarmManager.cancel(pendingIntent)
    }

    // Função auxiliar para converter nossa string de dia ("SEG", "TER") para o inteiro do Calendar.
    private fun getCalendarDayOfWeek(day: String): Int {
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