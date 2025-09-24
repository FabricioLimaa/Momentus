// ARQUIVO: ui/RotinaAdapter.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ItemRotinaBinding

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. Adicionamos um parâmetro ao construtor: uma função que será chamada quando um item for clicado.
// Esta função recebe um objeto 'Rotina' como argumento.
class RotinaAdapter(
    private var rotinas: List<Rotina>,
    private val onItemClicked: (Rotina) -> Unit
) : RecyclerView.Adapter<RotinaAdapter.RotinaViewHolder>() {
// --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotinaViewHolder {
        val binding = ItemRotinaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RotinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RotinaViewHolder, position: Int) {
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Passamos a rotina atual para o método bind do ViewHolder.
        val rotinaAtual = rotinas[position]
        holder.bind(rotinaAtual)
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }

    override fun getItemCount(): Int = rotinas.size

    fun updateData(novaLista: List<Rotina>) {
        this.rotinas = novaLista
        notifyDataSetChanged()
    }

    fun getRotinaAt(position: Int): Rotina {
        return rotinas[position]
    }

    inner class RotinaViewHolder(private val binding: ItemRotinaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rotina: Rotina) {
            binding.textViewNome.text = rotina.nome
            binding.textViewDuracao.text = "Duração: ${rotina.duracaoPadraoMinutos} minutos"
            try {
                binding.viewCor.setBackgroundColor(Color.parseColor(rotina.cor))
            } catch (e: Exception) {
                binding.viewCor.setBackgroundColor(Color.GRAY)
            }

            // --- MODIFICAÇÃO INICIA AQUI ---
            // 3. Adicionamos um listener de clique ao item inteiro da lista (binding.root).
            // Quando o item é clicado, chamamos a função 'onItemClicked' que recebemos
            // lá no construtor, passando a rotina correspondente a este item.
            binding.root.setOnClickListener {
                onItemClicked(rotina)
            }
            // --- MODIFICAÇÃO TERMINA AQUI ---
        }
    }
}