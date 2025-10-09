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
 * Esta versão foi limpa para remover funções obsoletas que usavam a entidade TemplateItem.
 */
@Dao
interface TemplateDao {

    /**
     * Busca todos os templates com seus respectivos eventos (itens do cronograma).
     */
    @Transaction
    @Query("SELECT * FROM tabela_templates ORDER BY nome ASC")
    fun getTemplatesComEventos(): Flow<List<TemplateComEventos>>

    /**
     * Busca um template específico com seus eventos.
     */
    @Transaction
    @Query("SELECT * FROM tabela_templates WHERE id = :templateId")
    fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos>

    /**
     * Insere um novo template.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: Template)

    /**
     * Deleta um template. A deleção dos eventos associados ocorre em cascata (onDelete = CASCADE).
     */
    @Delete
    suspend fun delete(template: Template)
}
