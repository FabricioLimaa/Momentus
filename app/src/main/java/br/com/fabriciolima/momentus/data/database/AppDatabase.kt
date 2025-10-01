// ARQUIVO: data/database/AppDatabase.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fabriciolima.momentus.data.* // Usamos wildcard para importar todas as entidades

// 1. Adicione HabitoConcluido::class e mude a versão para 8
@Database(entities = [Rotina::class, ItemCronograma::class, Template::class, TemplateItem::class, Meta::class, HabitoConcluido::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao
    abstract fun itemCronogramaDao(): ItemCronogramaDao
    abstract fun templateDao(): TemplateDao
    abstract fun metaDao(): MetaDao
    abstract fun habitoConcluidoDao(): HabitoConcluidoDao // 2. Adicione o novo DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "momentus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}