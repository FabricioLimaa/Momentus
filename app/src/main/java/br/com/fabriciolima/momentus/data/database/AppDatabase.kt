package br.com.fabriciolima.momentus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.Template

/**
 * CORREÇÃO DEFINITIVA:
 * O arquivo de schema da versão 9 não existe, tornando a auto-migração impossível.
 * A solução é voltar a usar a migração destrutiva, que apaga e recria o banco de dados.
 * Mantemos a exportação do schema para que, no futuro, a migração da v10 para a v11 seja possível.
 */
@Database(
    entities = [Rotina::class, ItemCronograma::class, Template::class, Meta::class, HabitoConcluido::class],
    version = 10,
    exportSchema = true // Mantido para que o schema 10.json seja gerado para o futuro.
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rotinaDao(): RotinaDao
    abstract fun itemCronogramaDao(): ItemCronogramaDao
    abstract fun templateDao(): TemplateDao
    abstract fun metaDao(): MetaDao
    abstract fun habitoConcluidoDao(): HabitoConcluidoDao

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
                    // CORREÇÃO: Reintroduzindo a migração destrutiva, pois a auto-migração não é possível.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}