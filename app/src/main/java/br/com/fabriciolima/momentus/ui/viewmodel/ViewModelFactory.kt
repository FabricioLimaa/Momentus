package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.fabriciolima.momentus.data.database.AppDatabase
import br.com.fabriciolima.momentus.data.repository.RotinaRepository

class ViewModelFactory(
    private val appDatabase: AppDatabase,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val repository by lazy {
            RotinaRepository(
                rotinaDao = appDatabase.rotinaDao(),
                itemCronogramaDao = appDatabase.itemCronogramaDao(),
                templateDao = appDatabase.templateDao(),
                metaDao = appDatabase.metaDao(),
                habitoConcluidoDao = appDatabase.habitoConcluidoDao()
            )
        }

        return when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> {
                CalendarViewModel(repository, application) as T
            }
            modelClass.isAssignableFrom(TemplateViewModel::class.java) -> {
                TemplateViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TemplateDetailViewModel::class.java) -> {
                TemplateDetailViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CreateTemplateViewModel::class.java) -> {
                CreateTemplateViewModel(repository) as T
            }
            // Adicionando a "receita" que faltava
            modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
                CategoryViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
