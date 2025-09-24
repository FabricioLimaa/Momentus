// ARQUIVO: data/database/AppDatabase.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.data.TemplateItem

// 1. Adicione Template::class e TemplateItem::class e mude a versão para 3
@Database(entities = [Rotina::class, ItemCronograma::class, Template::class, TemplateItem::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao
    abstract fun itemCronogramaDao(): ItemCronogramaDao
    abstract fun templateDao(): TemplateDao // 2. Adicione o novo DAO

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