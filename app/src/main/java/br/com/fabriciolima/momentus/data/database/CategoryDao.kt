package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.data.model.StatsResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun clear()

    @Query("SELECT * FROM categories LEFT JOIN tabela_metas ON categories.id = tabela_metas.categoryId ORDER BY nome ASC")
    fun getCategoriesWithMetas(): Flow<List<CategoryWithMeta>>

    /**
     * Retorna todas as categorias como um Flow, ordenadas pelo nome.
     */
    @Query("SELECT * FROM categories ORDER BY nome ASC")
    fun getAll(): Flow<List<Category>>

    /**
     * NOVA CONSULTA SÍNCRONA PARA O WIDGET
     */
    @Query("SELECT * FROM categories")
    fun getAllSync(): List<Category>

    @Query("""
        SELECT
            c.nome AS category_name,
            c.cor AS category_color,
            SUM( (strftime('%s', i.horarioTermino) - strftime('%s', i.horarioInicio)) / 60 ) AS total_minutos
        FROM
            tabela_itens_cronograma AS i
        INNER JOIN
            categories AS c ON i.categoryId = c.id
        GROUP BY
            c.id, c.nome, c.cor
        HAVING
            total_minutos > 0
        ORDER BY
            total_minutos DESC
    """)
    fun getStats(): Flow<List<StatsResult>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): Category?
}
