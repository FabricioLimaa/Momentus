// ARQUIVO: utils/MainCoroutineRule.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Esta é a nossa regra customizada para gerenciar Coroutines em testes de unidade.
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        // Antes do teste começar, substituímos o Dispatcher.Main pelo nosso dispatcher de teste.
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        // Depois que o teste termina, limpamos tudo, restaurando o Dispatcher.Main original.
        Dispatchers.resetMain()
    }
}