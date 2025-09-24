// ARQUIVO: ui/cronograma/CronogramaPagerAdapter.kt
package br.com.fabriciolima.momentus.ui.cronograma

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CronogramaPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

    override fun getItemCount(): Int = diasDaSemana.size

    override fun createFragment(position: Int): Fragment {
        // Passa o dia da semana correto para cada fragmento
        return DiaCronogramaFragment.newInstance(diasDaSemana[position])
    }
}