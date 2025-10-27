package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.model.Meta
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: Meta)

    @Query("SELECT * FROM tabela_metas WHERE categoryId = :categoryId")
    fun getMetaForCategory(categoryId: String): Flow<Meta?>

    @Query("DELETE FROM tabela_metas")
    suspend fun clear()
}
