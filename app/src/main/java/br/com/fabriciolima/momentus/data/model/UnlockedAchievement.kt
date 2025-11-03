package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
@IgnoreExtraProperties
@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievement(
    @PrimaryKey
    val achievementId: String = "",
    @ServerTimestamp
    val dateUnlocked: Date? = null
)
