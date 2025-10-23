package br.com.fabriciolima.momentus.di

import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSourceImpl
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
    fun provideTemplateRepository(
        templateDao: TemplateDao,
        eventoRepository: EventoRepository, // Adicionado
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
    fun provideRotinaRepository(
        rotinaDao: RotinaDao,
        metaDao: MetaDao,
        habitoConcluidoDao: HabitoConcluidoDao,
        templateRepository: TemplateRepository,
        eventoRepository: EventoRepository,
        googleCalendarSource: GoogleCalendarSource,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): RotinaRepository {
        return RotinaRepository(
            rotinaDao,
            metaDao,
            habitoConcluidoDao,
            templateRepository,
            eventoRepository,
            googleCalendarSource,
            dispatcher
        )
    }
}
