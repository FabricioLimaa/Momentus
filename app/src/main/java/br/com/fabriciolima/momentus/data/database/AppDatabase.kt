package br.com.fabriciolima.momentus.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template

@Database(
    entities = [Rotina::class, ItemCronograma::class, Template::class, Meta::class, HabitoConcluido::class],
    version = 2,
    exportSchema = false // Adicionado para resolver o erro de compilação do KSP
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao

    abstract fun itemCronogramaDao(): ItemCronogramaDao

    abstract fun templateDao(): TemplateDao

    abstract fun metaDao(): MetaDao

    abstract fun habitoConcluidoDao(): HabitoConcluidoDao
}
