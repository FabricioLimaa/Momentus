package br.com.fabriciolima.momentus.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // Injetamos um Provider para quebrar a dependência circular
        rotinaDaoProvider: Provider<RotinaDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "momentus_database"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Usamos o Provider para obter o DAO e inserir a categoria padrão
                CoroutineScope(Dispatchers.IO).launch {
                    val rotinaDao = rotinaDaoProvider.get()
                    val id = UUID.randomUUID().toString()
                    val defaultCategory = Rotina(
                        id = id,
                        nome = "Outros",
                        descricao = "Categoria para eventos diversos.",
                        duracaoPadraoMinutos = 60,
                        cor = "#808080", // Cinza
                        tag = null
                    )
                    rotinaDao.insert(defaultCategory)
                }
            }
        })
        .build()
    }

    @Provides
    fun provideRotinaDao(appDatabase: AppDatabase): RotinaDao {
        return appDatabase.rotinaDao()
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
    @Singleton
    fun provideRotinaRepository(
        rotinaDao: RotinaDao,
        itemCronogramaDao: ItemCronogramaDao,
        templateDao: TemplateDao,
        metaDao: MetaDao,
        habitoConcluidoDao: HabitoConcluidoDao
    ): RotinaRepository {
        return RotinaRepository(rotinaDao, itemCronogramaDao, templateDao, metaDao, habitoConcluidoDao)
    }
}
