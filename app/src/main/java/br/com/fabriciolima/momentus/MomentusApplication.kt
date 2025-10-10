package br.com.fabriciolima.momentus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// ADICIONADO: Anotação que habilita a injeção de dependência do Hilt
@HiltAndroidApp
class MomentusApplication : Application() {
    // A criação do banco de dados e do repositório será gerenciada pelo Hilt.
    // O código anterior foi removido.
}
