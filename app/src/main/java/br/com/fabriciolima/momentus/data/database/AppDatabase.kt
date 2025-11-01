package br.com.fabriciolima.momentus.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.UnlockedAchievement

@Database(
    entities = [Category::class, ItemCronograma::class, Template::class, Meta::class, HabitoConcluido::class, UnlockedAchievement::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun itemCronogramaDao(): ItemCronogramaDao

    abstract fun templateDao(): TemplateDao

    abstract fun metaDao(): MetaDao

    abstract fun habitoConcluidoDao(): HabitoConcluidoDao

    abstract fun unlockedAchievementDao(): UnlockedAchievementDao
}
