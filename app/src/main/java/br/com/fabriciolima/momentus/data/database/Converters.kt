package br.com.fabriciolima.momentus.data.database

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Conversores de tipo para o Room, para que ele saiba como salvar e ler tipos complexos
 * como o LocalTime do Java 8.
 */
class Converters {
    // Define um formatador padrão para garantir consistência
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    /**
     * Converte um objeto LocalTime em uma String para ser salva no banco de dados.
     */
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    /**
     * Converte uma String do banco de dados de volta para um objeto LocalTime.
     */
    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }
}
