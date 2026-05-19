package br.com.fabriciolima.momentus.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.UnlockedAchievement

@Database(
    entities = [Category::class, ItemCronograma::class, Template::class, Meta::class, HabitoConcluido::class, UnlockedAchievement::class],
    version = 14,
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

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                checkAndAddDescricaoColumn(db)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                checkAndAddDescricaoColumn(db)
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                checkAndAddDescricaoColumn(db)
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                checkAndAddIsDeletedColumn(db)
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Força a correção do esquema se a versão 12 falhou anteriormente
                checkAndAddIsDeletedColumn(db)
                checkAndAddOrdemColumn(db)
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migração apenas para recalcular o Hash do Room
            }
        }

        private fun checkAndAddIsDeletedColumn(db: SupportSQLiteDatabase) {
            val cursor = db.query("PRAGMA table_info(tabela_itens_cronograma)")
            var hasIsDeleted = false
            while (cursor.moveToNext()) {
                val nameColumnIndex = cursor.getColumnIndex("name")
                if (nameColumnIndex != -1 && cursor.getString(nameColumnIndex) == "isDeleted") {
                    hasIsDeleted = true
                    break
                }
            }
            cursor.close()

            if (!hasIsDeleted) {
                db.execSQL("ALTER TABLE tabela_itens_cronograma ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun checkAndAddOrdemColumn(db: SupportSQLiteDatabase) {
            val cursor = db.query("PRAGMA table_info(tabela_itens_cronograma)")
            var hasOrdem = false
            while (cursor.moveToNext()) {
                val nameColumnIndex = cursor.getColumnIndex("name")
                if (nameColumnIndex != -1 && cursor.getString(nameColumnIndex) == "ordem") {
                    hasOrdem = true
                    break
                }
            }
            cursor.close()

            if (!hasOrdem) {
                db.execSQL("ALTER TABLE tabela_itens_cronograma ADD COLUMN ordem INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun checkAndAddDescricaoColumn(db: SupportSQLiteDatabase) {
            val cursor = db.query("PRAGMA table_info(tabela_templates)")
            var hasDescricao = false
            while (cursor.moveToNext()) {
                val nameColumnIndex = cursor.getColumnIndex("name")
                if (nameColumnIndex != -1 && cursor.getString(nameColumnIndex) == "descricao") {
                    hasDescricao = true
                    break
                }
            }
            cursor.close()
            
            if (!hasDescricao) {
                db.execSQL("ALTER TABLE tabela_templates ADD COLUMN descricao TEXT")
            }
        }
    }
}
