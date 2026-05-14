package br.com.fabriciolima.momentus.di

import android.content.Context
import android.util.Log
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
import br.com.fabriciolima.momentus.util.SecurityUtils
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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase
import java.io.File

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
        val dbName = "momentus_database"
        val dbFile = context.getDatabasePath(dbName)
        val passphrase = SecurityUtils.getDatabasePassphrase(context)
        
        // Garante que a pasta 'databases' exista
        dbFile.parentFile?.mkdirs()

        // --- LÓGICA DE MIGRAÇÃO PARA PRODUÇÃO ---
        if (dbFile.exists()) {
            var isPlain = false
            try {
                // TESTE REAL: Tenta abrir e ler o banco como texto plano.
                val plainDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                // Se conseguirmos rodar um SELECT, o banco é definitivamente texto plano.
                val cursor = plainDb.rawQuery("SELECT count(*) FROM sqlite_master", null)
                isPlain = cursor.moveToFirst()
                cursor.close()
                plainDb.close()
            } catch (e: Exception) {
                // Se der erro aqui, é o sinal de que o banco JÁ ESTÁ CRIPTOGRAFADO.
                Log.d("AppModule", "Verificação: Banco de dados já está protegido por criptografia.")
                isPlain = false
            }

            if (isPlain) {
                try {
                    Log.w("AppModule", "Iniciando criptografia do banco de dados existente...")
                    encryptExistingDatabase(context, dbName, passphrase)
                    Log.i("AppModule", "Sucesso: O banco de dados agora está criptografado.")
                } catch (e: Exception) {
                    Log.e("AppModule", "Erro ao converter banco: ${e.message}", e)
                }
            }
        }

        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .addMigrations(
            AppDatabase.MIGRATION_8_9, 
            AppDatabase.MIGRATION_9_10, 
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    /**
     * Converte um banco de dados SQLite comum em um banco SQLCipher criptografado.
     */
    private fun encryptExistingDatabase(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        val tempFile = context.getDatabasePath("${dbName}_encrypted_v9.db")
        
        // 1. Garante que o diretório 'databases' exista
        dbFile.parentFile?.mkdirs()
        
        // 2. Cria o arquivo temporário VAZIO manualmente para garantir permissões
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()

        // 3. Abre o banco original (Texto Plano) usando a lib SQLCipher com senha vazia
        System.loadLibrary("sqlcipher")
        val db = SQLCipherDatabase.openDatabase(
            dbFile.absolutePath,
            null as ByteArray?,
            null,
            SQLCipherDatabase.OPEN_READWRITE,
            null as net.zetetic.database.sqlcipher.SQLiteDatabaseHook?
        )
        
        try {
            val hexPassphrase = passphrase.joinToString("") { "%02x".format(it) }
            
            // 4. ATTACH: Conecta ao arquivo temporário que criamos, agora usando a chave
            db.rawExecSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS encrypted KEY x'$hexPassphrase'")
            
            // 5. EXPORT: Copia tudo do banco aberto (plano) para o anexo (criptografado)
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            
            // 6. DETACH: Finaliza a conexão com o novo banco
            db.rawExecSQL("DETACH DATABASE encrypted")
        } catch (e: Exception) {
            Log.e("AppModule", "Falha interna na exportação SQLCipher: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            throw e
        } finally {
            db.close()
        }

        // 7. Substituição Atômica
        if (tempFile.exists() && tempFile.length() > 0) {
            // Remove o banco antigo (plano) e seus arquivos auxiliares (-wal, -shm)
            context.deleteDatabase(dbName) 
            
            // Move o novo banco criptografado para o lugar oficial
            if (!tempFile.renameTo(dbFile)) {
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()
            }
        } else {
            throw Exception("O processo de criptografia gerou um arquivo inválido.")
        }
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
