package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.model.UnlockedAchievement
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockedAchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(unlockedAchievement: UnlockedAchievement)

    @Query("SELECT * FROM unlocked_achievements")
    fun getAll(): Flow<List<UnlockedAchievement>>

    @Query("SELECT COUNT(*) FROM unlocked_achievements")
    fun getCount(): Flow<Int>

    @Query("DELETE FROM unlocked_achievements")
    suspend fun clear()
}
