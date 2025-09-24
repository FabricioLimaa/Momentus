// ARQUIVO: MomentusApplication.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus

import android.app.Application
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.database.AppDatabase

class MomentusApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    // Modifique a criação do repositório para incluir o novo DAO
    val repository by lazy { RotinaRepository(database.rotinaDao(), database.itemCronogramaDao(), database.templateDao()) }
}