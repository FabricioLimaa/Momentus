package br.com.fabriciolima.momentus.di

import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.database.CategoryDao
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSourceImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepository(firestore, auth, dispatcher)
    }

    @Provides
    @Singleton
    fun provideTemplateRepository(
        templateDao: TemplateDao,
        eventoRepository: EventoRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): TemplateRepository {
        return TemplateRepository(templateDao, eventoRepository, dispatcher)
    }

    @Provides
    @Singleton
    fun provideEventoRepository(
        itemCronogramaDao: ItemCronogramaDao,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): EventoRepository {
        return EventoRepository(itemCronogramaDao, dispatcher)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        CategoryDao: CategoryDao,
        metaDao: MetaDao,
        habitoConcluidoDao: HabitoConcluidoDao,
        itemCronogramaDao: ItemCronogramaDao, // Adicionado o parâmetro que faltava
        templateRepository: TemplateRepository,
        eventoRepository: EventoRepository,
        googleCalendarSource: GoogleCalendarSource,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CategoryRepository {
        return CategoryRepository(
            CategoryDao,
            metaDao,
            habitoConcluidoDao,
            itemCronogramaDao, // Passado o parâmetro na ordem correta
            templateRepository,
            eventoRepository,
            googleCalendarSource,
            dispatcher
        )
    }
}
