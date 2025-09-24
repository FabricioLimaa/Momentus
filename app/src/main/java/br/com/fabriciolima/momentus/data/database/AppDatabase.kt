// ARQUIVO: data/database/AppDatabase.kt
package br.com.fabriciolima.momentus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.ItemCronograma // Adicione este import

// 1. Adicione ItemCronograma::class e mude a versão para 2
@Database(entities = [Rotina::class, ItemCronograma::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao
    abstract fun itemCronogramaDao(): ItemCronogramaDao // 2. Adicione o novo DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "momentus_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}