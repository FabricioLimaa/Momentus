// ARQUIVO: data/database/AppDatabase.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Meta
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.data.TemplateItem

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. Adicionamos Meta::class à lista de entidades.
// 2. Incrementamos a versão do banco de dados de 4 para 5, pois houve uma mudança na estrutura.
@Database(entities = [Rotina::class, ItemCronograma::class, Template::class, TemplateItem::class, Meta::class], version = 5, exportSchema = false)
// --- MODIFICAÇÃO TERMINA AQUI ---
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao
    abstract fun itemCronogramaDao(): ItemCronogramaDao
    abstract fun templateDao(): TemplateDao
    abstract fun metaDao(): MetaDao

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