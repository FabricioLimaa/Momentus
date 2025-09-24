// ARQUIVO: ui/RotinaAdapter.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ItemRotinaBinding

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. A classe agora herda de ListAdapter em vez de RecyclerView.Adapter.
//    Ele requer dois parâmetros: o tipo de dado (Rotina) e o ViewHolder.
// 2. Removemos a lista do construtor. O ListAdapter gerencia a lista internamente.
class RotinaAdapter(
    private val onItemClicked: (Rotina) -> Unit
) : ListAdapter<Rotina, RotinaAdapter.RotinaViewHolder>(DiffCallback) {
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
        // 3. Usamos getItem(position) para pegar o item da lista interna do ListAdapter.
        val rotinaAtual = getItem(position)
        holder.bind(rotinaAtual)
    }

    // 4. A função getRotinaAt agora usa getItem(position) também.
    fun getRotinaAt(position: Int): Rotina {
        return getItem(position)
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

            binding.root.setOnClickListener {
                onItemClicked(rotina)
            }
        }
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 5. Este objeto é a "inteligência" do ListAdapter.
    //    Ele diz ao adapter como verificar se dois itens são os mesmos (pelo ID)
    //    e se o conteúdo deles mudou (comparando o objeto inteiro).
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Rotina>() {
            override fun areItemsTheSame(oldItem: Rotina, newItem: Rotina): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Rotina, newItem: Rotina): Boolean {
                return oldItem == newItem
            }
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
    // NOTA: As funções 'getItemCount()' e 'updateData()' foram removidas, pois o ListAdapter cuida disso.
}