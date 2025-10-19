package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import kotlinx.coroutines.flow.Flow

/**
 * DAO para a entidade Template.
 */
@Dao
interface TemplateDao {

    @Transaction
    @Query("SELECT * FROM tabela_templates ORDER BY nome ASC")
    fun getTemplatesComEventos(): Flow<List<TemplateComEventos>>

    @Transaction
    @Query("SELECT * FROM tabela_templates WHERE id = :templateId")
    fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos>

    @Query("SELECT * FROM tabela_templates")
    fun getAllSync(): List<Template>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: Template)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<Template>)

    @Delete
    suspend fun delete(template: Template)

    @Query("DELETE FROM tabela_templates")
    suspend fun clear()
}
