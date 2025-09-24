// ARQUIVO: MomentusApplication.kt
package br.com.fabriciolima.momentus

import android.app.Application
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.database.AppDatabase

class MomentusApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    // Atualize a criação do repositório
    val repository by lazy { RotinaRepository(database.rotinaDao(), database.itemCronogramaDao()) }
}