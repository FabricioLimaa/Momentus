package br.com.fabriciolima.momentus.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import br.com.fabriciolima.momentus.BuildConfig
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.database.CategoryDao
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.database.UnlockedAchievementDao
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.GoogleAuthUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val USER_PREFERENCES_NAME = "user_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @VersionCode
    fun provideVersionCode(): Int = BuildConfig.VERSION_CODE

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = GoogleAuthUtils.getGoogleSignInOptions(context)
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "momentus_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }

    @Provides
    fun provideItemCronogramaDao(appDatabase: AppDatabase): ItemCronogramaDao {
        return appDatabase.itemCronogramaDao()
    }

    @Provides
    fun provideTemplateDao(appDatabase: AppDatabase): TemplateDao {
        return appDatabase.templateDao()
    }

    @Provides
    fun provideMetaDao(appDatabase: AppDatabase): MetaDao {
        return appDatabase.metaDao()
    }

    @Provides
    fun provideHabitoConcluidoDao(appDatabase: AppDatabase): HabitoConcluidoDao {
        return appDatabase.habitoConcluidoDao()
    }

    @Provides
    fun provideUnlockedAchievementDao(appDatabase: AppDatabase): UnlockedAchievementDao {
        return appDatabase.unlockedAchievementDao()
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmScheduler(context)
    }
}
