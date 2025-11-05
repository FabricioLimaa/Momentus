package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: Template)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<Template>)

    @Update
    suspend fun update(template: Template)

    @Delete
    suspend fun delete(template: Template)

    @Query("SELECT * FROM tabela_templates")
    fun getAllSync(): List<Template>

    @Query("SELECT COUNT(*) FROM tabela_templates")
    fun getCountSync(): Int

    @Transaction
    @Query("SELECT * FROM tabela_templates")
    fun getTemplatesComEventos(): Flow<List<TemplateComEventos>>

    @Transaction
    @Query("SELECT * FROM tabela_templates WHERE id = :templateId")
    fun getTemplateComEventos(templateId: String): Flow<TemplateComEventos?>

    @Query("DELETE FROM tabela_templates")
    suspend fun clear()
}
