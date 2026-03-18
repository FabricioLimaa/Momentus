package br.com.fabriciolima.momentus.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.database.CategoryDao
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSourceImpl
import br.com.fabriciolima.momentus.domain.usecase.CheckAndUnlockAchievementsUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindGoogleCalendarSource(impl: GoogleCalendarSourceImpl): GoogleCalendarSource

}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        dataStore: DataStore<Preferences>,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepository(firestore, auth, dataStore, dispatcher)
    }

    @Provides
    @Singleton
    fun provideTemplateRepository(
        @ApplicationContext context: Context,
        templateDao: TemplateDao,
        rotinaRepository: RotinaRepository,
        checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): TemplateRepository {
        return TemplateRepository(context, templateDao, rotinaRepository, checkAndUnlockAchievementsUseCase, dispatcher)
    }

    @Provides
    @Singleton
    fun provideRotinaRepository(
        itemCronogramaDao: ItemCronogramaDao,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationContext context: Context
    ): RotinaRepository {
        return RotinaRepository(itemCronogramaDao, dispatcher, context)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao,
        metaDao: MetaDao,
        habitoConcluidoDao: HabitoConcluidoDao,
        itemCronogramaDao: ItemCronogramaDao,
        templateRepository: TemplateRepository,
        rotinaRepository: RotinaRepository,
        userRepository: UserRepository,
        gamificationRepository: GamificationRepository,
        checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
        googleCalendarSource: GoogleCalendarSource,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CategoryRepository {
        return CategoryRepository(
            categoryDao,
            metaDao,
            habitoConcluidoDao,
            itemCronogramaDao,
            templateRepository,
            rotinaRepository,
            userRepository,
            gamificationRepository,
            checkAndUnlockAchievementsUseCase,
            googleCalendarSource,
            dispatcher
        )
    }
}
